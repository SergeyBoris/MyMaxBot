package org.example.entity.Contragents;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.mail.Message;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.constants.Const;
import org.example.constants.ConstPfi;
import org.example.entity.InlineKeyboard;
import org.example.entity.Req;
import org.example.entity.User;
import org.example.services.MessageService;
import org.example.spring.service.ReqService;
import org.example.spring.service.UserService;
import org.example.util.AllReqCash;
import org.example.util.HendzCloseRequest;
import org.example.util.UserUploadSession;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class Pfi implements Contragent {

    public static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/146.0.0.0 YaBrowser/26.4.0.0 Safari/537.36";
    public static final String CONTENT_TYPE = "application/x-www-form-urlencoded; charset=UTF-8";

    private final MessageService messageService;
    private final HttpClient client;
    private final ObjectMapper mapper;
    private final UserService userService;
    private final ReqService reqService;
    private String cookie;
    private final AllReqCash cash;
    private Map<String,Req> notInDbReqs = new ConcurrentHashMap<>();
    private final String name = "PFI";


    @Override
    public InlineKeyboard getContragentReqKeyboard() {
        return Const.KEYBOARD_ATTACHMENT_TO_ALL_REQ_PFI;
    }

    @Override
    public List<Req> getAllRequests() {
        return reqService.getNewWithPending(name, notInDbReqs.values(),getSorter());
    }

    @Override
    public Optional<Req> getRequestByNumber(String requestNumber) {
        Optional<Req> reqByNumber = Optional.ofNullable(reqService.getReqByNumber(requestNumber));
        if (reqByNumber.isEmpty() && notInDbReqs.containsKey(requestNumber)) {
            reqByNumber = Optional.ofNullable(notInDbReqs.get(requestNumber));
        }
        return reqByNumber;
    }

    @Override
    public boolean closeReq(Req req, UserUploadSession userUploadSession) {


        return HendzCloseRequest.close(req, userUploadSession, cookie);
    }

    @Override
    public String getContragentName() {
        return name;
    }

    @Override
    public void searchReqRest(HttpClient client, ObjectMapper mapper) {
        // 1. Если cookie нет — логинимся
        if (cookie == null || cookie.isBlank()) {
            log.info("Pfi: cookie отсутствует — логинимся");
            cookie = login();
            if (cookie == null || cookie.isBlank()) {
                log.error("Pfi: не удалось залогиниться — пропускаем опрос");
                return;
            }
            log.info("Pfi: логин успешен, cookie получен");
        }
        List<Long> allUserIds = userService.findAll().stream()
                .map(User::getChatId)
                .toList();
        // 2. Основной запрос
        List<Req> allRequestsFromSyte = new ArrayList<>();
        System.out.println("ПФИ ИЩЕТ НАЧАЛО");

        try {

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://work.hendz.ru:10294/pfi"))
                    .header("Cookie", cookie)
                    .header("X-October-Request-Handler", "ticketsListServer::onLoadMoreTickets")
                    .header("user-agent", ConstPfi.USER_AGENT)
                    .header("Content-Type", CONTENT_TYPE)
                    .header("X-Requested-With", "XMLHttpRequest")
                    .header("Referer", "https://work.hendz.ru:10294/pfi")
                    .timeout(Duration.ofSeconds(60))
                    .POST(HttpRequest.BodyPublishers.ofString(ConstPfi.SHOW_REQ_PAYLOAD))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            log.info("Pfi: статус {}", response.statusCode());

            // 3. Если 401/403/302 — cookie протух, перелогиниться
            if (response.statusCode() == 401 || response.statusCode() == 403
                    || response.statusCode() == 302) {
                log.warn("Pfi: cookie протух ({}), перелогиниваемся", response.statusCode());
                cookie = login();
                if (cookie == null || cookie.isBlank()) {
                    log.error("Pfi: перелогин не удался — пропускаем");
                    return;
                }

                // Повторный запрос с новым cookie
                request = HttpRequest.newBuilder()
                        .uri(URI.create("https://work.hendz.ru:10294/pfi"))
                        .header("Cookie", cookie)
                        .header("X-October-Request-Handler", "ticketsListServer::onLoadMoreTickets")
                        .header("user-agent", ConstPfi.USER_AGENT)
                        .header("Content-Type", CONTENT_TYPE)
                        .header("X-Requested-With", "XMLHttpRequest")
                        .header("Referer", "https://work.hendz.ru:10294/pfi")
                        .timeout(Duration.ofSeconds(60))
                        .POST(HttpRequest.BodyPublishers.ofString(ConstPfi.SHOW_REQ_PAYLOAD))
                        .build();
                response = client.send(request, HttpResponse.BodyHandlers.ofString());
                log.info("Pfi: повторный статус {}", response.statusCode());
            }

            if (response.statusCode() != 200) {
                log.error("Pfi: неожиданный статус {} — пропускаем", response.statusCode());
                return;
            }

            allRequestsFromSyte = parseReq(response.body());

        } catch (Exception e) {
            log.error("Pfi: ошибка запроса: {}", e.getMessage(), e);
            return;   // ← не пробрасывать RuntimeException — иначе FindReqProcessor ловит, но лучше skip
        }

        for (Req req : allRequestsFromSyte) {
            String number = req.getRequestNumber();

            if (!cash.contains(number)) {
                // Выполняем дополнительный код для НОВЫХ заявок
                addNewReq(req, allUserIds);
            }
        }
        System.out.println("ПФИ ИЩЕТ КОНЕЦ");
    }

    @Override
    public void addNewReq(Req req, List<Long> allUserIds) {
        for (Long userChatId : allUserIds) {
            messageService.sendSimpleMessage(userChatId, req.toString(), Const.KEYBOARD_ATTACHMENT_TO_ALL_REQ_PFI);
        }
        if(!reqService.addReq(req)){
            String error = Const.REQ_ADD_ERROR_MESSAGE;
            log.error("addNewReq: ошибка заявки {}", req.getRequestNumber() );
            req.setRequestText(req.getRequestText() + error);
            notInDbReqs.put(req.getRequestNumber(),req);
        }

        for (Long getUserChatId : allUserIds) {
            messageService.sendSimpleMessage(getUserChatId, req.toString(), Const.KEYBOARD_ATTACHMENT_TO_ALL_REQ_PBF);
        }
        log.info("New reqMaxDTO {}", req.getRequestNumber());

    }

    @Override
    public void processNewEmail(Message message) {

    }

    private List<Req> parseReq(String responseBody) {
        List<Req> reqList = new ArrayList<>();
        try {
            JsonNode fullJson = mapper.readTree(responseBody);
            JsonNode requestsJson = fullJson.get("ticketsGeo");

            if (requestsJson == null || !requestsJson.isArray() || requestsJson.isEmpty()) {
                log.warn("Pfi: пустой ticketsGeo в ответе");
                return reqList;
            }

            for (JsonNode reqJson : requestsJson) {
                String requestNumber = reqJson.path("number").asText(null);
                try {
                    if (requestNumber == null) {
                        log.warn("Pfi: заявка без number — пропускаем");
                        continue;
                    }

                    String ticketId = reqJson.path("id").asText();
                    String payload = "show_overdue=0&ticket_id=" + ticketId;

                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create("https://work.hendz.ru:10294/pfi?show_overdue=0"))
                            .header("cookie", cookie)
                            .header("user_agent", ConstPfi.USER_AGENT)
                            .header("Content-Type", CONTENT_TYPE)
                            .header("X-October-Request-Handler", "ticketsListServer::onLoadTicket")
                            .header("X-Requested-With", "XMLHttpRequest")
                            .header("Referer", "https://work.hendz.ru:10294/pfi?show_overdue=0")
                            .timeout(Duration.ofSeconds(30))          // ← timeout
                            .POST(HttpRequest.BodyPublishers.ofString(payload))
                            .build();

                    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                    JsonNode json = mapper.readTree(response.body());

                    if (!json.has("result")) {
                        log.warn("Pfi: заявка {} — нет result в ответе", requestNumber);
                        continue;
                    }

                    JsonNode result = json.get("result");
                    if (!result.has("ticketHtml")) {
                        log.warn("Pfi: заявка {} — нет ticketHtml", requestNumber);
                        continue;
                    }

                    Req req = parseTicketModal(result.get("ticketHtml").asText(), new Req());
                    if (req == null) {                                          // ← null check
                        log.warn("Pfi: заявка {} — пустая таблица, пропускаем", requestNumber);
                        continue;
                    }

                    req.setRequestNumber(requestNumber);
                    req.setContragent(this.name);
                    req.setParams("outgoing", result.path("outgoing").asText(""));
                    req.setParams("ticket_id", ticketId);
                    reqList.add(req);
                    log.debug("Pfi: заявка {} распарсена", requestNumber);

                } catch (Exception e) {
                    log.warn("Pfi: ошибка заявки {}: {}", requestNumber, e.getMessage());
                    // продолжаем — остальные заявки не теряются
                }
            }
        } catch (Exception e) {
            log.error("Pfi: ошибка парсинга ответа: {}", e.getMessage(), e);
            // не пробрасываем — возвращаем то, что успели распарсить
        }
        return reqList;
    }

    @Override
    public void specialContragentAction(String action, UserUploadSession userUploadSession) {
        switch (action) {
            case "Локализовано" -> {
                userUploadSession.setNextMessageToUser("Напишите ФИО сотрудника ТСТ", Const.KEYBOARD_CANCEL); // следующее сообщение
                userUploadSession.setParams("task_uid", "83");
                userUploadSession.setParams("status_uid", "639");
                userUploadSession.setParams("close_mode", "no_onsite");
            }
            case "Закрыто" -> {
                userUploadSession.setParams("task_uid", "83");
                userUploadSession.setParams("status_uid", "518");
                userUploadSession.setParams("close_mode", "onsite");
                userUploadSession.setNextMessageToUser("Введите текст заявки закрытие с выездом: ", Const.KEYBOARD_CANCEL);
            }
            case "Напишите ФИО сотрудника ТСТ" -> {
                userUploadSession.setParams("params[791]", userUploadSession.getText()); // имя отправителя заявки
                userUploadSession.setNextMessageToUser("Введите текст заявки для закрытия без выезда: ", Const.KEYBOARD_CANCEL); // следующее сообщение
            }
            case "Введите текст заявки для закрытия без выезда: " -> {
                userUploadSession.setParams("params[792]", userUploadSession.getText()); // имя отправителя заявки
                userUploadSession.setNextMessageToUser("Приложите фото и нажмите готово ", Const.KEYBOARD_END_PHOTO); // следующее сообщение
            }
            case "Введите текст заявки закрытие с выездом: " -> {
                userUploadSession.setParams("params[731]", userUploadSession.getText()); // имя отправителя заявки
                userUploadSession.setNextMessageToUser("Приложите фото и нажмите готово ", Const.KEYBOARD_END_PHOTO); // следующее сообщение
            }
            case "p_n_r_worked" -> {
                userUploadSession.setParams("task_uid", "88");
                userUploadSession.setParams("status_uid", "531");       // статус - ПНР
                userUploadSession.setParams("params[920]", "ПНР успех"); // описание работ
                userUploadSession.setNextMessageToUser("Введите серийный номер терминала текстом:", Const.KEYBOARD_CANCEL);
            }
            case "Введите серийный номер терминала текстом:" -> {
                userUploadSession.setParams("close_mode", "onsite");
                String serialTerminal = userUploadSession.getText();
                log.info("entered serialTerminal {}", serialTerminal);
                userUploadSession.setParams("params[592]", serialTerminal);
                userUploadSession.setText("");
                userUploadSession.setNextMessageToUser("Введите контактные данные представителя ТСТ:", Const.KEYBOARD_CANCEL);

            }
            case "p_n_r_not_allowed" -> {
                userUploadSession.setParams("status_uid", "530"); // статус "не допущен"
                userUploadSession.setNextMessageToUser("Введите описание работ для статуса допуск не обеспечен:", Const.KEYBOARD_CANCEL);
            }
            case "Введите описание работ для статуса допуск не обеспечен:" -> {
                String comment = userUploadSession.getText();
                userUploadSession.setParams("params[590]", comment);
                userUploadSession.setParams("close_mode", "onsite");
                userUploadSession.setNextMessageToUser("Введите контактные данные представителя ТСТ:", Const.KEYBOARD_CANCEL);
            }
            case "Введите контактные данные представителя ТСТ:" -> {
                String phoneContactOnTst = userUploadSession.getText();
                log.info("entered phoneContactOnTst {}", phoneContactOnTst);
                userUploadSession.setParams("params[894]", phoneContactOnTst); // имя отправителя заявки
                userUploadSession.setNextMessageToUser("Приложите фото и нажмите готово ", Const.KEYBOARD_END_PHOTO);
            }


        }

    }

    public Req parseTicketModal(String html, Req req) {
        Document doc = Jsoup.parse(html);
        // Берём таблицу
        Elements upperTableRows = doc.select("dl").first().getElementsByClass("row small mb-3");


        req.setSla(upperTableRows.select("dd").get(1).text()); // установка sla


        Element table = doc.select("table.table").first();
        if (table == null) {
            System.out.println("Таблица пуста для заявки " + req.getRequestNumber());
            return null;
        }

        // Проходим по всем строкам
        Elements rows = table.select("tr");

        for (Element row : rows) {
            String key = row.select("th").text().toLowerCase();
            String value = row.select("td").text();

            switch (key) {
                case "tid":
                    req.setTid(value);
                    req.setParams("params[593]", value);
                    System.out.println("TID: " + value);
                    break;
                case "рекомендации":
                    System.out.println("Рекомендации: " + value);
                    req.setRequestText("Рекомендации: " + value + "\n");
                    break;
                case "модель":
                    System.out.println("Модель: " + value);
                    break;
                case "серийный номер":
                    System.out.println("SN: " + value);
                    req.setParams("params[591]", value);
                    break;
                case "владелец":
                    System.out.println("владелец: " + value);
                    req.setParams("владелец", value);
                    break;
                case "клиент":
                    System.out.println("клиент: " + value);
                    req.setParams("клиент", value);
                    break;
                case "адрес":
                    System.out.println("Адрес: " + value);
                    req.setRequestAddress(value);
                    break;
                case "текст заявки":
                    System.out.println("Текст заявки: " + value);
                    if (req.getRequestText() == null) {
                        req.setRequestText("Рекомендации: ОТСУТСВУЮТ\nТекст заявки: " + value + "\n");
                    } else {
                        req.setRequestText(req.getRequestText() + "Текст заявки: " + value);
                    }
                    break;
                case "замечания к заявке":
                    req.setRequestText(req.getRequestText() + "\nЗамечания к заявке: " + value);
                    break;
            }
        }
        return req;
    }

    private String login() {
        String userAuthCookie = "";
        String sessionCookie = "";
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://work.hendz.ru:10294/account/login"))
                    .header("accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7")
                    .header("user-agent", ConstPfi.USER_AGENT)
                    .timeout(Duration.ofSeconds(30))
                    .build();


            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println(response.body());
            String sessionKey = extractValue(response.body(), "_session_key");
            String token = extractValue(response.body(), "_token");
            HttpHeaders headers = response.headers();
            List<String> octoberSession = headers.map().get("set-cookie");
            System.out.println(sessionKey);
            System.out.println(token);
            String formData = "_session_key=" + URLEncoder.encode(sessionKey, StandardCharsets.UTF_8) +
                    "&_token=" + URLEncoder.encode(token, StandardCharsets.UTF_8) +
                    "&login=" + URLEncoder.encode(ConstPfi.PFI_LOGIN, StandardCharsets.UTF_8) +
                    "&password=" + URLEncoder.encode(ConstPfi.PFI_PASSWORD, StandardCharsets.UTF_8) +
                    "&remember=1";

            request = HttpRequest.newBuilder()
                    .header("Content-Type", CONTENT_TYPE)
                    .header("X-October-Request-Handler", "onSignin")
                    .header("X-Requested-With", "XMLHttpRequest")
                    .header("User-Agent", ConstPfi.USER_AGENT)
                    .uri(URI.create("https://work.hendz.ru:10294/account/login"))
                    .POST(HttpRequest.BodyPublishers.ofString(formData))
                    .build();

            response = client.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println("response.statusCode() = " + response.statusCode());
            List<String> cookies = response.headers().allValues("set-cookie");

            for (String cookie : cookies) {
                if (cookie.startsWith("user_auth")) {
                    userAuthCookie = cookie;
                } else {
                    sessionCookie = cookie;
                }
            }

            return userAuthCookie + ";" + sessionCookie;

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static String extractValue(String html, String fieldName) {
        String regex = "name=\"" + fieldName + "\".*?value=\"([^\"]+)\"";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(html);
        if (matcher.find()) {
            return matcher.group(1);
        }
        throw new RuntimeException("Не найден " + fieldName);
    }

    @Scheduled(fixedDelayString = "${bot.merge-retry-ms:60000}")
    public void mergeReqWithDb() {
        if (notInDbReqs.isEmpty()) return;

        Iterator<Map.Entry<String, Req>> it = notInDbReqs.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Req> entry = it.next();
            String number = entry.getKey();
            Req req = entry.getValue();

            try {
                if (reqService.addReq(req)) {
                    it.remove();   // ← успех — убрать из Map
                    log.info("Досохранена {}", number);
                }
            } catch (Exception e) {
                log.error("Retry {}: {}", number, e.getMessage());
            }
        }
    }

}
