package com.syb.coffeelint.entity;

import java.util.List;

public class CIssue {

    public String type;
    public List<String> keys;
    public String id;
    public int priority;
    public String severity;
    public String description;
    public String explanation;

    @Override
    public String toString() {
        return "CIssue{" +
                "type='" + type + '\'' +
                ", keys=" + keys +
                ", id='" + id + '\'' +
                ", priority=" + priority +
                ", severity='" + severity + '\'' +
                ", description='" + description + '\'' +
                ", explanation='" + explanation + '\'' +
                '}';
    }
}
