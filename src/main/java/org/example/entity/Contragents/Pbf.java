package org.example.entity.Contragents;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.mail.Message;
import okhttp3.*;
import org.example.constants.Const;
import org.example.constants.ConstPbf;
import org.example.db.MapDB;
import org.example.entity.Req;
import org.example.services.MessageService;
import org.example.util.UserUploadSession;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;


public class Pbf implements Contragent {
    private final Map<String, Req> cashedRequests;
    private final MessageService messageService;
    private final MapDB db;
    private final String name;
    private final OkHttpClient client = new OkHttpClient();

    public Pbf(MessageService messageService, MapDB db,String name) {
        this.db = db;
        this.messageService = messageService;
        cashedRequests = new ConcurrentHashMap<>();
        this.name = name;
    }

    @Override
    public boolean closeReq(Req req, UserUploadSession userUploadSession) {
        String reqNumber = req.getRequestNumber();
        List<Path> files = userUploadSession.getSavedJpgPaths();
        String url = "https://service.unitodi.ru/rest/api/2/issue/" + reqNumber + "/attachments";

        MultipartBody.Builder builder = new MultipartBody.Builder()
                .setType(MultipartBody.FORM);

        for (Path filePath : files) {
            File file = filePath.toFile();
            RequestBody fileBody = RequestBody.create(
                    file,
                    okhttp3.MediaType.parse("application/octet-stream")
            );
            builder.addFormDataPart("file", file.getName(), fileBody);
        }

        Request request = new Request.Builder()
                .url(url)
                .post(builder.build())
                .addHeader("Authorization", ConstPbf.AUTHORISATION)
                .addHeader("X-Atlassian-Token", "no-check")
                .build();

        try (Response response = client.newCall(request).execute()) {
            System.out.println("Status: " + response.code());
            System.out.println("Response: " + response.body().string());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        if(userUploadSession.getText() !=null && !userUploadSession.getText().isEmpty()){
            addComment(reqNumber, userUploadSession.getText());
        }
        return true;
    }

    @Override
    public void processNewEmail(Message message) {

    }

    @Override
    public List<Req> getAllRequests() {


        return cashedRequests.values().stream().toList();
    }

    @Override
    public Optional<Req> getRequestByNumber(String requestNumber) {
        return Optional.ofNullable(cashedRequests.get(requestNumber));
    }

    @Override
    public void searchReqRest(HttpClient client, ObjectMapper mapper) {
        // Формируем JSON-тело запроса через DTO для надёжности
        String jsonBody = null;
        try {
            jsonBody = createSearchRequestBody(mapper);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        // Создаём HTTP-запрос
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(ConstPbf.URL))
                .header("Content-Type", "application/json")
                .header("Authorization", ConstPbf.AUTHORISATION)
                .header("Cookie", ConstPbf.COOKIE)
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        // Отправляем запрос и получаем ответ
        HttpResponse<String> response;
        JsonNode root;
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofString());


            String responseText = response.body();

            //полный ответ
            System.out.println(responseText);
            root = mapper.readTree(responseText);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }


