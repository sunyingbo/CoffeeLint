package com.syb.lintplugin.constants;

public class Constants {

    // 控制是否在 commit/build/assemble/install 时进行 lint 检查
    public static final String ANDROID_LINT = "AndroidLint";
    // 控制是否操作 Jira
    public static final String ANDROID_JIRA = "AndroidJira";

    /**
     * lintCommon 使用
     * 用来控制 lint 检测范围，为空时全部检测
     * 格式：例：HEAD～10
     */
    public static final String CHECK_LINE = "";

    public static final int JIRA_NUM = 5;

    /**
     * git 命令中使用
     * 用来过滤需要检测的文件类型
     * 需配合 Detector 扫描的文件类型使用
     */
    public static final String[] FILE_TYPE = new String[]{"'*.java'"};

    public static final String JIRA_PROJECT_ID = "";
    public static final String JIRA_PROJECT_NAME = "";
    public static final String JIRA_PROJECT_KEY = "";

}
