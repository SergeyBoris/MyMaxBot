package org.example.constants;

import org.example.configs.ConfigLoader;
import org.example.configs.PbfConfig;

import java.util.Set;


public class ConstPbf {
    public static String URL;
    public static String ISSUE_URL;
    public static String AUTHORISATION;
    public static String COOKIE;
    private static final Set<String> CANCELLED_STATUS_IDS;

    static {

        try {
            PbfConfig config =  ConfigLoader.load("pbfConfig.json", PbfConfig.class);

            URL = config.url;
            ISSUE_URL = config.issueUrl;
            AUTHORISATION = config.authorisation;
            COOKIE = config.cookie;
            CANCELLED_STATUS_IDS = config.cancelStatus;



        } catch (Exception e) {
            throw new RuntimeException("Ошибка при инициализации констант: " + e.getMessage(), e);
        }
    }
}
