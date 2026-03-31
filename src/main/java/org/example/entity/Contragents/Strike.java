package org.example.entity.Contragents;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.mail.BodyPart;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import lombok.AllArgsConstructor;
import org.example.constants.Const;
import org.example.db.MapDB;
import org.example.entity.Req;
import org.example.services.EmailMonitorService;
import org.example.services.MessageService;

import java.io.IOException;
import java.net.http.HttpClient;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@AllArgsConstructor
public class Strike implements Contragent {

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
    public void processNewEmail(Message message) {
        StringBuilder sb = new StringBuilder();


        try {
            sb.append(emailMonitorService.getEmailBody(message));
            System.out.println("Новое письмо:!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
            System.out.println(sb.toString());
            System.out.println("Новое письмо конец!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
            for (Long user : db.getAllUsers()) {
                messageService.sendSimpleMessage(db.getUserChatId(user),sb.toString(), Const.KEYBOARD_ALL_REQ);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void searchReqRest(HttpClient client, ObjectMapper mapper) {

    }


    @Override
    public void addReqToCash(Req req) {

    }
}
