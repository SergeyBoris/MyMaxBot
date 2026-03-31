package org.example.processors;


import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.entity.Contragents.Contragent;
import org.example.services.ContragentFactory;

import java.net.http.HttpClient;
import java.util.Map;

public class FindReqProcessor implements Runnable {
    private final ContragentFactory contragentFactory;
    private final HttpClient client;
    private final ObjectMapper mapper;
    private boolean running = true;
    public FindReqProcessor(ContragentFactory contragentFactory, HttpClient client, ObjectMapper mapper){
        this.contragentFactory = contragentFactory;
        this.client = client;
        this.mapper = mapper;
    }

    @Override
    public void run() {
        Map<String, Contragent> contragentMap = contragentFactory.getContragentMap();
        while(running){
            try {
                Thread.sleep(2 * 60 * 1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        contragentMap.forEach((k, v) -> {
            try {

                v.searchReqRest(client,mapper);
            }catch (Exception e){
                e.printStackTrace();
            }
        });
        }
    }
    public void shutdown() {
        running = false;
        System.out.println("Отправлена остановка потока мониторинга ");
    }
}
