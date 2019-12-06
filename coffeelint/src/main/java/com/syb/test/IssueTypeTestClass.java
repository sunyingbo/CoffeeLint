package com.syb.test;

import com.android.tools.lint.detector.api.Issue;
import com.syb.coffeelint.CIssueRegister;
import com.syb.coffeelint.type.Type;
import com.syb.coffeelint.utils.JavaUtil;

import java.util.List;

public class IssueTypeTestClass {

    public static void main(String[] args) {
//        List<Issue> issues = CIssueRegister.getInstance().getIssues();
//        System.out.println(issues);
//
//        Type type = Type.getEnumByKey("print");
//        System.out.println(type);
//        System.out.println(Type.valueOf("SERVICE"));

        System.out.println(JavaUtil.getUrl());
    }

}
