package org.example.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.entity.Contragents.Contragent;
import org.example.entity.Req;
import org.example.util.UserUploadSession;

import java.net.http.HttpClient;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

public class RequestService {

    private final ContragentFactory contragentFactory;
    private final HttpClient client;
    private final ObjectMapper mapper;

    public RequestService(ContragentFactory contragentFactory, HttpClient client, ObjectMapper mapper) {
        this.contragentFactory = contragentFactory;
        this.client = client;
        this.mapper = mapper;
    }

    public List<Req> getAllRequests(String contragentType) {
        Contragent contragent = contragentFactory.getContragent(contragentType);
        return contragent.getAllRequests();
    }

    public Optional<Req> getRequestByNumber(String contragentType, String requestNumber) {
        Contragent contragent = contragentFactory.getContragent(contragentType);
        return contragent.getRequestByNumber(requestNumber);
    }

    public boolean closeRequest(UserUploadSession userUploadSession) {
        Contragent contragent = contragentFactory.getContragent(userUploadSession.getContragent());
        Req req = getRequestByNumber(userUploadSession.getContragent(),userUploadSession.getRequestNumber()).orElse(null);
        if (req == null) {
            return false;
        }

        return contragent.closeReq(req, userUploadSession);  // извлекаем Req из Optional
    }
}