        System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
        int total = root.path("total").asInt();
        System.out.println("Всего задач: " + total);
// Получаем массив issues
        JsonNode issues = root.path("issues");
        List<Req> allRequests = new ArrayList<>();
        // Проходим по всем задачам (если есть)
        issues.forEach(issue -> {
            // Извлекаем нужные поля
            String key = issue.path("key").asText();
            String summary = issue.path("fields").path("summary").asText();
            String tid = issue.path("fields").path("customfield_10610").asText();
            String address = issue.path("fields").path("customfield_10611").asText();
            String description = issue.path("fields").path("description").asText();
            String phone = issue.path("fields").path("customfield_10724").asText();

            String priority = issue.path("fields")
                    .path("priority")
                    .path("name")
                    .asText();
            String statusName = issue.path("fields")
                    .path("status")
                    .path("name")
                    .asText();
            String created = issue.path("fields").path("created").asText();
            String fixed = created.replaceFirst("(\\+\\d{2})(\\d{2})$", "$1:$2");
            if (phone.matches("^7.*")) {
                phone = "+" + phone;
            } else if (!phone.matches("^7.*") && !phone.matches("^8.*")) {
                phone = "+7" + phone;
            }

            OffsetDateTime odt = OffsetDateTime.parse(fixed);

            DateTimeFormatter formattedDate = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");
            LocalDateTime ldt = odt.toLocalDateTime();
            Req req = new Req();

            // Выводим данные
            System.out.println("Номер заявки: " + key);
            System.out.println("Краткое описание: " + summary);
            System.out.println("Описание: " + description);
            System.out.println("TID: " + tid);
            System.out.println("address: " + address);
            System.out.println("тел: " + phone);
            System.out.println("Приоритет: " + priority);
            System.out.println("Статус: " + statusName);
            System.out.println("Создано: " + odt.format(formattedDate));
            System.out.println("---");
            req.setRequestNumber(key);
            req.setRequestText(summary + "\n" + description);
            req.setTID(tid);
            req.setRequestAddress(address);
            req.setPhone(phone);
            req.setPriority(priority);
            req.setCreationTime(ldt);
            req.setToUsersId(db.getAllUsers().stream().toList());
            req.setActual(false);
            req.setContragent(this);
            allRequests.add(req);
        });
        Set<String> newNumbers = allRequests.stream()
                .map(Req::getRequestNumber)
                .collect(Collectors.toSet());
        cashedRequests.keySet().retainAll(newNumbers);

        for (Req req : allRequests) {
            String number = req.getRequestNumber();

            if (!cashedRequests.containsKey(number)) {
                // Выполняем дополнительный код для НОВЫХ заявок
                addReqToCash(req);
            }
        }
    }

    @Override
    public String getContragentName() {
        return name;
    }


    @Override
    public void addReqToCash(Req req) {
        for (Long user : db.getAllUsers()) {
            messageService.sendSimpleMessage(db.getUserChatId(user), req.toString(), Const.KEYBOARD_ATTACHMENT_TO_ALL_REQ);
        }
        cashedRequests.put(req.getRequestNumber(), req);

    }

    private static String createSearchRequestBody(ObjectMapper mapper) throws JsonProcessingException {
        // DTO для тела запроса
        class SearchRequest {
            public final String jql;
            public final List<String> fields;
            public final int maxResults;

            public SearchRequest(String jql, List<String> fields, int maxResults) {
                this.jql = jql;
                this.fields = fields;
                this.maxResults = maxResults;
            }
        }

        SearchRequest searchRequest = new SearchRequest(
                "status != \"Ожидание ответа от Клиента\" AND statusCategory != Done ORDER BY created DESC",
                List.of(
                        "summary", "description", "status", "assignee", "created",
                        "priority", "customfield_10510", "customfield_10610",
                        "customfield_10611", "customfield_11702", "customfield_10724"
                ),
                150
        );

        return mapper.writeValueAsString(searchRequest);
    }

    private void addComment(String reqNumber, String comment){
        String url = "https://service.unitodi.ru/rest/api/2/issue/" + reqNumber + "/comment";

        // Формируем JSON с комментарием
        String jsonBody = String.format(
                "{\"body\": \"%s\"}",
                comment.replace("\"", "\\\"").replace("\n", "\\n")
        );

        Request request = new Request.Builder()
                .url(url)
                .post(RequestBody.create(jsonBody, MediaType.parse("application/json")))
                .addHeader("Authorization", ConstPbf.AUTHORISATION)
                .addHeader("Content-Type", "application/json")
                .build();

        try (Response response = client.newCall(request).execute()) {
            System.out.println("Status: " + response.code());
            System.out.println("Response: " + response.body().string());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

