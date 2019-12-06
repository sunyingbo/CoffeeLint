package com.syb.lintplugin

import com.syb.lintplugin.task.LintCommonTask
import com.syb.lintplugin.utils.FileUtil
import com.syb.lintplugin.utils.GroovyUtil
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task

class CoffeeLintPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        initGitHooks(project)
        initTask(project)
    }

    private void initGitHooks(Project project) {
        String projectDir = project.rootDir.absolutePath
        String moduleDir = FileUtil.getModuleDir(project)
        File preFilePro = new File(projectDir, ".git/hooks/pre-commit")
        File preFileMod = new File(moduleDir, ".git/hooks/pre-commit")
        if (!GroovyUtil.needLintCheck(project)) {
            GroovyUtil.delete(preFilePro)
            GroovyUtil.delete(preFileMod)
        } else {
            // 工程目录
            GroovyUtil.copyAndRun(preFilePro, projectDir)
            // allmodules/res ... 目录
            GroovyUtil.copyAndRun(preFileMod, moduleDir)
        }
    }

    private void initTask(Project project) {
        // lint 全量和指定范围检查，编译/打包时使用
        addLintCommonTask(project)

        if (!GroovyUtil.needJira(project)) {
            // lint 增量检查，commit 时使用
            addLintIncrementTask(project)

            // lint 扫描删除无用资源
            addLintDecrementTask(project)
        }
    }

    private void addLintIncrementTask(Project project) {
        project.afterEvaluate {
            project.tasks.create('lintIncrement', LintIncrementTask.class)
        }
    }

    private void addLintCommonTask(Project project) {
        project.afterEvaluate {
            LintCommonTask lintCommonTask = project.tasks.create('lintCommon', LintCommonTask.class)
            if (GroovyUtil.needLintCheck(project)) {
                adjustTaskDependsOn(project, lintCommonTask)
            }
        }
    }

    private void addLintDecrementTask(Project project) {
        project.afterEvaluate {
            project.tasks.create('lintDecrement', LintDecrementTask.class)
        }
    }

    private void adjustTaskDependsOn(Project project, Task task) {
        def preBuild = project.tasks.findByName("preBuild")

        if (preBuild == null) {
            throw new GradleException("lint need depend on preBuild")
        }

        preBuild.finalizedBy task
    }

}