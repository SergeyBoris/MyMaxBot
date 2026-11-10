package org.example.entity.Contragents;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
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
import org.example.util.UserUploadSession;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.net.http.HttpClient;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Service
public class Hendz implements Contragent {

    private final MessageService messageService;
    private final UserService userService;
    private final ReqService reqService;
    private EmailMonitorService emailMonitorService;
    private Map<String,Req> notInDbReqs = new ConcurrentHashMap<>();
    private final String name = "HENDZ";

    @Override
    public InlineKeyboard getContragentReqKeyboard() {
        return Const.KEYBOARD_ALL_REQ;
    }

    @Override
    public List<Req> getAllRequests() {
        return reqService.getNewWithPending(name, notInDbReqs.values(),getSorter());
    }

    @Override
    public Optional<Req> getRequestByNumber(String requestNumber) {
        return Optional.empty();
    }

    @Override
    public String getContragentName() {
        return name;
    }

    @Override
    public void specialContragentAction(String action, UserUploadSession userUploadSession) {

    }

    @Override
    public void searchReqRest(HttpClient client, ObjectMapper mapper) {

    }

    @Override
    public void addNewReq(Req req, List<Long> allUserIds) {

        if(!reqService.addReq(req)){
            String error = Const.REQ_ADD_ERROR_MESSAGE;
            log.error("addNewReq: ошибка заявки {}", req.getRequestNumber() );
            req.setRequestText(req.getRequestText() + error);
            notInDbReqs.put(req.getRequestNumber(),req);
        }

        for (Long getUserChatId : allUserIds) {
            messageService.sendSimpleMessage(getUserChatId, req.toString(), Const.KEYBOARD_ATTACHMENT_TO_ALL_REQ_MKB);
        }
        log.info("New reqMaxDTO {}", req.getRequestNumber());

    }

    @Override
    public boolean closeReq(Req req, UserUploadSession userUploadSession) {
        reqService.setReqStatus(req.getRequestNumber(),ReqStatus.CLOSED);
        return true;
    }

    @Override
    public void processNewEmail(Message message) {
        StringBuilder sb = new StringBuilder();


        try {
            List<Long> allUserIds = userService.findAll().stream()
                    .map(User::getChatId)
                    .toList();

            sb.append(message.getSubject());


            System.out.println("Новое письмо:!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
            System.out.println(sb.toString());
            System.out.println("Новое письмо конец!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
            String messageText = sb.toString();
            if (messageText.contains("До истечения регламентного времени")) {
                for (Long userChatId : allUserIds) {
                    messageService.sendSimpleMessage(userChatId, messageText, Const.KEYBOARD_ALL_REQ);
                }
            } else if (messageText.contains("назначена на инженера")) {
                addNewReq(parseToReq(message),allUserIds);
            } else if (messageText.contains("Задача выполнена") || messageText.contains("Задача ОТМЕНЕНА")) {
                Req req = parseToReq(message);
                for (Long userChatId : allUserIds) {
                    messageService.sendSimpleMessage(userChatId, messageText, Const.KEYBOARD_ALL_REQ);
                }
                closeReq(req, null);
                log.info("reqMaxDTO HENDS autoclosed {}", req.getRequestNumber());
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    private Req parseToReq(Message message) throws MessagingException {
        Req req = new Req();
        String subject = message.getSubject();
        Pattern pattern = Pattern.compile("IM\\d+-\\d+");
        Matcher matcher = pattern.matcher(subject);

        if (matcher.find()) {
            String result = matcher.group();
            req.setRequestNumber(result);


        } else {
            throw new RuntimeException("ошибка парсинга заявки");
        }

        String messageBody = null;
        try {
            messageBody = emailMonitorService.getEmailBody(message);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        int addressBeginIndex = messageBody.indexOf("Регион установки");
        int addressEndIndex = messageBody.indexOf("Место установки");
        if (addressEndIndex != -1 && addressBeginIndex != -1) {
            req.setRequestAddress(messageBody.substring(addressBeginIndex, addressEndIndex));
        }
        int bodyBeginIndex = messageBody.indexOf("Описание");
        int bodyEndIndex = messageBody.indexOf("Статус");
        if (bodyBeginIndex != -1 && bodyEndIndex != -1) {
            req.setRequestText(messageBody.substring(bodyBeginIndex, bodyEndIndex));
        }
        int slaBeginIndex = messageBody.indexOf("время (PFT)");

        if (slaBeginIndex != -1) {
            pattern = Pattern.compile("\\b\\d{2}\\.\\d{2}\\.\\d{4} \\d{2}:\\d{2}:\\d{2}\\b");
            matcher = pattern.matcher(messageBody.substring(slaBeginIndex));

            if (matcher.find()) {
                String result = matcher.group();
                req.setSla(result);
            }
        }

        req.setContragent(this.name);
        return req;
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
