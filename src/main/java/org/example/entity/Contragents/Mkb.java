package org.example.entity.Contragents;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
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
public class Mkb implements Contragent {
    private final MessageService messageService;
    private final AllReqCash cash;
    private final ReqService reqService;
    private final UserService userService;
    private final EmailMonitorService emailMonitorService;
    private final Map<String, Req> cashNewReq = new ConcurrentHashMap<>();
    private final String name = "MKB";


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
        return Const.KEYBOARD_ATTACHMENT_TO_ALL_REQ_MKB;
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
    public String getContragentName() {
        return name;
    }

    @Override
    public void searchReqRest(HttpClient client, ObjectMapper mapper) {

    }

    @Override
    public void addNewReq(Req req, List<Long> allUserIds) {
        cashNewReq.put(req.getRequestNumber(), req);
        if (!reqService.addReq(req)) {
            log.error("addNewReq: ошибка заявки {}", req.getRequestNumber());
            messageService.sendSimpleMessage(Const.ADMIN_CHAT_ID, req.toString() + Const.REQ_ADD_ERROR_MESSAGE, null);
        }

        for (Long getUserChatId : allUserIds) {
            messageService.sendSimpleMessage(getUserChatId, req.toString(), Const.KEYBOARD_ATTACHMENT_TO_ALL_REQ_MKB);
        }
        log.info("New reqMaxDTO {}", req.getRequestNumber());
    }

    @Override
    public void processNewEmail(Message message, Long uid) {
        boolean reqInCash = false;
        try {


        for (Req req : cashNewReq.values()) {
            if (uid.toString().equals(req.getParams().get("Email UID"))) {
                reqInCash = true;
                break;
            }
        }
        }catch (Exception e){
            log.error(e.getMessage());
        }
        if (!reqInCash) {
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
                    req.setRequestAddress(request.get("Адрес") + "\n" + request.get("Наименование"));
                    req.setCreationTime(LocalDateTime.now());
                    req.setTid(request.get("№ПТ"));
                    req.setParams("Режим работы", request.get("Режим работы"));
                    req.setParams("Email UID", uid.toString());

                    // Выполняем дополнительный код для НОВЫХ заявок
                    allRequestsFromSyte.add(req);

                }
                for (Req req : allRequestsFromSyte) {
                    // Выполняем дополнительный код для НОВЫХ заявок
                    addNewReq(req, allUserIds);
                }
            } catch (Exception e) {
                log.error("Mkb.processNewEmail: ошибка обработки письма: {}", e.getMessage(), e);
            }
        }

    }

    @Override
    public void specialContragentAction(String action, UserUploadSession userUploadSession) {
        userUploadSession.setNextMessageToUser("Напишите текст нажмите готово", Const.KEYBOARD_END_PHOTO);
    }

    @Override
    public boolean closeReq(Req req, UserUploadSession userUploadSession) {
        String reqNumber = req.getRequestNumber();
        cashNewReq.remove(reqNumber);
        //todo отправка письма
        Req reqFromDb = reqService.getReqByNumber(reqNumber);
        if (reqFromDb != null) {
            reqService.setReqStatus(reqNumber, ReqStatus.CLOSED);
        } else {
            messageService.sendSimpleMessage(Const.ADMIN_CHAT_ID, "Закрыта заявка которой нет в БД  разберись! " + reqNumber, null);
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
            // if (cells.length < headers.length) continue;

            // Пропускаем не-задачи (на случай мусора)
            // if (!"Принтер".equals(cells[0]) && !"СВЯЗЬ".equals(cells[0])) continue;

            Map<String, String> task = new LinkedHashMap<>();
            for (int i = 0; i < headers.length; i++) {
                task.put(headers[i], cells[i]);
            }
            tasks.add(task);
        }
        return tasks;
    }

}
