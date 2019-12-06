package com.syb.jirautil;

import com.atlassian.jira.rest.client.domain.BasicIssue;
import com.atlassian.jira.rest.client.domain.Issue;

import java.net.URISyntaxException;

public class JiraManager {

    private static String jqlUnresolved = "project=WBANDROIDLINT AND resolution=Unresolved";
    private static String comment = "此问题还未修复，为了避免影响alpha进度，请您尽快解决，多谢。";

    public static void main(String[] args) {
        try {
            Iterable<BasicIssue> basicIssues = JiraUtil.searchIssues(jqlUnresolved, 0, 200);
            for (BasicIssue basicIssue : basicIssues) {
                Issue issue = JiraUtil.getIssue(basicIssue.getKey());
                System.out.println(issue.getSelf());
//                JiraUtil.changeIssueStatus(issue.getKey(), 5, null);
//                System.out.println(issue.getCommentsUri());
//                JiraUtil.addComment(issue, comment);
            }
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

}
