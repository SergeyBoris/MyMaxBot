package org.example.entity.Contragents;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.mail.Message;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.constants.Const;
import org.example.db.MapDB;
import org.example.entity.Dto.ReqMaxDto;
import org.example.entity.InlineKeyboard;
import org.example.entity.Req;
import org.example.services.EmailMonitorService;
import org.example.services.MessageService;
import org.example.util.UserUploadSession;

import java.net.http.HttpClient;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@AllArgsConstructor
public class Mkb implements Contragent{
    private final Map<String, Req> cashedRequests = new ConcurrentHashMap<>();
    private final MessageService messageService;
    private final MapDB db;
    private EmailMonitorService emailMonitorService;
    private final String name;

    @Override
    public InlineKeyboard getContragentReqKeyboard() {
        return Const.KEYBOARD_ATTACHMENT_TO_ALL_REQ_ONLY_CLOSE;
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
    public String getContragentName() {
        return name;
    }

    @Override
    public void searchReqRest(HttpClient client, ObjectMapper mapper) {

    }

    @Override
    public void addReqToCash(Req req) {
        for (Long user : db.getAllUsers()) {
            messageService.sendSimpleMessage(db.getUserChatId(user), req.toString(), Const.KEYBOARD_ATTACHMENT_TO_ALL_REQ_ONLY_CLOSE);
        }

        cashedRequests.put(req.getRequestNumber(), req);
        log.info("New req {}", req.getRequestNumber());
    }

    @Override
    public void processNewEmail(Message message) {

        try {

            String messageBody = emailMonitorService.getTextForLlm(message);
            System.out.println("!!!!!!!!!!!!!!!!!!!!!!");
            List<Map<String, String>> requests = parseTasksNamed(messageBody);
            for (Map<String, String> request : requests) {
                Req req = new Req();
                req.setContragent(name);
                String rnd = UUID.randomUUID().toString().substring(0, 3);
                req.setRequestNumber("MKB-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyMMddHHmmss")) + "-" + rnd);
                req.setRequestText(request.get("Ошибка/Время ошибки"));
                req.setRequestAddress(request.get("Адрес") +"\n" + request.get("Наименование"));
                req.setCreationTime(LocalDateTime.now());
                req.setTid(request.get("№ПТ"));
                req.setParams("Режим работы",request.get("Режим работы"));
                if (!cashedRequests.containsKey(req.getRequestNumber())) {
                    // Выполняем дополнительный код для НОВЫХ заявок
                    addReqToCash(req);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public void specialContragentAction(String action, UserUploadSession userUploadSession) {

    }

    @Override
    public boolean closeReq(Req req, UserUploadSession userUploadSession) {
        return Contragent.super.closeReq(req, userUploadSession);
    }

    private List<Map<String, String>> parseTasksNamed(String text) {
        List<Map<String, String>> tasks = new ArrayList<>();
        if (text == null || text.isBlank()) return tasks;

        String[] lines = text.split("\\r?\\n");
        String[] headers = null;

        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;

            String[] cells = line.split("\\s*\\|\\s*", -1);

            // Первая непустая строка — заголовок
            if (headers == null) {
                headers = cells;
                continue;
            }

            // Строка данных: должно совпадать по числу колонок
            if (cells.length < headers.length) continue;

            // Пропускаем не-задачи (на случай мусора)
            if (!"Принтер".equals(cells[0]) && !"СВЯЗЬ".equals(cells[0])) continue;

            Map<String, String> task = new LinkedHashMap<>();
            for (int i = 0; i < headers.length; i++) {
                task.put(headers[i], cells[i]);
            }
            tasks.add(task);
        }
        return tasks;
    }
}
