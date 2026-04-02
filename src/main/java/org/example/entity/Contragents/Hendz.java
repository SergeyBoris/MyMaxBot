package org.example.entity.Contragents;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import lombok.AllArgsConstructor;
import org.example.constants.Const;
import org.example.db.MapDB;
import org.example.entity.Req;
import org.example.services.EmailMonitorService;
import org.example.services.MessageService;
import java.net.http.HttpClient;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@AllArgsConstructor
public class Hendz implements Contragent {

    private final Map<String, Req> cashedRequests = new ConcurrentHashMap<>();
    private final MessageService messageService;
    private final MapDB db;
    private EmailMonitorService emailMonitorService;


    @Override
    public List<Req> getAllRequests() {
        return cashedRequests.values().stream().toList();
    }

    @Override
    public Optional<Req> getRequestByNumber(String requestNumber) {
        return Optional.empty();
    }

    @Override
    public String getContragentType() {
        return "";
    }

    @Override
    public void searchReqRest(HttpClient client, ObjectMapper mapper) {

    }

    @Override
    public void addReqToCash(Req req) {
        for (Long user : db.getAllUsers()) {
            messageService.sendSimpleMessage(db.getUserChatId(user), req.toString(), Const.KEYBOARD_ALL_REQ);
        }
        cashedRequests.put(req.getRequestNumber(), req);
    }

    @Override
    public void processNewEmail(Message message) {
        StringBuilder sb = new StringBuilder();


        try {
            sb.append(message.getSubject());


            System.out.println("Новое письмо:!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
            System.out.println(sb.toString());
            System.out.println("Новое письмо конец!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
            String messageText = sb.toString();
            if (messageText.contains("До истечения регламентного времени")) {
                for (Long user : db.getAllUsers()) {
                    messageService.sendSimpleMessage(db.getUserChatId(user), messageText, Const.KEYBOARD_ALL_REQ);
                }
            } else if (messageText.contains("назначена на инженера")) {
                addReqToCash(parseToReq(message));
            } else if (messageText.contains("Задача выполнена") || messageText.contains("Задача ОТМЕНЕНА")) {
                Req req = parseToReq(message);
                for (Long user : db.getAllUsers()) {
                    messageService.sendSimpleMessage(db.getUserChatId(user), messageText, Const.KEYBOARD_ALL_REQ);
                }
                    cashedRequests.remove(req.getRequestNumber());

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

        if (slaBeginIndex != -1 ) {
             pattern = Pattern.compile("\\b\\d{2}\\.\\d{2}\\.\\d{4} \\d{2}:\\d{2}:\\d{2}\\b");
             matcher = pattern.matcher(messageBody.substring(slaBeginIndex));

            if (matcher.find()) {
                String result = matcher.group();
                req.setSla(result);
            }
            }




        return req;
    }
}
