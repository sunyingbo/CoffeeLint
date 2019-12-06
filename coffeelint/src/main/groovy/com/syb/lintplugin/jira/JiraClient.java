package com.syb.lintplugin.jira;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.syb.lintplugin.net.WebRequest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import okhttp3.Credentials;
import okhttp3.Headers;
import okhttp3.RequestBody;

public abstract class JiraClient {

    public static String BASEURL = "";
    public static String LOGIN_NAME = "";
    public static String LOGIN_PASSWORD = "";

    public static Jira.Priority HIGHEST = new Jira.Priority("1", "Highest");
    public static Jira.Priority HIGH = new Jira.Priority("2", "High");
    public static Jira.Priority MEDIUM = new Jira.Priority("3", "Medium");
    public static Jira.Priority LOW = new Jira.Priority("4", "Low");

    Jira.IssueType IMPROVEMENT = new Jira.IssueType("10201", "改进");
    Jira.IssueType BUG = new Jira.IssueType("10203", "Bug");

    abstract Jira.Project getProject();

    public int getJiraId(String id) {
        return Integer.parseInt(id.split("-")[1]);
    }

    public Jira.Issues create(String summary, String description, String assignee, Jira.Version version, Jira.Priority priority) {
        RequestBody requestBody = WebRequest.getJSONRequestBody(createParam(summary, description, assignee, version, priority));
        String webData = WebRequest.getInstance().postRequest(createUrl(), requestBody, defaultHeader());
        return webData == null ? null : JSONObject.parseObject(webData, Jira.Issues.class);
    }

    public Jira.Issues find(String jiraId) {
        String webData = WebRequest.getInstance().getRequest(getUrl(jiraId), defaultHeader());
        if (webData == null) {
            return null;
        } else {
            return JSON.parseObject(webData, Jira.Issues.class);
        }
    }

    public Jira search(String jql) {
        String webData = WebRequest.getInstance().getRequest(searchUrl(jql), defaultHeader());
        if (webData == null) {
            return null;
        } else {
            return JSON.parseObject(webData, Jira.class);
        }
    }

    public void addComment(String jiraId, String comment) {
        WebRequest.getInstance().putRequest(getUrl(jiraId), WebRequest.getJSONRequestBody(addParam("comment", "body", comment)), defaultHeader());
    }

    public void resolveIssue(String jiraId) {
        WebRequest.getInstance().postRequest(updateUrl(jiraId), WebRequest.getJSONRequestBody(resolveParam()), defaultHeader());
    }

    public void reopenIssue(String jiraId) {
        WebRequest.getInstance().postRequest(updateUrl(jiraId), WebRequest.getJSONRequestBody(reopenParam()), defaultHeader());
    }

    private Headers defaultHeader() {
        Headers.Builder headers = new Headers.Builder();
        headers.add("Authorization", Credentials.basic(LOGIN_NAME, LOGIN_PASSWORD));
        return headers.build();
    }

    private String createUrl() {
        return BASEURL + "/rest/api/2/issue";
    }

    private String getUrl(String jiraId) {
        return BASEURL + "/rest/api/2/issue/" + jiraId;
    }

    private String searchUrl(String jql) {
        return BASEURL + "/rest/api/2/search?jql=" + jql;
    }

    private String updateUrl(String jiraId) {
        return BASEURL + "/rest/api/2/issue/" + jiraId + "/transitions?expand=transitions.fields";
    }

    private String resolveParam() {
        Map<String, String> param = new HashMap<>();
        param.put("transition", "5");
        return JSON.toJSONString(param);
    }

    private String reopenParam() {
        Map<String, String> param = new HashMap<>();
        param.put("transition", "3");
        return JSON.toJSONString(param);
    }

    private String addParam(String field, String contentField, String content) {
        JSONObject jsonObject = new JSONObject();
        JSONObject parent = new JSONObject();
        JSONArray fixField = new JSONArray();
        jsonObject.put(contentField, content);
        parent.put("add", jsonObject);
        fixField.add(parent);
        JSONObject updateContent = new JSONObject();
        updateContent.put(field, fixField);
        JSONObject sendData = new JSONObject();
        sendData.put("update", updateContent);
        return JSON.toJSONString(sendData);
    }

    private String createParam(String summary, String description, String assignee, Jira.Version version, Jira.Priority priority) {
        Jira.Fields fields = new Jira.Fields();
        fields.project = getProject();
        fields.issuetype = BUG;

        if (priority == null) {
            priority = HIGH;
        }
        fields.priority = priority;

        fields.summary = summary;
        fields.description = description;

        if (version != null) {
            fields.versions = new ArrayList<>();
            fields.versions.add(version);
        }

        Jira.User assignUser = new Jira.User();
        assignUser.name = assignee;
        fields.assignee = assignUser;

        JSONObject sendData = new JSONObject();
        sendData.put("fields", fields);
        return JSON.toJSONString(sendData);
    }

}
