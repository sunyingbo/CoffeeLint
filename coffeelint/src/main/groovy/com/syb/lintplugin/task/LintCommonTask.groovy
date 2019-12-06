package com.syb.lintplugin.task

import com.android.SdkConstants
import com.android.build.gradle.internal.dsl.LintOptions
import com.syb.lintplugin.constants.Constants
import com.syb.lintplugin.jira.AndroidClient
import com.syb.lintplugin.jira.Jira
import com.syb.lintplugin.jira.JiraClient
import com.syb.lintplugin.utils.*
import org.gradle.api.GradleException
import org.gradle.api.tasks.TaskAction

/**
 * Lint 全量检查任务
 * 编译/打包时自动执行
 */
class LintCommonTask extends BaseLintTask {

    private List<File> changeFileList
    private Map<String, String> results
    private Map<String, Jira.Issues> jiraMap
    private Map<String, String> configMap
    private int jiraCount = 0
    private boolean createAbort = false

    LintCommonTask() {
        super()
    }

    @TaskAction
    void lintCommon() {
        preRun()
        if (!needCheck()) {
            return
        }

        if (GroovyUtil.needJira(project)) {
            initModuleConfig()
            initJiraDatas()
        }
        try {
            analyze(changeFileList)
            if (GroovyUtil.needJira(project)) {
                // 如果本次没有扫描出新的代码问题，尝试解决已经存在的旧的 Jira
                int errorCount = client.getErrorCount()
                if (errorCount == 0) {
                    resolveIssue()
                }
            }
        } catch (GradleException e) {
            if (GroovyUtil.needJira(project)) {
                results = XMLUtils.parseLintResult(project,
                        "${project.buildDir}/reports/lint/lint-result-${project.name}.xml",
                        0)
                println("\n解析出的问题数量：${results.size()} 。\n")

                if (JiraClient.LOGIN_NAME == null || JiraClient.LOGIN_PASSWORD == null) {
                    println("请在根目录的 gradle.properties 文件中配置 Jira 账户！\n")
                    throw e
                }

                // 添加备注
                addComment()
                // 解决已经修改的问题
                resolveIssue()
                // 每周四创建新问题
                if (GroovyUtil.getDate().startsWith("Thu")) {
                    create()
                }

                if (results != null) {
                    results.clear()
                    results == null
                }
                if (jiraMap != null) {
                    jiraMap.clear()
                    jiraMap == null
                }
                jiraCount = 0
                checkJiraDatas()
            }
            throw e
        }
    }

    @Override
    protected void preRun() {
        super.preRun()
        changeFileList = new ArrayList<>()

        if (GroovyUtil.needJira(project)) {
            return
        }

        if ("".equals(Constants.CHECK_LINE)) { // 如果 CHECK_LINE 为空，则不需要指定检测范围
            return
        }
        List<String> changeFilePathList = GitUtils.getChangedFiles(project)
        if (changeFilePathList.size() == 0) {
            changeFilePathList = GitUtils.getChangedFiles(project, Constants.CHECK_LINE)
        }
        for (int i = 0; i < changeFilePathList.size(); i++) {
            changeFileList.add(new File(changeFilePathList.get(i)))
        }
    }

    /*
    初始化 Jira 库数据
     */
    private void initJiraDatas() {
        if (jiraMap == null) {
            jiraMap = new HashMap<>()
            Jira[] jiras = checkJiraDatas()
            for (Jira jira : jiras) {
                if (jira != null) {
                    for (Jira.Issues issues : jira.issues) {
                        if (issues.fields.description.contains(project.name)) {
                            jiraMap.put(issues.fields.description, issues)
                        }
                    }
                }
            }
        }
    }

    /*
    初始化临时问题负责人
    防止原有模块负责人临时分配经办人后无法接收到邮件
     */
    private void initModuleConfig() {
        if (configMap == null) {
            configMap = new HashMap<>()
            String assignee = FileUtil.getProperty(project, SdkConstants.FN_GRADLE_PROPERTIES, "MODULE_CONFIG")
            if (assignee != null && !"".equals(assignee)) {
                String[] moduleConfig = assignee.split(",")
                for (String config : moduleConfig) {
                    String[] cfg = config.split("=")
                    configMap.put(cfg[0], cfg[1])
                }
            }
        }
    }

