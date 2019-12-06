package com.syb.lintplugin.task

import com.android.SdkConstants
import com.android.annotations.NonNull
import com.android.build.gradle.internal.LintGradleClient
import com.android.build.gradle.internal.dsl.LintOptions
import com.android.build.gradle.internal.scope.GlobalScope
import com.android.build.gradle.internal.tasks.BaseTask
import com.android.build.gradle.internal.variant.BaseVariantData
import com.android.builder.model.AndroidProject
import com.android.builder.model.Variant
import com.android.tools.lint.LintCliFlags
import com.android.tools.lint.client.api.IssueRegistry
import com.syb.coffeelint.CIssueRegister
import com.syb.lintplugin.CoffeeLintClient
import com.syb.lintplugin.utils.FileUtil
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.tooling.provider.model.ToolingModelBuilder

import java.lang.reflect.Method

abstract class BaseLintTask extends BaseTask {
    protected BaseVariantData variantData
    protected Variant variant
    protected GlobalScope globalScope
    protected AndroidProject modelProject

    protected LintCliFlags flags
    protected IssueRegistry registry
    protected CoffeeLintClient client

    protected LintOptions options

    protected abstract LintOptions createLintOptions()

    BaseLintTask() {
        initEnv()
    }

    protected void initEnv() {
        // 获取Gradle脚本中的variant列表
        // （这里的variant基类是个隐藏内部类，所以不直接指定类型）
        Object[] variantImplList = null
        if (project.plugins.hasPlugin('com.android.application')) {
            variantImplList = project.android.applicationVariants.toArray()
        } else if (project.plugins.hasPlugin('com.android.library')) {
            variantImplList = project.android.libraryVariants.toArray()
        }

        if (variantImplList == null || variantImplList.length == 0) {
            return
        }

        Object variantImpl = null
        String variantImplName = null

        try {
            // 获取Gradle工程的variant
            // （对多个variant，只取第一个debug variant，这里只做静态扫描，不是打包）

            // com.android.build.gradle.internal.api.BaseVariantImpl是内部类
            Class baseVariantImplClazz = Class.forName('com.android.build.gradle.internal.api.BaseVariantImpl')
            if (baseVariantImplClazz == null) {
                return
            }

            Method method = baseVariantImplClazz.getMethod('getName')
            if (method == null) {
                return
            }
            method.setAccessible(true)

            for (Object obj : variantImplList) {
                if (baseVariantImplClazz.isAssignableFrom(obj.getClass())) {
                    variantImplName = (String)method.invoke(obj)
                    if (variantImplName.toLowerCase().contains('debug')) {
                        variantImpl = obj
                        break
                    }
                }
            }

            if (variantImpl == null) {
                variantImpl = variantImplList[0]
            }

            if (variantImpl == null) {
                return
            }

            // 获取variant的详细数据
            method = baseVariantImplClazz.getDeclaredMethod('getVariantData')
            if (method != null) {
                // BaseVariantImpl#getVariantData()是protected方法
                method.setAccessible(true)
                variantData = method.invoke(variantImpl)
            }
        } catch (Exception e) {
            e.printStackTrace()
        }

        if (variantData == null) {
            return
        }

        // 获取全局scope
        globalScope = variantData.scope.globalScope

        // 创建AndroidProject
        String modelName = AndroidProject.class.getName()
        ToolingModelBuilder modelBuilder = globalScope.toolingRegistry.getBuilder(modelName)
        modelProject = (AndroidProject)modelBuilder.buildAll(modelName, project)

        if (modelProject == null) {
            return
        }

        Collection<Variant> variantList = modelProject.getVariants()
        if (variantList != null && !variantList.isEmpty()) {
            for (Variant v : variantList) {
                if (v.name.equals(variantImplName)) {
                    variant = v
                    break
                }
            }
        }

        if (variant != null) {
            setVariantName(variant.name)
        }
    }

    protected void preRun() {
        registry = CIssueRegister.getInstance()
        flags = new LintCliFlags()
        client = createLintClient()

        options = createLintOptions()

        setVariantName(variant.name)
    }

    protected void analyze(@NonNull List<File> files) {
        if (syncLintOptions()) {
            try {
                client.run(registry, files)
            } catch (IOException e) {
                throw new GradleException("Invalid arguments.", e)
            }

            if (client.haveErrors() && flags.isSetExitCode()) {
                abort()
            }
        }
    }

    private CoffeeLintClient createLintClient() {
        CoffeeLintClient lintClient = new CoffeeLintClient(
                registry,
                flags,
                project,
                modelProject,
                new File(FileUtil.getProperty(project, SdkConstants.FN_LOCAL_PROPERTIES, SdkConstants.SDK_DIR_PROPERTY)),
                variant,
                null)

        return lintClient
    }

    protected static final String LintTask_2_0_0 = 'com.android.build.gradle.tasks.Lint'

    private static Class lintTaskClazz = null
    private static Class getLintTaskClazz() {
        if (lintTaskClazz == null) {
            try {
                lintTaskClazz = Class.forName(LintTask_2_0_0)
            } catch (Exception e) {
                println("getLintTaskClazz Exception : " + e.message)
            }
        }
        return lintTaskClazz
    }

    protected boolean syncLintOptions() {
        LintOptions pluginLintOptions = globalScope.extension.lintOptions

        // 关闭系统规则
        Set<String> disableSet = new HashSet<>()
        disableSet.add('LintError')
        disableSet.add('NewApi')
        pluginLintOptions.disable = disableSet

        // 必要 lintOptions
        pluginLintOptions.warningsAsErrors = true

        pluginLintOptions.htmlReport = true
        pluginLintOptions.htmlOutput = project.file("${project.buildDir}/reports/lint/lint-result-${project.name}.html")
        pluginLintOptions.xmlReport = true
        pluginLintOptions.xmlOutput = project.file("${project.buildDir}/reports/lint/lint-result-${project.name}.xml")

        pluginLintOptions.abortOnError = true

        // 个性化 lintOptions
        if (options.check != null) {
            pluginLintOptions.check = options.check
        }
        if (options.textReport != null) {
            pluginLintOptions.textReport = options.textReport
        }

        boolean ret = true
        try {
            Class clazz = getLintTaskClazz()
            if (clazz == null) {
                ret = false
            } else {
                Method method = clazz.getDeclaredMethod('syncOptions', LintOptions.class, LintGradleClient.class, LintCliFlags.class, Variant.class, Project.class, boolean.class, boolean.class)
                if (method == null) {
                    ret = false
                } else {
                    method.setAccessible(true)
                    method.invoke(null, pluginLintOptions, client, flags, variant, project, true, false)
                }
            }
        } catch (Exception e) {
            e.printStackTrace()
            ret = false
        }

        return ret
    }

    private void abort() {
        String buildDirPath = "/${project.name}/build"
        def message = "\n- ${project.name}：lint 检查没通过，请查看检测报告并进行修改。\n" +
                "发现异常：${client.getErrorCount()} errors, ${client.getWarningCount()} warnings\n" +
                "生成报告：${buildDirPath}/reports/lint/lint-result-${project.name}.xml"
        throw new GradleException(message)
    }

}
