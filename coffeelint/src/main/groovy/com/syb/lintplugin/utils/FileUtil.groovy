package com.syb.lintplugin.utils

import org.gradle.api.Project

class FileUtil {

    private static final String preCommitFile = "config/pre-commit" //jar下的配置文件

    static URL getUrl() {
        return FileUtil.class.getClassLoader().getResource(preCommitFile)
    }

    static void createPreCommitFile(String source, File dest) {
        File parent = dest.getParentFile()
        if (parent != null && (!parent.exists())) {
            parent.mkdirs()
        }
        FileWriter fw = new FileWriter(dest)
        fw.write(source)
        fw.close()
    }

    // 获取 module 路径
    static String getModuleDir(Project project) {
        return project.getProjectDir().absolutePath
    }

    // 获取 Property
    static String getProperty(Project project, String fileName, String propertyKey) {
        Properties properties = new Properties()
        InputStream inputStream = project.rootProject.file(fileName).newDataInputStream()
        properties.load(inputStream)
        return properties.getProperty(propertyKey)
    }

    static boolean copyResourceFile(File source, File dest) {
        File parent = dest.getParentFile()
        if (parent != null && (!parent.exists())) {
            parent.mkdirs()
        }

        InputStream is = null
        FileOutputStream os = null
        try {
            is = new FileInputStream(source)
            os = new FileOutputStream(dest, false)

            byte[] buffer = new byte[1024]
            int length
            while ((length = is.read(buffer)) != -1) {
                os.write(buffer, 0, length)
            }
        } catch (Exception e) {
            System.out.println(source.getName() + " 文件复制失败")
        } finally {
            if (is != null) {
                is.close()
            }
            if (os != null) {
                os.close()
            }
        }

        if (dest.exists())
            return true
        return false
    }

}