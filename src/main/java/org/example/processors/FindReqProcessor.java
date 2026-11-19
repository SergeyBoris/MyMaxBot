package org.example.processors;


import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.entity.Contragents.Contragent;
import org.example.services.ContragentFactory;
import org.example.util.AllReqCash;
import org.springframework.stereotype.Service;

import java.net.http.HttpClient;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class FindReqProcessor {

    private final ContragentFactory contragentFactory;
    private final HttpClient client;
    private final ObjectMapper mapper;
    private final AllReqCash allReqCash;

    /**
     * Один проход опроса всех контрагентов.
     * Вызывается из @Scheduled каждые N минут.
     */
    public void process() {
        List<Contragent> contragents = contragentFactory.getContragents();
        if (contragents == null || contragents.isEmpty()) {
            log.debug("FindReqProcessor: нет контрагентов для опроса");
            return;
        }
        log.debug("FindReqProcessor: опрос {} контрагентов", contragents.size());

        for (Contragent contragent : contragents) {
            try {
                contragent.searchReqRest(client, mapper);
            } catch (Exception e) {
                log.error("FindReqProcessor: ошибка контрагента {}: {}",
                        contragent.getContragentName(), e.getMessage(), e);
                // не прерываем цикл — остальные контрагенты должны опроситься
            }
        }
    }
}