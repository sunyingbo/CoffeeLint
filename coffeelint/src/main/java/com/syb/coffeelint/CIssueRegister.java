package com.syb.coffeelint;

import com.android.tools.lint.client.api.IssueRegistry;
import com.android.tools.lint.detector.api.Issue;
import com.syb.coffeelint.utils.JavaUtil;

import java.util.List;

public class CIssueRegister extends IssueRegistry {

    {
        CIssueConfig.init(JavaUtil.getResource());
    }

    private static CIssueRegister instance;

    private CIssueRegister() {}

    public static CIssueRegister getInstance() {
        if (instance == null) {
            instance = new CIssueRegister();
        }
        return instance;
    }

    @Override
    public List<Issue> getIssues() {
        return CIssueConfig.issue_list;
    }

}
