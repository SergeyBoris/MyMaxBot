package org.example.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import org.example.constants.Const;
import org.example.entity.InlineKeyboard;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@AllArgsConstructor
public class MessageService {
    private HttpClient client;

    public void sendSimpleMessage(long chatId, String messageText, InlineKeyboard keyBoard) {
        String kbd="";
        // Замена буквальных последовательностей \\r\\n на <br>
        messageText = messageText.replaceAll("\\\\\\\\r\\\\\\\\n", "\\\\n");
        messageText = messageText.replaceAll("\\n", "\\\\n");
        messageText = messageText.replaceAll("\\\\\\\\t", " ");

        // Очистка от множественных <br> (2+ подряд → один <br>)
        messageText = messageText.replaceAll("(\\\\n){2,}", "\\\\n");
        System.out.println("После замены: " + messageText);
//        System.out.println("messgeytext : "+messageText);
        messageText = messageText
//             .replace("\\", "\\\\")
          .replace("\"", "\\\"")
         .replace("\n", "\\\\n");
        if(messageText.length()>4000){
            messageText = messageText.substring(0, 3999);
        }
        ObjectMapper mapper = new ObjectMapper();

        try {
            kbd = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(keyBoard);
        } catch (Exception e) {
            e.printStackTrace();
        }

        String json = """
                {
                  "text": "%s",
                  "format": "html",
                  "attachments": [
                    %s
                  ]
                }
                """.formatted(messageText,kbd);
        if(keyBoard==null){
            json = """
                {
                  "text": "%s",
                  "format": "html"
                }
                """.formatted(messageText);
        }
        System.out.println(json);
        HttpRequest request = getHttpRequest("https://platform-api.max.ru/messages?chat_id=", chatId, json);
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println("Status: " + response.statusCode());
            System.out.println("Response: " + response.body());
        }catch (Exception e) {
            throw new RuntimeException(e);
        }

    }
    public void deleteMessage(String messageId) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://platform-api.max.ru/messages?message_id=" + messageId))
                .header("Authorization", Const.BOT_TOKEN)
                .header("Content-Type", "application/json")
                .DELETE()
                .build();
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println("Status: " + response.statusCode());
            System.out.println("Response: " + response.body());
        }catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    private static HttpRequest getHttpRequest(String url, long chatId, String json) {
        return HttpRequest.newBuilder()
                .uri(URI.create(url + chatId))
                .header("Authorization", Const.BOT_TOKEN)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
    }
}


