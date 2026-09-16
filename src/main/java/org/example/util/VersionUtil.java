package org.example.util;

import lombok.Getter;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class VersionUtil {
    @Getter
    private static String version = "dev";
    @Getter
    private static String appName = "MyMaxBot";
    @Getter
    private static String buildTime = "unknown";

    static {
        loadVersion();
    }

    private static void loadVersion() {
        try (InputStream is = VersionUtil.class
                .getClassLoader()
                .getResourceAsStream("version.properties")) {

            if (is == null) {
                System.out.println("⚠️ version.properties не найден, используется dev-версия");
                return;
            }

            Properties props = new Properties();
            props.load(is);

            version = props.getProperty("app.version", "dev");
            appName = props.getProperty("app.name", "MyMaxBot");
            buildTime = props.getProperty("app.build.time", "unknown");

        } catch (IOException e) {
            System.err.println("⚠️ Ошибка загрузки версии: " + e.getMessage());
        }
    }

    public static String getFullInfo() {
        return appName + " v" + version + " (собрано: " + buildTime + ")";
    }
}