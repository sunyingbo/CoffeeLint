package com.syb.coffeelint.utils;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;

public class JavaUtil {

    private static final String lintConfigFile = "config/lint-config.json"; //jar下的配置文件

    public static URL getUrl() {
        return JavaUtil.class.getClassLoader().getResource(lintConfigFile);
    }

    public static String getResource() {
        URL resource = getUrl();
        if (resource == null) {
            return null;
        }
        try {
            InputStream is = resource.openStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            StringBuilder stringBuilder = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                stringBuilder.append(line);
            }
            return stringBuilder.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

}