    /*
    检查 Jira 库数据
     */
    private Jira checkJiraDatas() {
        String[] assignees = [Module.getMail(project.name), configMap.get(project.name)]
        Jira[] jiras = new Jira[assignees.size()]
        for (int i = 0; i < assignees.size(); i++) {
            String assignee = assignees[i]
            if (assignee != null) {
                String jql = "project = WBANDROIDLINT AND resolution = Unresolved AND assignee = $assignee order by updated DESC"
                Jira jira = AndroidClient.getInstance().search(jql)
                if (jira != null && jira.total > 0) {
                    println("$assignee 目前有 ${jira.total} 个 Jira 没有解决！\n")
                    jiras[i] = jira
                } else {
                    println("\n${project.name} 所有问题解决完毕！负责人 $assignee 。\n")
                }
            }
        }
        return jiras
    }

    // 如果 CHECK_LINE 不为空，但文件列表为空，则不检查
    private boolean needCheck() {
        if (!GroovyUtil.needJira(project) && !"".equals(Constants.CHECK_LINE) && changeFileList.size() == 0) {
            return false
        }
        return true
    }

    @Override
    protected LintOptions createLintOptions() {
        LintOptions lintOptions = new LintOptions()

        Set<String> checkSet = new HashSet<>()
        checkSet.add('MessageObtainUseError')
        checkSet.add('FinalError')
        checkSet.add('StaticInfoError')
        checkSet.add('LogUseError')
        checkSet.add('ToastUseError')
        checkSet.add('ThreadUseError')
        checkSet.add('SharePreManagerUseError')
        checkSet.add('StartServiceError')
        if (GroovyUtil.needJira(project)) {
            checkSet.add('InnerClassError')
            checkSet.add('MapUseError')
        }

        lintOptions.check = checkSet

        lintOptions.textReport = !GroovyUtil.needJira(project)

        return lintOptions
    }

    /*
    创建 Jira
     */
    void create() {
        int errorCount = client.getErrorCount() > Constants.JIRA_NUM ?
                Constants.JIRA_NUM : client.getErrorCount()

        for (Map.Entry<String, String> entry : results.entrySet()) {
            String desc = entry.key
            String title = entry.value
            if (jiraCount < errorCount) {
                // 如果 Jira 库中已经存在内存类问题，且本次扫描再次遇到内存类问题，则不再创建
                // 不影响给原有问题添加备注
                if (createAbort && (title.contains("InnerClassError") || title.contains("MapUseError"))) {
                    continue
                }
                // 问题不存在，每周四创建新问题
                Jira.Issues issues = AndroidClient.getInstance().create(title, desc, Module.getMail(project.name), null, JiraClient.HIGH)
                if (issues == null) {
                    println("创建失败！")
                } else {
                    println("创建成功！\n键值：${issues.key} : 网址：${JiraClient.BASEURL}/browse/${issues.key}\n")
                }
                jiraCount++
                // 如果本次扫描已经创建内存类问题，则不再创建同类问题
                if (title.contains("InnerClassError") || title.contains("MapUseError")) {
                    createAbort = true
                }
            } else {
                break
            }
        }
    }

    /*
    给已有 Jira 添加备注
     */
    void addComment() {
        if (jiraMap != null && !jiraMap.isEmpty()) {
            Set<String> descSet = results.keySet()
            Iterator<Map.Entry<String, Jira.Issues>> it = jiraMap.entrySet().iterator()
            while (it.hasNext()) {
                Map.Entry<String, Jira.Issues> entry = it.next()
                String desc = entry.key
                if (descSet.contains(desc) && desc.contains(project.name)) { // 问题已经存在，走添加备注
                    println("问题已经存在！添加备注提示\n键值：${jiraMap.get(desc).key} : 网址：${JiraClient.BASEURL}/browse/${jiraMap.get(desc).key}\n")
                    if (desc.contains("内存泄漏") || desc.contains("节约内存")) {
                        createAbort = true
                    }
                    AndroidClient.getInstance().addComment(jiraMap.get(desc).key, "此问题还未修复，为了避免影响alpha进度，请您尽快解决；如已修复，请及时修改Jira状态，多谢。")
                    jiraCount++
                    it.remove() // 使用迭代器的remove()方法删除元素
                    results.remove(desc)
                }
            }
        }
    }

    /*
    解决问题
    防止问题解决后重复添加备注
     */
    void resolveIssue() {
        if (jiraMap != null && !jiraMap.isEmpty()) {
            Iterator<Map.Entry<String, Jira.Issues>> it = jiraMap.entrySet().iterator()
            while (it.hasNext()) {
                Map.Entry<String, Jira.Issues> entry = it.next()
                String desc = entry.key
                Jira.Issues issues = entry.value
                if (desc.contains(project.name)) {
                    println("解决遗留 Jira \n键值：${issues.key} : 网址：${JiraClient.BASEURL}/browse/${issues.key}\n")
                    AndroidClient.getInstance().resolveIssue(entry.value.key)
                    it.remove()
                }
            }
        }
    }

}