package com.syb.coffeelint.type;

import com.android.annotations.NonNull;
import com.android.tools.lint.detector.api.Issue;
import com.syb.coffeelint.CIssueConfig;

import java.util.Arrays;
import java.util.List;

public enum Type {

    TYPE(Arrays.asList("one", "two"), null, null);

    public final List<String> keys;
    public final Issue issue;
    public final String desc;

    Type(@NonNull List<String> keys, Issue issue, String desc) {
        this.keys = keys;
        this.issue = issue;
        this.desc = desc;
    }

    public static List<String> getMethodKeys() {
        return CIssueConfig.methodKeys;
    }

    public static List<String> getConstructorKeys() {
        return CIssueConfig.constructorKeys;
    }

    public static List<String> getClassKeys() {
        return CIssueConfig.classKeys;
    }

    public static Type getEnumByKey(String key) {
        if (key == null) {
            return null;
        }
        for (Type cType : values()) {
            if (cType.keys == null) {
                continue;
            }
            if (cType.keys.contains(key)) {
                return cType;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return "Type{" +
                "keys=" + keys +
                ", issue=" + issue +
                ", desc='" + desc + '\'' +
                '}';
    }
}
