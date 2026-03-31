package org.example;

import jakarta.mail.BodyPart;
import jakarta.mail.Flags;
import jakarta.mail.Message;
import jakarta.mail.Multipart;
import org.example.services.EmailMonitorService;

public class EmailMonitoringApp {
    public static void main(String[] args) {
        EmailMonitorService monitor = new EmailMonitorService();

        try {
            monitor.connect();

            while (true) {
                Message[] newMessages = monitor.checkNewMessages();
                for (Message message : newMessages) {
                    processNewEmail(message);
                    // Помечаем как прочитанное
                    message.setFlag(Flags.Flag.SEEN, true);
                }
                Thread.sleep(30000); // Проверка каждые 30 секунд
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                monitor.disconnect();
            } catch (Exception ignored) {}
        }
    }

    private static void processNewEmail(Message message) throws Exception {
        System.out.println("Новое письмо:");
        System.out.println("От: " + message.getFrom()[0]);
        System.out.println("Тема: " + message.getSubject());
        System.out.println("Дата: " + message.getSentDate());

        System.out.println(getEmailBody(message));
        // Здесь можно добавить логику обработки письма
    }
    public static String getEmailBody(Message message) throws Exception {
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
    private static String getTextFromMultipart(Multipart multipart) throws Exception {
        StringBuilder textBuilder = new StringBuilder();

        for (int i = 0; i < multipart.getCount(); i++) {
            BodyPart bodyPart = multipart.getBodyPart(i);
            String contentType = bodyPart.getContentType();

            if (bodyPart.isMimeType("text/plain")) {
                // Текстовая часть
                textBuilder.append(bodyPart.getContent().toString());
            } else if (bodyPart.isMimeType("text/html")) {
                // HTML‑часть — преобразуем в текст
                String htmlContent = bodyPart.getContent().toString();
                textBuilder.append(htmlToText(htmlContent));
            } else if (bodyPart.getContent() instanceof Multipart) {
                // Вложенная Multipart‑структура
                textBuilder.append(getTextFromMultipart((Multipart) bodyPart.getContent()));
            }
            // Вложения (attachments) пропускаем — они не входят в тело письма
        }

        return textBuilder.toString();
    }
    private static String htmlToText(String html) {
        return html
                .replaceAll("<[^>]*>", "")  // Удаляем все теги
                .replaceAll("\\s+", " ")     // Заменяем множественные пробелы на один
                .trim();
    }
}


