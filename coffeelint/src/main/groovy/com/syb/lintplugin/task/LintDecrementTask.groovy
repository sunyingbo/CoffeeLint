package com.syb.lintplugin.task

import com.android.build.gradle.internal.dsl.LintOptions
import org.gradle.api.GradleException
import org.gradle.api.tasks.TaskAction

class LintDecrementTask extends BaseLintTask {

    LintDecrementTask() {
        super()
    }

    @TaskAction
    void lintDecrement() {
        preRun()
        try {
            analyze(new ArrayList<>())
        } catch (GradleException e) {
            if (client.haveErrors()) {
                dealResult()
            }
        }
    }

    @Override
    protected LintOptions createLintOptions() {
        LintOptions lintOptions = new LintOptions()

        Set<String> checkSet = new HashSet<>()
        checkSet.add('UnusedResources')
        lintOptions.check = checkSet

        lintOptions.warningsAsErrors = true
        lintOptions.textReport = false

        return lintOptions
    }

    private void dealResult() {
        new Thread({
            Map<String, List<String>> results = XMLUtils.parseLintResult("${project.buildDir}/reports/lint/lint-result-${project.name}.xml")
            if (results == null || results.size() == 0) {
                return
            }
            for (Map.Entry<String, List<String>> entry : results.entrySet()) {
                String resourceId = entry.getKey()
                String resourceName = resourceId.substring(resourceId.lastIndexOf(".") + 1, resourceId.length())
                List<String> files = entry.getValue()
                for (String file : files) {
                    if (resourceId.startsWith("R.layout.")
                            || resourceId.startsWith("R.drawable.")
                            || resourceId.startsWith("R.anim.")
                            || resourceId.startsWith("R.animator.")
                            || resourceId.startsWith("R.mipmap.")
                            || resourceId.startsWith("R.raw.")
                            || resourceId.startsWith("R.menu.")
                            || resourceId.startsWith("R.xml.")
                    ) {
                        CommonUtils.delete(new File(file))
                    } else if (resourceId.startsWith("R.color.")) {
                        XMLUtils.modifyXml(file, ["color", "item"], "name", resourceName)
                    } else if (resourceId.startsWith("R.style.")) {
                        XMLUtils.modifyXml(file, "style", "name", resourceName)
                    } else if (resourceId.startsWith("R.string.")) {
                        XMLUtils.modifyXml(file, "string", "name", resourceName)
                    } else if (resourceId.startsWith("R.dimen.")) {
                        XMLUtils.modifyXml(file, "dimen", "name", resourceName)
                    } else if (resourceId.startsWith("R.integer.")) {
                        XMLUtils.modifyXml(file, "integer", "name", resourceName)
                    } else if (resourceId.startsWith("R.array.")) {
                        XMLUtils.modifyXml(file, "string-array", "name", resourceName)
                    } else if (resourceId.startsWith("R.plurals.")) {
                        XMLUtils.modifyXml(file, "plurals", "name", resourceName)
                    } else if (resourceId.startsWith("R.bool.")) {
                        XMLUtils.modifyXml(file, "bool", "name", resourceName)
                    }
                }
                files.clear()
            }
            results.clear()
        }).start()
    }

}
