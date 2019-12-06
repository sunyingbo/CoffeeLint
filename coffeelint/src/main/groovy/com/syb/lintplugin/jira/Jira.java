package com.syb.lintplugin.jira;

import java.util.List;

public class Jira {

    public int total;
    public List<Issues> issues;

    public static class Issues {
        public String id;
        public String self;
        public String key;
        public Fields fields;
    }

    public static class Fields {
        public Project project;
        public IssueType issuetype;
        public List<Version> versions;
        public User assignee;
        public Status status;
        public String description;
        public String summary;
        public Priority priority;
        public User creator;
        public User reporter;
        public Resolution resolution;
    }

    public static class Version {
        public String id;
        public String description;
        public String name;
        public boolean released;

        public Version() {
        }

        public Version(String name) {
            this.name = name;
        }
    }

    public static class IssueType {
        public String id;
        public String name;

        public IssueType(String id, String name) {
            this.id = id;
            this.name = name;
        }

        public IssueType() {
        }
    }

    public static class User {
        public String name;
        public String key;
        public String emailAddress;
    }

    public static class Project {
        public String id;
        public String name;
        public String key;

        public Project(String id, String name, String key) {
            this.id = id;
            this.name = name;
            this.key = key;
        }

        public Project() {
        }
    }

    public static class Priority {
        public String id;
        public String name;

        public Priority(String id, String name) {
            this.id = id;
            this.name = name;
        }

        public Priority() {
        }
    }

    public static class Status {
        public String id;
        public String name;

        public static String translate(Status status) {
            switch (status.id) {
                case "1":
                    return "未处理";
                case "3":
                    return "处理中";
                case "4":
                    return "已重开";
                case "5":
                    return "已解决";
                case "6":
                    return "已关闭";
                default:
                    return "未知状态";
            }
        }
    }

    public static class Resolution{
//        "self":"http://",
//        "id":"10207",
//        "description":"",
//        "name":"延迟处理(本项目直接)"
        public String name;
    }


}
