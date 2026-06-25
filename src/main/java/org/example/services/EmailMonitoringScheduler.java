package org.example.services;

import org.example.processors.EmailMonitoringProcessor;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class EmailMonitoringScheduler {
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final EmailMonitoringProcessor processor;
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private int consecutiveFailures = 0;
    private static final int MAX_CONSECUTIVE_FAILURES = 5;

    public EmailMonitoringScheduler(EmailMonitoringProcessor processor) {
        this.processor = processor;
    }

    public void start() {
        if (isRunning.get()) {
            System.out.println("Email мониторинг уже запущен");
            return;
        }

        isRunning.set(true);
        consecutiveFailures = 0;

        // Запускаем с фиксированной задержкой 30 секунд между выполнениями
        scheduler.scheduleWithFixedDelay(() -> {
            if (!isRunning.get()) {
                return;
            }

            try {
                processor.run();
                consecutiveFailures = 0; // сброс при успехе
            } catch (Exception e) {
                consecutiveFailures++;
                System.err.println("Ошибка при выполнении email мониторинга: " + e.getMessage());
                e.printStackTrace();

                if (consecutiveFailures >= MAX_CONSECUTIVE_FAILURES) {
                    System.err.println("Превышено количество ошибок (" + MAX_CONSECUTIVE_FAILURES + "). Останавливаем мониторинг.");
                    stop();
                }
            }
        }, 0, 30, TimeUnit.SECONDS); // начальная задержка 0, интервал 30 секунд

        System.out.println("Email мониторинг запущен с интервалом 30 секунд");
    }

    public void stop() {
        if (!isRunning.get()) {
            return;
        }

        isRunning.set(false);
        processor.stop();
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(10, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        System.out.println("Email мониторинг остановлен");
    }

    public boolean isRunning() {
        return isRunning.get() && !scheduler.isShutdown();
    }

    public void restart() {
        stop();
        // Даём время на остановку
        try {
            Thread.sleep(1000);
        } catch (InterruptedException ignored) {}
        start();
    }
}