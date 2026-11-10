package org.example.spring.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@Data
@ConfigurationProperties(prefix = "bot")
public class BotProperties {

    private String password;
    private String token;
    private String url = "https://platform-api2.max.ru";
    private long updateDelayMs = 1000;
    private long checkNewRequestDelayMs = 30000;

//    private Ollama ollama = new Ollama();
    private Mail mail = new Mail();
    private List<Contragent> contragents = new ArrayList<>();

//    @Data
//    public static class Ollama {
//        private String host = "http://localhost:11434";
//        private String model = "jkh-assistant";
//    }

    @Data
    public static class Mail {
        private String imapHost;
        private int imapPort = 993;
        private String user;
        private String password;
        private String folder = "INBOX";
        private boolean debug = false;
    }

    @Data
    public static class Contragent {
        private String name;
        private boolean enabled = true;
    }
}