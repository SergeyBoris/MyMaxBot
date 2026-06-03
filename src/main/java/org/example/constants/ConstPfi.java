package org.example.constants;

import org.example.configs.Config;
import org.example.configs.ConfigLoader;
import org.example.configs.PfiConfig;

public class ConstPfi {

    public static String SHOW_REQ_PAYLOAD;
    public static String PFI_LOGIN;
    public static String PFI_PASSWORD;
    public static String USER_AGENT;
    public static String NEW_PARAM_INSTANCE = "newUndefinedParam";

    static {
        try {
            PfiConfig pfiConfig = ConfigLoader.load("pfiConfig.json", PfiConfig.class);

            SHOW_REQ_PAYLOAD = pfiConfig.showReqPayload;
            PFI_LOGIN = pfiConfig.pfiLogin;
            PFI_PASSWORD = pfiConfig.pfiPassword;
            USER_AGENT = pfiConfig.userAgent;


        }catch (Exception e) {
            throw new RuntimeException(e);
        }
    }



}
