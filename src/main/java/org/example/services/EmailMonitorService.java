package org.example.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.mail.*;
import jakarta.mail.internet.MimeUtility;
import jakarta.mail.search.FlagTerm;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.example.constants.Const;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
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
            return cleanText((String) content);
        } else if (content instanceof Multipart) {
            // Составное письмо (MIME)
            return extractTextFromMultipart((Multipart) content);
        }

        return "Не удалось извлечь тело письма";
    }

    private String extractTextFromMultipart(Multipart multipart) throws Exception {
        StringBuilder textContent = new StringBuilder();
        boolean textFound = false;

        for (int i = 0; i < multipart.getCount(); i++) {
            BodyPart bodyPart = multipart.getBodyPart(i);
            String contentType = bodyPart.getContentType().toLowerCase();

            if (bodyPart.isMimeType("text/plain") && !textFound) {
                // Берём только первую найденную plain‑text часть
                textContent.append(bodyPart.getContent().toString()).append("\n");
                textFound = true;
            } else if (bodyPart.isMimeType("text/html") && !textFound) {
                // Если plain‑text не найден, берём HTML и конвертируем
                String htmlContent = bodyPart.getContent().toString();
                textContent.append(htmlToText(htmlContent)).append("\n");
                textFound = true;
            } else if (bodyPart.isMimeType("multipart/*")) {
                // Рекурсивно обрабатываем вложенные Multipart
                Multipart nestedMultipart = (Multipart) bodyPart.getContent();
                textContent.append(extractTextFromMultipart(nestedMultipart));
            }
            // Вложения пропускаем
        }

        return cleanText(textContent.toString());
    }

    public String extractText(Message message) {
        try {
            Tika tika = new Tika();
            try (InputStream emailStream = message.getInputStream()) {
                String extractedText = tika.parseToString(emailStream);
                return cleanText(extractedText);
            }
        } catch (IOException e) {
            return "Ошибка чтения письма: " + e.getMessage();
        } catch (TikaException e) {
            return "Ошибка парсинга Tika: " + e.getMessage();
        } catch (Exception e) {
            return "Неожиданная ошибка: " + e.getMessage();
        }
    }

    /**
     * Конвертирует HTML в простой текст
     */
    private String htmlToText(String html) {
        return html.replaceAll("<[^>]+>", "")  // удаляем теги
                .replaceAll("&nbsp;", " ")  // заменяем HTML‑сущности
                .replaceAll("&amp;", "&")
                .replaceAll("&quot;", "\"")
                .replaceAll("&#39;", "'")
                .replaceAll("\\s+", " ")      // нормализуем пробелы
                .trim();
    }

    /**
     * Дополнительная очистка извлечённого текста
     * @param text исходный текст
     * @return очищенный текст
     */
    private String cleanText(String text) {
        if (text == null || text.trim().isEmpty()) {
            return "Текст не найден";
        }

        // Очищаем HTML, оставляя только текст
        String plainText = Jsoup.clean(text, Safelist.none());

        //Нормализуем переносы и пробелы
        plainText = plainText.replaceAll("\\r\\n|\\r", "\n")
                .replaceAll("\\n\\s*\\n", "\n\n")
                .replaceAll("\\s+", " ")
                .trim();
        System.out.println(plainText);
        return plainText;
    }

    /**
     * Извлекает только основную часть письма (без заголовков и служебной информации)
     * @param message email-сообщение
     * @return основной текст письма
     */
    public String extractMainBody(Message message) {
        String fullText = extractText(message);

        // Удаляем стандартные заголовки email, если они есть в тексте
        String[] parts = fullText.split("(?:From:|To:|Subject:|Date:)", 2);
        if (parts.length > 1) {
            return cleanText(parts[1]);
        }
        return fullText;
    }

}
