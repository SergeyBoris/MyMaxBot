package org.example.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.mail.*;
import jakarta.mail.search.FlagTerm;
import org.example.constants.Const;

import java.util.Properties;

public class EmailMonitorService {

    private Store store;
    private Folder inbox;

    public EmailMonitorService() {

    }

    public void connect() throws Exception {
        Properties properties = new Properties();
        properties.put("mail.debug", Const.MAIL_DEBUG);
        properties.put("mail.imap.host", Const.MAIL_IMAP_HOST);
        properties.put("mail.imap.port", Const.MAIL_IMAP_PORT);
        properties.put("mail.imaps.ssl.enable", Const.MAIL_IMAPS_SSL_ENABLE); // для порта 993
        properties.put("mail.imaps.ssl.protocols", Const.MAIL_IMAPS_SSL_PROTOCOLS); // поддерживаемые протоколы
        properties.put("mail.imap.timeout", Const.MAIL_IMAP_TIMEOUT);
        properties.put("mail.imap.connectionTimeout", Const.MAIL_IMAP_CONNECTION_TIMEOUT);
        properties.put("mail.imap.auth", Const.MAIL_IMAP_AUTH);

        Session session = Session.getInstance(properties);
        // session.setDebug(true);

        store = session.getStore("imaps");
        store.connect(Const.MAIL_IMAP_HOST, Const.MAIL_USER_NAME, Const.MAIL_PASSWORD);

        inbox = store.getFolder(Const.MAIL_FOLDER_TO_SCAN);
        inbox.open(Folder.READ_WRITE);
    }

    public Message[] checkNewMessages() throws Exception {
        if (inbox == null || !inbox.isOpen()) {
            connect();
        }

        // Ищем непрочитанные сообщения
        Message[] messages = inbox.search(
                new FlagTerm(new Flags(Flags.Flag.SEEN), false)
        );

        return messages;
    }

    public void disconnect() throws Exception {
        if (inbox != null && inbox.isOpen()) {
            inbox.close(true);
        }
        if (store != null && store.isConnected()) {
            store.close();
        }
    }

    //    public String processNewEmail(Message message) throws Exception {
//        StringBuilder sb = new StringBuilder();
//
//        System.out.println("Новое письмо:");
//        sb.append("От: ").append(message.getFrom()[0]).append("\n");
//        sb.append("Тема: ").append(message.getSubject()).append("\n");
//
//        sb.append(getEmailBody(message));
//       return sb.toString();
//    }
    public String getEmailBody(Message message) throws Exception {
        Object content = message.getContent();

        if (content instanceof String) {
            // Простое текстовое письмо
            return (String) content;
        } else if (content instanceof Multipart) {
            // Составное письмо (MIME)
            return getTextFromMultipart((Multipart) content);
        }

        return "Не удалось извлечь тело письма";
    }

    private String getTextFromMultipart(Multipart multipart) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        StringBuilder textBuilder = new StringBuilder();

        for (int i = 0; i < multipart.getCount(); i++) {
            BodyPart bodyPart = multipart.getBodyPart(i);
            String contentType = bodyPart.getContentType();

            if (bodyPart.isMimeType("text/plain")) {
                // Текстовая часть
                textBuilder.append(normalizeToJson(bodyPart.getContent().toString()));
            } else if (bodyPart.isMimeType("text/html")) {
                // HTML‑часть — преобразуем в текст
                String htmlContent = bodyPart.getContent().toString();
                textBuilder.append(normalizeToJson(htmlToText(htmlContent)));
            } else if (bodyPart.getContent() instanceof Multipart) {
                // Вложенная Multipart‑структура
                textBuilder.append(getTextFromMultipart((Multipart) bodyPart.getContent()));
            }
            // Вложения (attachments) пропускаем — они не входят в тело письма
        }

        return mapper.writeValueAsString(textBuilder.toString()).replace("\"", "");
        // return textBuilder.toString();
    }

    private String htmlToText(String html) {
        return html;
//                .replaceAll("&nbsp;", " ")
//                .replaceAll("<[^>]*>", "")  // Удаляем все теги
//                .replaceAll("\\s+", " ")     // Заменяем множественные пробелы на один
//                .trim();
    }

    private String normalizeToJson(String badText) {
        badText = badText.replace("\"", " ").replace("'", " ");

//        badText = badText.replaceAll("\\n", " ");
        int htmlBegin = !badText.contains("<HTML>") ? badText.indexOf("<html>"): -1 ;
        int htmlEnd = !badText.contains("</HTML>")? badText.indexOf("</html>") : -1;
        if (htmlBegin != -1 || htmlEnd != -1) {
            htmlEnd += "</HTML>".length();
            badText = badText.substring(0, htmlBegin) + badText.substring(htmlEnd);
        }

        int resentIndex = badText.indexOf("Пересылаемое сообщение");
        if (resentIndex != -1) {
            badText = badText.substring(resentIndex);
            int tema = badText.indexOf("Тема");
            if (tema != -1) {
                badText = badText.substring(tema);
                int newlineIndex = badText.indexOf('\n');
                if (newlineIndex != -1) {
                    badText = badText.substring(newlineIndex + 1).trim();
                }
            }
        }

        return badText;


    }
}
