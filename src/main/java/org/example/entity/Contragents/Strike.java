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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class Strike implements Contragent {

    private final Map<String, Req> cashedRequests = new ConcurrentHashMap<>();
    private final MessageService messageService;
    private final UserService userService;
    private final EmailMonitorService emailMonitorService;
    private final ReqService reqService;
    private final Map<String, Req> cashNewReq = new ConcurrentHashMap<>();
    private final String name = "STRIKE";

    @Override
    public void initCash() {

    }

    @Override
    public InlineKeyboard getContragentReqKeyboard() {
        return null;
    }

    @Override
    public void specialContragentAction(String action, UserUploadSession userUploadSession) {

    }

    @Override
    public List<Req> getAllRequests() {
        return reqService.findByStatusesAndContragent(List.of(ReqStatus.NEW,ReqStatus.CLOSED_BY_ENGINEER),name)
                .values()
                .stream()
                .sorted(getSorter())
                .toList();
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
    public void processNewEmail(Message message, Long uid) {
        StringBuilder sb = new StringBuilder();


        try {
            List<Long> allUserIds = userService.findAll().stream()
                    .map(User::getChatId)
                    .toList();
            sb.append(emailMonitorService.getEmailBody(message));
            System.out.println("Новое письмо:!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
            System.out.println(sb.toString());
            System.out.println("Новое письмо конец!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
            for (Long userChatId : allUserIds) {
                messageService.sendSimpleMessage(userChatId,sb.toString(), Const.KEYBOARD_ALL_REQ);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void searchReqRest(HttpClient client, ObjectMapper mapper) {

    }


    @Override
    public void addNewReq(Req req, List<Long> userChatId) {

    }


}
