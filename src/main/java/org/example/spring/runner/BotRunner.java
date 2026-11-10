package org.example.spring.runner;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.entity.Update;
import org.example.processors.UpdateProcessor;
import org.example.services.UpdatesService;
import org.example.spring.config.BotProperties;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class BotRunner implements CommandLineRunner {

    private final UpdatesService updatesService;
    private final UpdateProcessor updateProcessor;
    private final BotProperties props;

    @Override
    public void run(String... args) {
        log.info("🤖 Бот запущен, long-poll стартует...");

        while (true) {
            try {
                List<Update> updates = updatesService.getUpdate();
                if (updates != null && !updates.isEmpty()) {
                    for (Update update : updates) {
                        updateProcessor.startProcess(update);
                        Thread.sleep(props.getUpdateDelayMs());
                    }
                }
            } catch (Exception e) {
                log.error("Ошибка long-poll: {}", e.getMessage(), e);
                try { Thread.sleep(5000); } catch (InterruptedException ie) { break; }
            }
        }
    }
}