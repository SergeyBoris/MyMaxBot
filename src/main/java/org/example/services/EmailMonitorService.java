package org.example.services;

import jakarta.mail.*;
import jakarta.mail.search.FlagTerm;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.example.constants.Const;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Safelist;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class EmailMonitorService {

    private Store store;
    private Folder inbox;

    public EmailMonitorService() {
    }

    // ==================== Подключение ====================

    public void connect() throws Exception {
        Properties properties = new Properties();
        properties.put("mail.debug", Const.MAIL_DEBUG);
        properties.put("mail.imap.host", Const.MAIL_IMAP_HOST);
        properties.put("mail.imap.port", Const.MAIL_IMAP_PORT);
        properties.put("mail.imaps.ssl.enable", Const.MAIL_IMAPS_SSL_ENABLE);
        properties.put("mail.imaps.ssl.protocols", Const.MAIL_IMAPS_SSL_PROTOCOLS);
        properties.put("mail.imap.timeout", Const.MAIL_IMAP_TIMEOUT);
        properties.put("mail.imap.connectionTimeout", Const.MAIL_IMAP_CONNECTION_TIMEOUT);
        properties.put("mail.imap.auth", Const.MAIL_IMAP_AUTH);

        Session session = Session.getInstance(properties);
        store = session.getStore("imaps");
        store.connect(Const.MAIL_IMAP_HOST, Const.MAIL_USER_NAME, Const.MAIL_PASSWORD);

        inbox = store.getFolder(Const.MAIL_FOLDER_TO_SCAN);
        inbox.open(Folder.READ_WRITE);
    }

    public Message[] checkNewMessages() throws Exception {
        if (inbox == null || !inbox.isOpen()) {
            connect();
        }
        return inbox.search(new FlagTerm(new Flags(Flags.Flag.SEEN), false));
    }

    public void disconnect() throws Exception {
        if (inbox != null && inbox.isOpen()) inbox.close(true);
        if (store != null && store.isConnected()) store.close();
    }

    public boolean isConnected() {
        return store != null && store.isConnected();
    }

    // ==================== Для LLM (основной путь) ====================

    /**
     * Возвращает текст письма, оптимизированный для LLM:
     * - если есть HTML-таблица → "ячейка | ячейка | ..." по строкам
     * - иначе → text/plain как есть
     */
    public String getTextForLlm(Message message) throws Exception {
        String html = getRawEmailHtml(message);
        if (html != null && !html.isBlank()) {
            String tableText = htmlTableToText(html);
            if (!tableText.isBlank()) {
                return tableText;
            }
        }
        // Fallback: text/plain
        return getRawEmailBody(message);
    }

    /**
     * HTML → текст с сохранением структуры таблицы:
     *   ячейки через " | ", строки через "\n".
     */
    private String htmlTableToText(String html) {
        Document doc = Jsoup.parse(html);
        doc.select("script, style, head, meta, link").remove();

        Element table = doc.select("table").first();
        if (table == null) {
            return doc.body().text().trim();
        }

        StringBuilder sb = new StringBuilder();
        for (Element row : table.select("tr")) {
            Elements cells = row.select("> td, > th");
            if (cells.isEmpty()) continue;

            for (int i = 0; i < cells.size(); i++) {
                if (i > 0) sb.append(" | ");
                // .text() уже нормализует пробелы и раскрывает &lt; и т.п.
                sb.append(cells.get(i).text().trim());
            }
            sb.append("\n");
        }
        return sb.toString().trim();
    }

    // ==================== HTML-версия письма ====================

    /**
     * Возвращает HTML-часть письма (приоритет text/html).
     * Если HTML нет — возвращает null.
     */
    public String getRawEmailHtml(Message message) throws Exception {
        if (message.isMimeType("text/html")) {
            return readPartAsString(message);
        }
        if (message.isMimeType("multipart/*")) {
            return extractHtmlFromMultipart((Multipart) message.getContent());
        }
        return null;
    }

    private String extractHtmlFromMultipart(Multipart multipart) throws Exception {
        // 1. Сначала ищем text/html (приоритет)
        for (int i = 0; i < multipart.getCount(); i++) {
            BodyPart bodyPart = multipart.getBodyPart(i);
            if (bodyPart.isMimeType("text/html")) {
                return readPartAsString(bodyPart);
            }
        }
        // 2. Рекурсивно во вложенных multipart
        for (int i = 0; i < multipart.getCount(); i++) {
            BodyPart bodyPart = multipart.getBodyPart(i);
            if (bodyPart.isMimeType("multipart/*")) {
                String html = extractHtmlFromMultipart((Multipart) bodyPart.getContent());
                if (html != null) return html;
            }
        }
        return null;
    }

    // ==================== text/plain версия письма ====================

    /**
     * Возвращает text/plain-часть письма без очистки.
     * Fallback, если HTML нет.
     */
    public String getRawEmailBody(Message message) throws Exception {
        if (message.isMimeType("text/plain")) {
            return readPartAsString(message);
        }
        if (message.isMimeType("text/html")) {
            return htmlToText(readPartAsString(message));
        }
        if (message.isMimeType("multipart/*")) {
            String plain = extractPlainFromMultipart((Multipart) message.getContent());
            if (plain != null) return plain;
        }
        return "";
    }

    private String extractPlainFromMultipart(Multipart multipart) throws Exception {
        for (int i = 0; i < multipart.getCount(); i++) {
            BodyPart bodyPart = multipart.getBodyPart(i);
            if (bodyPart.isMimeType("text/plain")) {
                return readPartAsString(bodyPart);
            }
        }
        for (int i = 0; i < multipart.getCount(); i++) {
            BodyPart bodyPart = multipart.getBodyPart(i);
            if (bodyPart.isMimeType("multipart/*")) {
                String plain = extractPlainFromMultipart((Multipart) bodyPart.getContent());
                if (plain != null) return plain;
            }
        }
        return null;
    }

    // ==================== Чтение Part с учётом charset ====================

    /**
     * Читает байты Part и декодирует с учётом charset из Content-Type.
     * Без нормализации — сохраняет \t, \n, пробелы.
     */
    private String readPartAsString(Part part) throws Exception {
        String charset = "UTF-8";
        try {
            String ct = part.getContentType();
            String cs = new jakarta.mail.internet.ContentType(ct).getParameter("charset");
            if (cs != null && !cs.isBlank()) charset = cs;
        } catch (Exception ignored) {
        }

        try (InputStream is = part.getInputStream()) {
            byte[] bytes = is.readAllBytes();

            // Если charset не указан — эвристика UTF-8 → CP1251
            if ("UTF-8".equalsIgnoreCase(charset)) {
                try {
                    java.nio.charset.StandardCharsets.UTF_8.newDecoder()
                            .onMalformedInput(java.nio.charset.CodingErrorAction.REPORT)
                            .decode(java.nio.ByteBuffer.wrap(bytes));
                } catch (java.nio.charset.CharacterCodingException e) {
                    charset = "windows-1251";
                }
            }
            return new String(bytes, java.nio.charset.Charset.forName(charset));
        }
    }

    // ==================== Старые методы (для отображения) ====================

    public String getEmailBody(Message message) throws Exception {
        Object content = message.getContent();
        if (content instanceof String) {
            return cleanText((String) content);
        } else if (content instanceof Multipart) {
            return extractTextFromMultipart((Multipart) content);
        }
        return "Не удалось извлечь тело письма";
    }

    private String extractTextFromMultipart(Multipart multipart) throws Exception {
        StringBuilder textContent = new StringBuilder();
        boolean textFound = false;

        for (int i = 0; i < multipart.getCount(); i++) {
            BodyPart bodyPart = multipart.getBodyPart(i);
            if (bodyPart.isMimeType("text/plain") && !textFound) {
                textContent.append(readPartAsString(bodyPart)).append("\n");
                textFound = true;
            } else if (bodyPart.isMimeType("text/html") && !textFound) {
                textContent.append(htmlToText(readPartAsString(bodyPart))).append("\n");
                textFound = true;
            } else if (bodyPart.isMimeType("multipart/*")) {
                Multipart nested = (Multipart) bodyPart.getContent();
                textContent.append(extractTextFromMultipart(nested));
            }
        }
        return cleanText(textContent.toString());
    }

    public String extractText(Message message) {
        try {
            Tika tika = new Tika();
            try (InputStream emailStream = message.getInputStream()) {
                return cleanText(tika.parseToString(emailStream));
            }
        } catch (IOException e) {
            return "Ошибка чтения письма: " + e.getMessage();
        } catch (TikaException e) {
            return "Ошибка парсинга Tika: " + e.getMessage();
        } catch (Exception e) {
            return "Неожиданная ошибка: " + e.getMessage();
        }
    }

    private String htmlToText(String html) {
        return html.replaceAll("<[^>]+>", "")
                .replaceAll("&nbsp;", " ")
                .replaceAll("&amp;", "&")
                .replaceAll("&quot;", "\"")
                .replaceAll("&#39;", "'")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String cleanText(String text) {
        if (text == null || text.trim().isEmpty()) {
            return "Текст не найден";
        }
        String plainText = Jsoup.clean(text, Safelist.none());
        plainText = plainText.replaceAll("\\r\\n|\\r", "\n")
                .replaceAll("\\n\\s*\\n", "\n\n")
                .replaceAll("\\s+", " ")
                .trim();
        return plainText;
    }
}