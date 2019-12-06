package com.syb.lintplugin.jira;

import com.syb.lintplugin.constants.Constants;

public class AndroidClient extends JiraClient {

    public static final Jira.Project ANDROID = new Jira.Project(Constants.JIRA_PROJECT_ID, Constants.JIRA_PROJECT_NAME, Constants.JIRA_PROJECT_KEY);

    private AndroidClient() {}

    public static AndroidClient getInstance() {
        return InstanceHelper.instance;
    }

    @Override
    public Jira.Project getProject() {
        return ANDROID;
    }

    private static class InstanceHelper {
        private static AndroidClient instance = new AndroidClient();
    }

}
