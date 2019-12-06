package com.syb.lintplugin

import com.android.build.gradle.internal.LintGradleClient
import com.android.builder.model.AndroidProject
import com.android.builder.model.Variant
import com.android.sdklib.BuildToolInfo
import com.android.tools.lint.LintCliFlags
import com.android.tools.lint.client.api.IssueRegistry
import com.android.tools.lint.client.api.LintRequest
import com.android.tools.lint.detector.api.Project
import com.syb.coffeelint.CIssueRegister

class CoffeeLintClient extends LintGradleClient {

    private org.gradle.api.Project project

    CoffeeLintClient(CIssueRegister registry, LintCliFlags flags, org.gradle.api.Project gradleProject, AndroidProject modelProject, File sdkHome, Variant variant, BuildToolInfo buildToolInfo) {
         super(registry, flags, gradleProject, modelProject, sdkHome, variant, buildToolInfo)
         this.project = gradleProject
    }

    @Override
    protected LintRequest createLintRequest(List<File> files) {
        LintRequest lintRequest =  super.createLintRequest(files)
        if (files.size() == 0)
            return lintRequest
        String subModuleDir = project.projectDir.absolutePath
        Collection<Project> projects = new ArrayList<>()
        for (Project project : lintRequest.getProjects()) {
            for (int i = 0; i < files.size(); i++) {
                File changeFile = files.get(i)
                if (changeFile.absolutePath.startsWith(subModuleDir)) {
                    project.addFile(changeFile)
                }
            }
            projects.add(project)
        }
        lintRequest.setProjects(projects)
        return lintRequest
    }

    @Override
    int run(IssueRegistry registry, List<File> files) throws IOException {
        return super.run(registry, files)
    }

    @Override
    String readFile(File file) {
        return super.readFile(file)
    }

    int getErrorCount() {
        return mErrorCount
    }

    int getWarningCount() {
        return mWarningCount
    }
}