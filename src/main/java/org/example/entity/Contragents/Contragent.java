package org.example.entity.Contragents;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.mail.Message;
import org.example.entity.Req;
import org.example.util.UserUploadSession;

import java.net.http.HttpClient;
import java.util.List;
import java.util.Optional;

public interface Contragent {

    List<Req> getAllRequests();
    Optional<Req> getRequestByNumber(String requestNumber);
    String getContragentName();
    void searchReqRest(HttpClient client, ObjectMapper mapper);
    void addReqToCash(Req req);
    void processNewEmail(Message message);
    default boolean closeReq(Req req, UserUploadSession userUploadSession) {
        return true;
    }


}