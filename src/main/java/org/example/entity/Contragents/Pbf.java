package org.example.entity.Contragents;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.mail.Message;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.example.constants.Const;
import org.example.constants.ConstPbf;
import org.example.entity.InlineKeyboard;
import org.example.entity.Req;
import org.example.entity.ReqStatus;
import org.example.entity.User;
import org.example.services.MessageService;
import org.example.spring.service.ReqService;
import org.example.spring.service.UserService;
import org.example.util.UserUploadSession;
import org.springframework.stereotype.Service;

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

@Slf4j
@Service
@RequiredArgsConstructor
public class Pbf implements Contragent {
    private final MessageService messageService;
    private final String name = "PBF";
    private final OkHttpClient client = new OkHttpClient();
    private final UserService userService;
    private final ReqService reqService;
    private final Map<String, Req> cashNewReq = new ConcurrentHashMap<>();


    @Override
    @PostConstruct
    public void initCash() {
        try {
            cashNewReq.putAll(reqService.findByStatusesAndContragent(
                    List.of(ReqStatus.NEW, ReqStatus.CLOSED_BY_ENGINEER), name));
            log.info("[{}] Кеш инициализирован: {} заявок", name, cashNewReq.size());
        } catch (Exception e) {
            log.error("[{}] Не удалось загрузить кеш при старте: {}", name, e.getMessage(), e);
            // работаем с пустым кешем — наполнится при первом успешном searchReqRest
        }
    }

    @Override
    public InlineKeyboard getContragentReqKeyboard() {
        return Const.KEYBOARD_ATTACHMENT_TO_ALL_REQ_PBF;
    }

    @Override
    public Comparator<Req> getSorter() {
        return Comparator.comparing(Req::getCreationTime,
                Comparator.nullsLast(Comparator.naturalOrder()));
    }

    @Override
    public boolean closeReq(Req req, UserUploadSession userUploadSession) {
        String reqNumber = req.getRequestNumber();
        List<Path> files = userUploadSession.getSavedJpgPaths();
        String url = "https://service.unitodi.ru/rest/api/2/issue/" + reqNumber + "/attachments";

        MultipartBody.Builder builder = new MultipartBody.Builder()
                .setType(MultipartBody.FORM);
        if (!files.isEmpty()) {
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
        }
        if (userUploadSession.getText() != null && !userUploadSession.getText().isEmpty() && !userUploadSession.getText().equals("null")) {
            addComment(reqNumber, userUploadSession.getText());
        }

        try {
            reqService.setReqStatus(reqNumber, ReqStatus.CLOSED_BY_ENGINEER);
        } catch (Exception e) {
            log.error("Ошибка установки статуса заявки {}", e.getMessage());
            return false;   // или продолжить, но без обновления кэша
        }
        Req cached = cashNewReq.get(reqNumber);
        if (cached != null) {
            cached.setStatus(ReqStatus.CLOSED_BY_ENGINEER);
        }

        return true;
    }

    @Override
    public void specialContragentAction(String action, UserUploadSession userUploadSession) {

        userUploadSession.setNextMessageToUser("Приложите фото или текст нажмите готово", Const.KEYBOARD_END_PHOTO); // следующее сообщение


    }

    @Override
    public void processNewEmail(Message message, Long uid) {

    }

    @Override
    public List<Req> getAllRequests() {
        return cashNewReq.values().stream().sorted(getSorter()).toList();
    }

    @Override
    public Optional<Req> getRequestByNumber(String requestNumber) {

        return Optional.ofNullable(cashNewReq.get(requestNumber));
    }

