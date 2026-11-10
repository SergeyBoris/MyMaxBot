package org.example.services;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.entity.Contragents.Contragent;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@Getter
public class ContragentFactory {

    private final List<Contragent> contragents;   // Spring внедрит ВСЕ @Component-реализации Contragent
    private final Map<String, Contragent> byName = new HashMap<>();

    @PostConstruct
    public void init() {
        for (Contragent c : contragents) {
            byName.put(c.getContragentName(), c);
            log.info("Зарегистрирован контрагент: {}", c.getContragentName());
        }
    }

    public Contragent getContragent(String name) {
        return byName.get(name);
    }
}