package com.syb.lintplugin.task

import com.android.build.gradle.internal.dsl.LintOptions
import com.syb.lintplugin.utils.GroovyUtil
import org.gradle.api.GradleException
import org.gradle.api.tasks.TaskAction

/**
 * Lint 增量检查任务
 * commit 时自动执行
 */
class LintIncrementTask extends BaseLintTask {

    private List<File> changeFileList
    private File lintResultFile

    LintIncrementTask() {
        super()
    }

    @TaskAction
    void lintIncrement() {
        preRun()
        if (!needCheck())
            return
        try {
            analyze(changeFileList)
        } catch (GradleException e) {
            FileWriter fw = new FileWriter(lintResultFile, true)
            fw.write(e.getMessage())
            fw.write("\n")
            fw.close()
        }
    }

    @Override
    protected void preRun() {
        super.preRun()
        lintResultFile = new File(FileUtil.getModuleDir(project) + "/lint_result.txt")
        if (lintResultFile.exists())
            lintResultFile.delete()

        changeFileList = new ArrayList<>()
        List<String> changeFilePathList = GitUtils.getCachedFiles(project)
        for (int i = 0; i < changeFilePathList.size(); i++) {
            changeFileList.add(new File(changeFilePathList.get(i)))
        }
    }

    // 如果文件列表为空，则不检查
    private boolean needCheck() {
        if (changeFileList.size() == 0) {
            return false
        }
        return true
    }

    @Override
    protected LintOptions createLintOptions() {
        LintOptions lintOptions = new LintOptions()

        Set<String> checkSet = new HashSet<>()
        checkSet.add('MessageObtainUseError')
        checkSet.add('NewApiError')
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

        lintOptions.textReport = false

        return lintOptions
    }

}
