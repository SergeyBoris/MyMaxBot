package org.example.entity.Contragents;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.mail.Message;
import org.example.entity.Req;

import java.net.http.HttpClient;
import java.util.List;
import java.util.Optional;

public interface Contragent {

    List<Req> getAllRequests();
    Optional<Req> getRequestByNumber(String requestNumber);
    String getContragentType();
    void searchReqRest(HttpClient client, ObjectMapper mapper);
    void addReqToCash(Req req);
    void processNewEmail(Message message);


}