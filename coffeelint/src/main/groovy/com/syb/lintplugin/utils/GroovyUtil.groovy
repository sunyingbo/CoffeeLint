package com.syb.lintplugin.utils

import com.syb.lintplugin.constants.Constants
import org.gradle.api.Project

class GroovyUtil {

    static boolean needLintCheck(Project project) {
        boolean androidLint = true
        if (project.hasProperty(Constants.ANDROID_LINT)) {
            String value = project.property(Constants.ANDROID_LINT)
            if (!"".equals(value) && "false".equals(value.toLowerCase())) {
                androidLint = false
            }
        }
        return androidLint
    }

    static boolean needJira(Project project) {
        boolean androidJira = false
        if (project.hasProperty(Constants.ANDROID_JIRA)) {
            String value = project.property(Constants.ANDROID_JIRA)
            if (!"".equals(value) && "true".equals(value.toLowerCase())) {
                androidJira = true
            }
        }
        return androidJira
    }

    static void copyAndRun(File fileType, String filePath) {
        if (fileType.exists())
            fileType.delete()
        String sourcePath = FileUtil.getUrl().path
        File source = new File(sourcePath)
        FileUtil.copyResourceFile(source, fileType)
        String command = "chmod -R +x " + filePath + "/.git/hooks/"
        GitUtils.runCmd(command)
    }

    static void delete(File fileType) {
        if (fileType.exists())
            fileType.delete()
    }

    static String encode(String userName, String password) {
        String auth = userName + ":" + password
        Base64.Encoder encoder = Base64.getEncoder()
        byte[] rel = auth.getBytes("UTF-8")
        String encoded = encoder.encodeToString(rel)
        return "Basic " + encoded
    }

    static String getDate() {
        return new Date().toString()
    }

    /**
     * 执行shell脚本
     *
     * @param command
     * @return
     * @throws IOException
     */
    static void executeShell(String command) throws IOException {
        StringBuffer result = new StringBuffer()
        Process process = null
        InputStream is = null
        BufferedReader br = null
        try {

            process = new ProcessBuilder("/bin/sh", "-c", command).start()
            System.out.println("/bin/sh -c " + command)

            is = process.getInputStream()
            br = new BufferedReader(new InputStreamReader(is, "UTF-8"))

            String line
            while ((line = br.readLine()) != null) {
                result.append(line)
            }

            System.out.println(result.toString())
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace()
        } finally {
            br.close()
            process.destroy()
            is.close()
        }
    }

}
