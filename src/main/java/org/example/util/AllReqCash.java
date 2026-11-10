package org.example.util;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.spring.repo.RequestRepo;
import org.example.spring.service.ReqService;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@Getter
@RequiredArgsConstructor
public class AllReqCash {

    private final RequestRepo requestRepo;   // ← репозиторий, не сервис
    private final Set<String> cash = ConcurrentHashMap.newKeySet();

    @PostConstruct
    protected void init() {
        cash.addAll(requestRepo.findAllRequestNumbers());
        log.info("Кэш загружен: {}", cash.size());
    }

    public boolean contains(String number) {
        return number != null && cash.contains(number);
    }

    public void addReqNumber(String number) {
        if (number != null) cash.add(number);
    }

    public Set<String> asSet() {
        return Collections.unmodifiableSet(cash);
    }

    public int size() { return cash.size(); }
}