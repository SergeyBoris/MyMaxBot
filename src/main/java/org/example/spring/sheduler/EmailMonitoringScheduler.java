package org.example.spring.sheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.processors.EmailMonitoringProcessor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailMonitoringScheduler {

    private final EmailMonitoringProcessor processor;

    private final AtomicInteger consecutiveFailures = new AtomicInteger(0);
    private volatile boolean enabled = true;

    private static final int MAX_CONSECUTIVE_FAILURES = 5;

    /**
     * Каждые 60 секунд (после завершения предыдущего вызова).
     * Если подряд MAX_CONSECUTIVE_FAILURES ошибок — останавливаем (enabled = false).
     */
    @Scheduled(fixedDelayString = "${bot.email-check-delay-ms:60000}")
    public void checkEmails() {
        if (!enabled) {
            return;   // уже остановлен после серии ошибок todo сделать отображение активности в трее
        }

        try {
            processor.process();
            if (consecutiveFailures.getAndSet(0) > 0) {
                log.info("Email мониторинг восстановлен после ошибок");
            }
        } catch (Exception e) {
            int fails = consecutiveFailures.incrementAndGet();
            log.error("Ошибка email мониторинга ({} из {}): {}",
                    fails, MAX_CONSECUTIVE_FAILURES, e.getMessage(), e);

            if (fails >= MAX_CONSECUTIVE_FAILURES) {
                log.error("Превышено {} ошибок подряд — останавливаем email мониторинг",
                        MAX_CONSECUTIVE_FAILURES);
                enabled = false;
                processor.shutdown();   // закрываем IMAP
            }
        }
    }

    /**
     * Spring вызовет при shutdown контекста (Ctrl+C, context.close()).
     */
    @PreDestroy
    public void shutdown() {
        enabled = false;
        processor.shutdown();
        log.info("EmailMonitoringScheduler остановлен");
    }
}