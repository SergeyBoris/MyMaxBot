package org.example.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.entity.Contragents.Contragent;
import org.example.entity.Req;

import java.net.http.HttpClient;
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

    public boolean closeRequest(String contragentType, String requestNumber, List<String> photoUrls) {
        Contragent contragent = contragentFactory.getContragent(contragentType);
        Optional<Req> requestByNumber = contragent.getRequestByNumber(requestNumber);
         // Проверяем, что заявка найдена
        if (requestByNumber.isEmpty()) {
            System.out.println("Заявка " + requestNumber + " не найдена");
            return false;
        }
        return contragent.closeReq(requestByNumber.get());  // извлекаем Req из Optional
    }
}
