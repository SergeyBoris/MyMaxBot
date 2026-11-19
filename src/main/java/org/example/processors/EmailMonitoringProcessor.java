package org.example.processors;

import jakarta.mail.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.entity.Contragents.Contragent;
import org.example.services.ContragentFactory;
import org.example.services.EmailMonitorService;
import org.springframework.stereotype.Service;

import jakarta.annotation.PreDestroy;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailMonitoringProcessor {

    private final EmailMonitorService monitor;
    private final ContragentFactory contragentFactory;

    /**
     * Один цикл проверки почты. Вызывается из EmailScheduler @Scheduled.
     */
    public void process() {
        if (!monitor.isConnected()) {
            try {
                monitor.connect();
            } catch (Exception e) {
                log.error("Ошибка подключения к почте: {}", e.getMessage(), e);
                return;
            }
        }

        try {
            Message[] newMessages = monitor.checkNewMessages();
            if (newMessages == null || newMessages.length == 0) {
                return;
            }

            for (Message message : newMessages) {
                try {
                    Folder folder = monitor.getInbox();
                    Address[] from = message.getFrom();
                    UIDFolder uidFolder = (UIDFolder) folder;
                    long uid = uidFolder.getUID(message);

                    if (from == null || from.length == 0) {
                        log.warn("Письмо без отправителя — пропускаем");
                        continue;
                    }
                    String address = from[0].toString();
                    log.debug("получено письмо от: {}", address);
                    Contragent target = resolveContragent(address);
                    if (target == null) {
                        log.debug("Нет контрагента для отправителя: {}", address);
                        // всё равно помечаем прочитанным, чтобы не зацикливаться
                    } else {
                        target.processNewEmail(message, uid);
                    }

                    message.setFlag(Flags.Flag.SEEN, true);

                } catch (Exception e) {
                    log.error("Ошибка обработки письма от {}: {}",
                            (Object) message.getFrom(), e.getMessage(), e);
                    // не ставим SEEN — письмо останется для повтора
                }
            }
        } catch (Exception e) {
            log.error("Ошибка при проверке почты: {}", e.getMessage(), e);
        }
    }

    /**
     * Определяет контрагента по адресу отправителя.
     */
    private Contragent resolveContragent(String address) {
        if (address == null) return null;
        if (address.contains("<strike_mf@mail.ru>")) {
            return contragentFactory.getContragent("STRIKE");
        }
        if (address.contains("sd4@hendz.ru")) {
            return contragentFactory.getContragent("HENDZ");
        }
        if (address.contains("@elecsnet.ru") || address.contains("termhelp@mkb.ru")) {
            return contragentFactory.getContragent("MKB");
        }
        return null;
    }

    /**
     * Spring вызывает при shutdown контекста — закрываем IMAP.
     */
    @PreDestroy
    public void shutdown() {
        try {
            monitor.disconnect();
            log.info("EmailMonitor: IMAP отключён");
        } catch (Exception e) {
            log.warn("Ошибка отключения IMAP: {}", e.getMessage());
        }
    }
}