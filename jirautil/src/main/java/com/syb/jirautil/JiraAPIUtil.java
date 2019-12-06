package com.syb.jirautil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;

/**
 * JIRA REST API 工具类
 * https://developer.atlassian.com/jiradev/jira-apis/jira-rest-apis/jira-rest-api-tutorials
 * https://docs.atlassian.com/jira/REST/7.0-SNAPSHOT/
 *
 * @author hanqunfeng
 */
public class JiraAPIUtil {

    static String uri = "";
    static String user = "";
    static String pwd = "";
    static String osname = System.getProperty("os.name").toLowerCase();

    /**
     * 执行shell脚本
     *
     * @param command
     * @return
     * @throws IOException
     */
    private static String executeShell(String command) throws IOException {
        StringBuffer result = new StringBuffer();
        Process process = null;
        InputStream is = null;
        BufferedReader br = null;
        String line = null;
        try {

            if (osname.contains("windows")) {
                process = new ProcessBuilder("cmd.exe", "/c", command).start();
                System.out.println("cmd.exe /c " + command); //安装Cygwin，使windows可以执行linux命令
            } else {
                process = new ProcessBuilder("/bin/sh", "-c", command).start();
                System.out.println("/bin/sh -c " + command);
            }

            is = process.getInputStream();
            br = new BufferedReader(new InputStreamReader(is, "UTF-8"));

            while ((line = br.readLine()) != null) {
                System.out.println(line);
                result.append(line);
            }

        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {
            br.close();
            process.destroy();
            is.close();
        }

        return result.toString();
    }

    /**
     * 活动工单信息
     *
     * @param issueKey 工单key
     * @return
     * @throws IOException
     */
    public static String getIssue(String issueKey) throws IOException {

        String command = "curl -D- -u " + user + ":" + pwd
                + " -X GET -H \"Content-Type: application/json\" \"" + uri
                + "/rest/api/2/issue/" + issueKey + "\"";

        String issueSt = executeShell(command);

        return issueSt;

    }

    /**
     * 创建工单
     *
     * @param projectKey  项目key
     * @param issueType   工单类型 name
     * @param description 工单描述
     * @param summary     工单主题
     * @param assignee    工单负责人
     * @param map         工单参数map，key为参数名称，value为参数值，参数值必须自带双引号 比如： map.put("assignee",
     *                    "{\"name\":\"username\"}"); map.put("summary",
     *                    "\"summary00002\"");
     * @return
     * @throws IOException
     */
    public static String createIssue(String projectKey, String issueType,
                                     String description, String summary,
                                     Map<String, String> map) throws IOException {
        String fields = "";
        if (map != null && map.size() > 0) {
            StringBuffer fieldsB = new StringBuffer();
            for (Map.Entry<String, String> entry : map.entrySet()) {
                fieldsB.append(",\"").append(entry.getKey()).append("\":")
                        .append(entry.getValue());
            }
            fields = fieldsB.toString();
        }

        String command = "curl -D- -u " + user + ":" + pwd
                + " -X POST  --data '{\"fields\": {\"project\":{ \"key\": \""
                + projectKey + "\"},\"summary\": \"" + summary
                + "\",\"description\": \"" + description
                + "\",\"issuetype\": {\"name\": \"" + issueType + "\"}"
                + fields + "}}' -H \"Content-Type: application/json\" \"" + uri
                + "/rest/api/2/issue/\"";

        String issueSt = executeShell(command);

        return issueSt;
    }

    /**
     * 更新工单
     *
     * @param issueKey 工单key
     * @param map      工单参数map，key为参数名称，value为参数值，参数值必须自带双引号 比如： map.put("assignee",
     *                 "{\"name\":\"username\"}"); map.put("summary",
     *                 "\"summary00002\"");
     * @return
     * @throws IOException
     */
    public static String editIssue(String issueKey, Map<String, String> map)
            throws IOException {

        StringBuffer fieldsB = new StringBuffer();
        for (Map.Entry<String, String> entry : map.entrySet()) {
            fieldsB.append("\"").append(entry.getKey()).append("\":")
                    .append(entry.getValue()).append(",");
        }
        String fields = fieldsB.toString();
        fields = fields.substring(0, fields.length() - 1);

        String command = "curl -D- -u " + user + ":" + pwd
                + " -X PUT   --data '{\"fields\": { " + fields
                + "}}' -H \"Content-Type: application/json\" \"" + uri
                + "/rest/api/2/issue/" + issueKey + "\"";

        String issueSt = executeShell(command);

        return issueSt;
    }


    /**
     * 查询工单
     *
     * @param jql assignee=username
     *            assignee=username&startAt=2&maxResults=2
     *            assignee=username+order+by+duedate
     *            project=projectKey+order+by+duedate&fields=id,key
     * @return
     * @throws IOException
     */
    public static String searchIssues(String jql) throws IOException {
        String command = "curl -D- -u " + user + ":" + pwd
                + " -X GET -H \"Content-Type: application/json\" \"" + uri
                + "/rest/api/2/search?jql=" + jql + "\"";

        String issueSt = executeShell(command);

        return issueSt;
    }


    /**
     * 为工单增加注释说明
     *
     * @param issueKey 工单key
     * @param comment  注释说明
     * @return
     * @throws IOException
     */
    public static String addComments(String issueKey, String comment) throws IOException {
        String command = "curl -D- -u " + user + ":" + pwd
                + " -X PUT   --data '{\"update\": { \"comment\": [ { \"add\": { \"body\":\"" + comment + "\" } } ] }}' -H \"Content-Type: application/json\" \"" + uri
                + "/rest/api/2/issue/" + issueKey + "\"";

        String issueSt = executeShell(command);

        return issueSt;
    }


    /**
     * 删除工单
     *
     * @param issueKey 工单key
     * @return
     * @throws IOException
     */
    public static String deleteIssueByKey(String issueKey) throws IOException {
        String command = "curl -D- -u " + user + ":" + pwd
                + " -X DELETE -H \"Content-Type: application/json\" \"" + uri
                + "/rest/api/2/issue/" + issueKey + "\"";

        String issueSt = executeShell(command);

        return issueSt;
    }


    /**
     * 上传附件
     *
     * @param issueKey 工单key
     * @param filepath 文件路径
     * @return
     * @throws IOException
     */
    public static String addAttachment(String issueKey, String filepath) throws IOException {
        String command = "curl -D- -u " + user + ":" + pwd
                + " -X POST -H \"X-Atlassian-Token: nocheck\"  -F \"file=@" + filepath + "\" \"" + uri
                + "/rest/api/2/issue/" + issueKey + "/attachments\"";

        String issueSt = executeShell(command);

        return issueSt;
    }

}
