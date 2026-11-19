package org.example.entity.Contragents;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.mail.Message;
import org.example.entity.InlineKeyboard;
import org.example.entity.Req;
import org.example.entity.User;
import org.example.util.UserUploadSession;
import org.jetbrains.annotations.NotNull;
import org.springframework.scheduling.annotation.Scheduled;

import java.net.http.HttpClient;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public interface Contragent {
    Comparator<Req> BY_SLA = Comparator.comparing(Req::getSla,
            Comparator.nullsLast(Comparator.reverseOrder()));

    @PostConstruct
    void initCash();
    List<Req> getAllRequests();
    Optional<Req> getRequestByNumber(String requestNumber);
    String getContragentName();
    void searchReqRest(HttpClient client, ObjectMapper mapper);
    void addNewReq(Req req, List<Long> allUserIds);
    void processNewEmail(Message message, Long uid);
    void specialContragentAction(String action, UserUploadSession userUploadSession);
    InlineKeyboard getContragentReqKeyboard();
    default Comparator<Req> getSorter() {
        return BY_SLA;
    }
    default boolean closeReq(Req req, UserUploadSession userUploadSession) {
        return true;
    }


}