    @Override
    public void searchReqRest(HttpClient client, ObjectMapper mapper) {
        List<Long> allUserIds = userService.findAll().stream()
                .map(User::getChatId)
                .toList();
        // Формируем JSON-тело запроса через DTO для надёжности
        String jsonBody = null;
        try {
            jsonBody = createSearchRequestBody(mapper);
        } catch (JsonProcessingException e) {
            log.error("searchReqRest: ошибка запроса", e);
            return;   // scheduler продолжит
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
            //     System.out.println(responseText);
            root = mapper.readTree(responseText);
        } catch (Exception e) {

            log.error("searchReqRest: ошибка запроса", e);
            return;
        }


        int total = root.path("total").asInt();
        if (total == 0) {
            log.warn("[{}] Jira вернул 0 заявок — отмену пропускаем", name);
            return;
        }
// Получаем массив issues
        JsonNode issues = root.path("issues");
        Map<String, Req> allRequestsFromSyte = new LinkedHashMap<>();
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

            req.setRequestNumber(key);
            req.setRequestText(summary + "\n" + description);
            req.setTid(tid);
            req.setRequestAddress(address);
            req.setPhone(phone);
            req.setPriority(priority);
            req.setCreationTime(ldt);
            // req.setActual(false);
            req.setContragent(this.name);
            allRequestsFromSyte.put(req.getRequestNumber(), req);
        });

        // сверяеем кеш и заявки с сайта, если в кеше есть а с сайта нет, помечаем в кеше и в базе как отменена
        Iterator<Map.Entry<String, Req>> it = cashNewReq.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Req> entry = it.next();
            String number = entry.getKey();
            Req cached = entry.getValue();

            if (allRequestsFromSyte.containsKey(number)) continue;   // ещё на сайте

            try {
                if (cached.getStatus() == ReqStatus.NEW || reqService.getReqByNumber(number).getStatus() == ReqStatus.NEW ) {
                    reqService.setReqStatus(number, ReqStatus.CANCELLED);
                    log.info("[{}] {} → CANCELLED (пропала с сайта)", name, number);
                } else {
                    reqService.setReqStatus(number,ReqStatus.CLOSED);
                    log.info("[{}] {} пропала с сайта, статус {} — убираем из кеша",
                            name, number, cached.getStatus());
                }
            } catch (Exception e) {
                log.error("[{}] не удалось обновить {}: {}", name, number, e.getMessage());
            }
            it.remove();   // ← удаляем ВСЕГДА, при любом статусе
        }

        for (Req req : allRequestsFromSyte.values()) {
            String number = req.getRequestNumber();

            if (!cashNewReq.containsKey(number)) {
                // Выполняем дополнительный код для НОВЫХ заявок
                addNewReq(req, allUserIds);
            }
        }


    }

    @Override
    public String getContragentName() {
        return name;
    }


    @Override
    public void addNewReq(Req req, List<Long> allUserIds) {
        req.setStatus(ReqStatus.NEW);
        cashNewReq.put(req.getRequestNumber(), req);
        if (!reqService.addReq(req)) {
            messageService.sendSimpleMessage(Const.ADMIN_CHAT_ID, req.toString() + Const.REQ_ADD_ERROR_MESSAGE, null);
            log.error("addNewReq: ошибка заявки {}", req.getRequestNumber());
        }

        for (Long getUserChatId : allUserIds) {
            messageService.sendSimpleMessage(getUserChatId, req.toString(), Const.KEYBOARD_ATTACHMENT_TO_ALL_REQ_PBF);
        }
        log.info("New reqMaxDTO {}", req.getRequestNumber());

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
        String jql = "project in (\"Альфа POS\", \"АТМ Альянс. Сервис\", INTEL, Левобережный, \"Общий проект\") " +
                "AND issuetype in (\"Запрос на обслуживание\", \"Service Request\", \"False Request\") " +
                "AND status in (10314, 10109, 10802) " +
                "ORDER BY created DESC";
//        String jql = "project = \"Альфа POS\" AND key = APOS-621906";


        SearchRequest searchRequest = new SearchRequest(jql,
                List.of(
                        "summary", "description", "status", "assignee", "created",
                        "priority", "customfield_10510", "customfield_10610",
                        "customfield_10611", "customfield_11702", "customfield_10724"
                ),
                150
        );

        return mapper.writeValueAsString(searchRequest);
    }

    private void addComment(String reqNumber, String comment) {
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

