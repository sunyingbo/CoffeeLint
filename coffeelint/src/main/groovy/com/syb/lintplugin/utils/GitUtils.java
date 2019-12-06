package com.syb.lintplugin.utils;

import com.syb.lintplugin.constants.Constants;

import org.gradle.api.Project;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class GitUtils {

    private static final String GET_CACHE_FILE_LIST = "git diff --cached --name-only --diff-filter=ACMRT";
    private static final String GET_FILE_LIST = "git diff --name-only --diff-filter=ACMRT %s HEAD~0";
    private static final String GET_FILE_LIST_NEW = "git diff --name-only --diff-filter=ACMRT";
    private static final String GIT_RESET = "git reset --mixed HEAD";
    private static final String GIT_BRANCH_NAME = "git symbolic-ref --short -q HEAD";

    // 获取当前没有提交的（暂存区）文件列表
    public static List<String> getCachedFiles(Project project) {
        return filePathList(project,
                getCmdResult(project, getGitCmd(GET_CACHE_FILE_LIST)));
    }

    // 获取修改的（工作区）文件列表
    public static List<String> getChangedFiles(Project project) {
        return filePathList(project,
                getCmdResult(project, getGitCmd(GET_FILE_LIST_NEW)));
    }

    // 获取指定范围的（历史）文件列表
    public static List<String> getChangedFiles(Project project, String head) {
        String cmd = String.format(GET_FILE_LIST, head);
        return filePathList(project,
                getCmdResult(project, getGitCmd(cmd)));
    }

    // 获取分支名
    public static String getGitBranchName(Project project) {
        List<String> validLineList = getCmdResult(project, GIT_BRANCH_NAME);
        if (validLineList == null || validLineList.size() == 0) {
            return "";
        }
        return validLineList.get(0);
    }

    private static List<String> filePathList(Project project, List<String> infoList) {
        String subModuleDir = project.getProjectDir().getAbsolutePath();
        List<String> validLineList = new ArrayList<>();
        if (infoList == null || infoList.size() == 0) {
            return validLineList;
        }
        for (String line : infoList) {
            if (line.contains("androidTest")) // 过滤掉 androidTest 目录下的文件
                continue;
            line = line.startsWith("allmodules") ? line.substring(11, line.length()) : line;
            String filePath = FileUtil.getModuleDir(project) + "/" + line;
            if (filePath.startsWith(subModuleDir)) {
                validLineList.add(filePath);
            }
        }
        return validLineList;
    }

    private static List<String> getCmdResult(Project project, String command) {
        // 执行 git 命令前，进入对应模块目录
        StringBuilder sb = new StringBuilder();
        sb.append("cd ");
        sb.append(FileUtil.getModuleDir(project));
        sb.append(" && ");
        sb.append(command);

        Process process = runCmd(sb.toString());
        if (process == null)
            return new ArrayList<>();
        validErrorInfo(process);
        return infoList(process);
    }

    private static List<String> infoList(Process process) {
        List<String> validLineList = new ArrayList<>();

        InputStream inputStream = null;
        InputStreamReader inputStreamReader = null;
        BufferedReader bufferedReader = null;
        try {
            inputStream = process.getInputStream();
            inputStreamReader = new InputStreamReader(inputStream);
            bufferedReader = new BufferedReader(inputStreamReader);
            String line = null;
            while ((line = bufferedReader.readLine()) != null) {
                validLineList.add(line);
            }
            return validLineList;
        } catch (IOException e) {
            System.out.println(e.getMessage());
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (inputStreamReader != null) {
                try {
                    inputStreamReader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (bufferedReader != null) {
                try {
                    bufferedReader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return validLineList;
    }

    private static void validErrorInfo(Process process) {
        InputStream isError = null;
        InputStreamReader isrError = null;
        BufferedReader brError = null;
        try {
            isError = process.getErrorStream();
            isrError = new InputStreamReader(isError);
            brError = new BufferedReader(isrError);
            String error = null;
            while ((error = brError.readLine()) != null) {
                System.out.println("***************************************");
                System.out.println(error);
//                System.out.println("请安装 XCode ，使用：xcode-select --install");
                System.out.println("****************************************");
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println(e.getMessage());
        } finally {
            if (isError != null) {
                try {
                    isError.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (isrError != null) {
                try {
                    isrError.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (brError != null) {
                try {
                    brError.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    // 提交失败后撤销 add
    public static void gitReset(Project project) {
        StringBuilder sb = new StringBuilder();
        sb.append("cd ");
        sb.append(FileUtil.getModuleDir(project));
        sb.append(" && ");
        sb.append(GIT_RESET);
        runCmd(sb.toString());
    }

    public static Process runCmd(String command) {
        try {
            return Runtime.getRuntime().exec(new String[]{"/bin/sh", "-c", command});
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println(e.getMessage());
        }
        return null;
    }

    /**
     * 拼接 git 命令
     * @param cmd 初始 git 命令
     * @return 完整 git 命令
     */
    private static String getGitCmd(String cmd) {
        StringBuilder sb = new StringBuilder(cmd);
        if (Constants.FILE_TYPE.length > 0) {
            sb.append(" -- ");
            for (int i = 0; i < Constants.FILE_TYPE.length; i++) {
                sb.append(Constants.FILE_TYPE[i]);
                sb.append(" ");
            }
        }
        return sb.toString();
    }

}
