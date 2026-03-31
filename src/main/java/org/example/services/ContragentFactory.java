package org.example.services;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.example.db.MapDB;
import org.example.entity.Contragents.Contragent;
import org.example.entity.Contragents.Hendz;
import org.example.entity.Contragents.Pbf;
import org.example.entity.Contragents.Strike;

import java.util.HashMap;
import java.util.Map;

@Getter
public class ContragentFactory {
    private final Map<String, Contragent> contragentMap = new HashMap<>();
    private final MessageService messageService;
    private final EmailMonitorService emailMonitorService;

    // Регистрируем реализации при создании фабрики
    public ContragentFactory(MessageService messageService, MapDB db, EmailMonitorService emailMonitorService) {
        this.messageService = messageService;
        this.emailMonitorService = emailMonitorService;
        contragentMap.put("PBF", new Pbf(messageService,db));
        contragentMap.put("STRIKE", new Strike(messageService,db,emailMonitorService));
        contragentMap.put("HENDZ", new Hendz(messageService,db,emailMonitorService));
    }


    /**
     * Получает реализацию контрагента по его типу
     */
    public Contragent getContragent(String contragentType) {
        Contragent contragent = contragentMap.get(contragentType);
        if (contragent == null) {
            throw new IllegalArgumentException(
                    "Неизвестная реализация контрагента: " + contragentType);
        }
        return contragent;
    }
}
