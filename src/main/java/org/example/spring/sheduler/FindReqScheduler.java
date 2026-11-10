package org.example.spring.sheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.processors.FindReqProcessor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class FindReqScheduler {

    private final FindReqProcessor findReqProcessor;

    @Scheduled(fixedDelayString = "${bot.find-req-delay-ms:60000}")
    public void run() {
        log.debug("⏰ FindReqScheduler: старт опроса");
        try {
            findReqProcessor.process();
        } catch (Exception e) {
            log.error("FindReqScheduler: ошибка: {}", e.getMessage(), e);
        }
    }
}