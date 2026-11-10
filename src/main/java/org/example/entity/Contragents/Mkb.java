package org.example.entity.Contragents;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.mail.Message;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.constants.Const;
import org.example.entity.InlineKeyboard;
import org.example.entity.Req;
import org.example.entity.ReqStatus;
import org.example.entity.User;
import org.example.services.EmailMonitorService;
import org.example.services.MessageService;
import org.example.spring.service.ReqService;
import org.example.spring.service.UserService;
import org.example.util.AllReqCash;
import org.example.util.UserUploadSession;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.net.http.HttpClient;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class Mkb implements Contragent{
    private final MessageService messageService;
    private final AllReqCash cash;
    private final ReqService reqService;
    private final UserService userService;
    private final EmailMonitorService emailMonitorService;
    private final Map<String,Req> notInDbReqs = new ConcurrentHashMap<>();
    private final String name = "MKB";

    @Override
    public InlineKeyboard getContragentReqKeyboard() {
        return Const.KEYBOARD_ATTACHMENT_TO_ALL_REQ_MKB;
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
    public String getContragentName() {
        return name;
    }

    @Override
    public void searchReqRest(HttpClient client, ObjectMapper mapper) {

    }

    @Override
    public void addNewReq(Req req,List<Long> allUserIds) {
        if (!reqService.addReq(req)) {
            String error = Const.REQ_ADD_ERROR_MESSAGE;
            log.error("addNewReq: ошибка заявки {}", req.getRequestNumber());
            req.setRequestText(req.getRequestText() + error);
            notInDbReqs.put(req.getRequestNumber(), req);
        }

        for (Long getUserChatId : allUserIds) {
            messageService.sendSimpleMessage(getUserChatId, req.toString(), Const.KEYBOARD_ATTACHMENT_TO_ALL_REQ_MKB);
        }
        log.info("New reqMaxDTO {}", req.getRequestNumber());
    }

    @Override
    public void processNewEmail(Message message) {
        List<Req> allRequestsFromSyte = new LinkedList<>();
        try {
            List<Long> allUserIds = userService.findAll().stream()
                    .map(User::getChatId)
                    .toList();

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

                    // Выполняем дополнительный код для НОВЫХ заявок
                allRequestsFromSyte.add(req);

            }
            for (Req req : allRequestsFromSyte) {
                String number = req.getRequestNumber();

                if (!cash.contains(number)) {
                    // Выполняем дополнительный код для НОВЫХ заявок
                    addNewReq(req, allUserIds);
                }
            }
        } catch (Exception e) {
            log.error("Mkb.processNewEmail: ошибка обработки письма: {}", e.getMessage(), e);
        }

    }

    @Override
    public void specialContragentAction(String action, UserUploadSession userUploadSession) {
        userUploadSession.setNextMessageToUser("Напишите текст нажмите готово", Const.KEYBOARD_END_PHOTO);
    }

    @Override
    public boolean closeReq(Req req, UserUploadSession userUploadSession) {
        String reqNumber = req.getRequestNumber();
        //todo отправка письма
        Req reqFromDb = reqService.getReqByNumber(reqNumber);
        if (reqFromDb != null) {
            reqService.setReqStatus(reqNumber, ReqStatus.CLOSED);
        }else if(notInDbReqs.containsKey(reqNumber)) {
            notInDbReqs.get(reqNumber).setStatus(ReqStatus.CLOSED);
        }else{
            messageService.sendSimpleMessage(Const.ADMIN_CHAT_ID,"Закрыта заявка которой нет в БД и нет в кеше недобавленых заявок разберись! " + reqNumber, null);
            return false;
        }
        return true;
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
