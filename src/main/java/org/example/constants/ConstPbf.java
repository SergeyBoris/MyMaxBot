package org.example.constants;

import org.example.util.ConfigLoader;
import org.example.util.PbfConfig;



public class ConstPbf {
    public static String URL;
    public static String AUTHORISATION;
    public static String COOKIE;

    static {

        try {
            PbfConfig config =  ConfigLoader.load("config.json", PbfConfig.class);

            URL = config.url;
            AUTHORISATION = config.authorisation;
            COOKIE = config.cookie;



        } catch (Exception e) {
            throw new RuntimeException("Ошибка при инициализации констант: " + e.getMessage(), e);
        }
    }
}
