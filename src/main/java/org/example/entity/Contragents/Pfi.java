package org.example.entity.Contragents;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.mail.Message;
import org.example.constants.Const;
import org.example.db.MapDB;
import org.example.entity.Req;
import org.example.services.MessageService;
import org.example.util.HendzCloseRequest;
import org.example.util.UserUploadSession;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Pfi implements Contragent {
    public static final String PAYLOAD = "min_hours=" +
            "&max_hours=" +
            "&number=" +
            "&equipment_serial=" +
            "&tid=" +
            "&tl_from=" +
            "&tl_to=" +
            "&show_overdue=false" +
            "&only_overdue=false" +
            "&require_sv=false" +
            "&require_pnr=false" +
            "&require_other=false" +
            "&require_reject=false" +
            "&include_region_none=true" +
            "&include_executor_none=true" +
            "&executor_only_none=false" +
            "&page=1";
    public static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/146.0.0.0 YaBrowser/26.4.0.0 Safari/537.36";
    public static final String CONTENT_TYPE = "application/x-www-form-urlencoded; charset=UTF-8";

    private final MessageService messageService;
    private final MapDB db;
    private final Map<String, Req> cashedRequests;
    private  HttpClient client = HttpClient.newHttpClient();
    private String cookie;
    private final String name;

    public Pfi(MessageService messageService, MapDB db,String name){
        this.messageService = messageService;
        this.db = db;
        this.cookie = login();
        this.name = name;
        cashedRequests = new ConcurrentHashMap<>();

    }
    @Override
    public List<Req> getAllRequests() {
        if(cashedRequests == null || cashedRequests.isEmpty()){
            searchReqRest(HttpClient.newHttpClient(), new ObjectMapper() ); //todo
        }
        return cashedRequests.values().stream().toList();
    }

    @Override
    public Optional<Req> getRequestByNumber(String requestNumber) {
        return Optional.ofNullable(cashedRequests.get(requestNumber));
    }

    @Override
    public boolean closeReq(Req req, UserUploadSession userUploadSession) {

        HendzCloseRequest.close(req, userUploadSession,cookie);
        return true;
    }

    @Override
    public String getContragentName() {
        return name;
    }

    @Override
    public void searchReqRest(HttpClient client, ObjectMapper mapper) {
        List<Req> allRequests = new ArrayList<>();
        System.out.println("ПФИ ИЩЕТ НАЧАЛО");
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://work.hendz.ru:10294/pfi"))
                .header("Cookie", cookie)
                .header("X-October-Request-Handler", "ticketsListServer::onLoadMoreTickets")
                .header("user-agent", USER_AGENT)
                .header("Content-Type", CONTENT_TYPE)
                .header("X-Requested-With", "XMLHttpRequest")
                .header("Referer", "https://work.hendz.ru:10294/pfi")
                .POST(HttpRequest.BodyPublishers.ofString(PAYLOAD))
                .build();

        try {
            HttpResponse<String> response =  client.send(request, HttpResponse.BodyHandlers.ofString());
            // System.out.println(response.body());
            System.out.println("статус код " + response.statusCode());
            if(response.statusCode() != 200){
                login();
            }
            allRequests = parseReq(response.body());


        } catch (Exception e) {
            throw new RuntimeException(e);
        }

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
        System.out.println("ПФИ ИЩЕТ КОНЕЦ");
    }

    @Override
    public void addReqToCash(Req req) {
        for (Long user : db.getAllUsers()) {
            messageService.sendSimpleMessage(db.getUserChatId(user), req.toString(), Const.KEYBOARD_ATTACHMENT_TO_ALL_REQ);
        }
        cashedRequests.put(req.getRequestNumber(), req);

    }

    @Override
    public void processNewEmail(Message message) {

    }

    private List<Req> parseReq(String responseBody) {
        ObjectMapper mapper = new ObjectMapper();
       List<Req> reqList = new ArrayList<>();
        try {
            JsonNode fullJson = mapper.readTree(responseBody);
            JsonNode requestsJson = fullJson.get("ticketsGeo");

            if (requestsJson.isArray() && !requestsJson.isEmpty()) {
                for (JsonNode reqJson : requestsJson) {
                    Req req = new Req();
                    String requestNumber = reqJson.get("number").asText();
                    String ticketId = reqJson.get("id").asText();  // ticketId = "21851"
                    String payload = "show_overdue=0&ticket_id=" + ticketId ;
                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create("https://work.hendz.ru:10294/pfi?show_overdue=0"))
                            .header("cookie", cookie)
                            .header("user_agent", USER_AGENT)
                            .header("Content-Type", CONTENT_TYPE)
                            .header("X-October-Request-Handler", "ticketsListServer::onLoadTicket")
                            .header("X-Requested-With", "XMLHttpRequest")
                            .header("Referer", "https://work.hendz.ru:10294/pfi?show_overdue=0")
                            .POST(HttpRequest.BodyPublishers.ofString(payload))
                            .build();
                    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                    JsonNode json = mapper.readTree(response.body());

                    // Извлекаем HTML из результата
                    if (json.has("result")) {
                        JsonNode result = json.get("result");
                        System.out.println("Весь JSON: " + json.toPrettyString());
                        if (result.has("ticketHtml")) {
                            req = parseTicketModal( result.get("ticketHtml").asText(),req);
                            req.setRequestNumber(requestNumber);
                            req.setContragent(this);
                            req.params.put("outgoing",result.get("outgoing").asText());
                            System.out.println("json.get(utgoingtextValue()"  + result.get("outgoing").asText());
                            req.params.put("ticketId",ticketId);
                            reqList.add(req);
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return reqList;
    }
    public Req parseTicketModal(String html,Req req) {
        Document doc = Jsoup.parse(html);
        // Берём таблицу
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
                    req.setTID(value);
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
                    break;
                case "адрес":
                    System.out.println("Адрес: " + value);
                        req.setRequestAddress(value);
                    break;
                case "контрольный срок":
                        System.out.println("SLA (из таблицы): " + value);
                    req.setSla(value);
                    break;
                case "текст заявки":
                    System.out.println("Текст заявки: " + value);
                    if(req.getRequestText() == null){
                        req.setRequestText("Рекомендации: ОТСУТСВУЮТ\nТекст заявки: " + value + "\n" );
                    }else {
                        req.setRequestText(req.getRequestText() +"Текст заявки: " + value);
                        }
                    break;
            }
        }
        return req;
    }

    private String login() {
        String userAuthCookie = "";
        String sessionCookie="";
        try {
            ObjectMapper mapper = new ObjectMapper();
            client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://work.hendz.ru:10294/account/login"))
                    .header("accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7")
                    .header("user-agent", USER_AGENT)

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
                    "&login=" + URLEncoder.encode(Const.PFI_LOGIN, StandardCharsets.UTF_8) +
                    "&password=" + URLEncoder.encode(Const.PFI_PASSWORD, StandardCharsets.UTF_8) +
                    "&remember=1";

            request = HttpRequest.newBuilder()
                    .header("Content-Type", CONTENT_TYPE)
                    .header("X-October-Request-Handler", "onSignin")
                    .header("X-Requested-With", "XMLHttpRequest")
                    .header("User-Agent", USER_AGENT)
                    .uri(URI.create("https://work.hendz.ru:10294/account/login"))
                    .POST(HttpRequest.BodyPublishers.ofString(formData))
                    .build();

            response = client.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println("response.statusCode() = " + response.statusCode());
            List<String> cookies = response.headers().allValues("set-cookie");

            for (String cookie : cookies) {
                if (cookie.startsWith("user_auth")) {
                    userAuthCookie = cookie;
                }else {
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

}
