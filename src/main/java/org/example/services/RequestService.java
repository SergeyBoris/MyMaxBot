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
}
