package com.syb.coffeelint;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.Detector;
import com.android.tools.lint.detector.api.Implementation;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;
import com.syb.coffeelint.detectors.CComDetector;
import com.syb.coffeelint.entity.CIssue;
import com.syb.coffeelint.type.Type;
//import com.syb.coffeelint.utils.EnumHelper;
//import com.syb.lintplugin.jira.JiraClient;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

public class CIssueConfig {

    private static final Category ISSUE_CATEGORY = Category.CORRECTNESS;
    private static final EnumSet<Scope> DETECTOR_SCOPE = Scope.JAVA_FILE_SCOPE;
    private static final Class<? extends Detector> COMMON_DET_CLASS = CComDetector.class;
    private static final Implementation COMMON_IMPL = new Implementation(COMMON_DET_CLASS, DETECTOR_SCOPE);

    public static List<Issue> issue_list;
    public static List<String> methodKeys;
    public static List<String> constructorKeys;
    public static List<String> classKeys;

    public static void init(String json) {
        if (json == null) {
            return;
        }
        issue_list = new ArrayList<>();

        JSONObject source = JSON.parseObject(json);

        JSONObject jiraAccount = source.getJSONObject("jiraAccount");
//        JiraClient.BASEURL = jiraAccount.getString("baseUrl");
//        JiraClient.LOGIN_NAME = jiraAccount.getString("loginName");
//        JiraClient.LOGIN_PASSWORD = jiraAccount.getString("loginPassword");

        JSONArray issueType = source.getJSONArray("issueType");
        List<String> typeList = issueType.toJavaList(String.class);
        for (int i = 0; i < typeList.size(); i++) {
            String type = typeList.get(i);
            createIssue(type, source.getJSONObject(type));
        }

        JSONArray jMethodKeys = source.getJSONArray("methodKeys");
        JSONArray jConstructorKeys = source.getJSONArray("constructorKeys");
        JSONArray jClassKeys = source.getJSONArray("classKeys");

        methodKeys = jMethodKeys.toJavaList(String.class);
        constructorKeys = jConstructorKeys.toJavaList(String.class);
        classKeys = jClassKeys.toJavaList(String.class);

    }

    private static void createIssue(String typeName, JSONObject jsonObject) {
        CIssue cIssue = JSON.parseObject(jsonObject.toJSONString(), CIssue.class);
        Issue issue = Issue.create(cIssue.id, cIssue.description, cIssue.explanation, ISSUE_CATEGORY, cIssue.priority, getSeverity(cIssue.severity), COMMON_IMPL);
        issue_list.add(issue);
//        EnumHelper.addEnum(Type.class, typeName.toUpperCase(),
//                new Class[]{List.class, Issue.class, String.class},
//                new Object[]{cIssue.keys, issue, cIssue.description});

    }

    private static Severity getSeverity(String severity) {
        switch (severity) {
            case "Error":
                return Severity.ERROR;
        }
        return null;
    }

}
