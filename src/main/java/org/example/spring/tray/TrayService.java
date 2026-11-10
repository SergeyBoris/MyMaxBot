package org.example.spring.tray;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.util.VersionUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Objects;

@Slf4j
@Component
@RequiredArgsConstructor
public class TrayService {

    private final ApplicationContext context;
    @Value("${bot.tray-enabled:true}")
    private boolean trayEnabled;
    private TrayIcon trayIcon;

    /**
     * Spring вызовет при старте контекста.
     * Если трей не поддерживается (headless / сервер) — просто логируем и выходим.
     */
    @PostConstruct
    public void init() {

        log.info("java.awt.headless = {}", System.getProperty("java.awt.headless"));
        log.info("GraphicsEnvironment.isHeadless() = {}", GraphicsEnvironment.isHeadless());
        log.info("SystemTray.isSupported() = {}", SystemTray.isSupported());
        log.info("OS = {}", System.getProperty("os.name"));
        if (!trayEnabled) {
            log.info("Трей отключён в конфиге");
            return;
        }
        if (!SystemTray.isSupported()) {
            log.info("SystemTray не поддерживается — трей отключён (headless-режим)");
            return;
        }
        if (GraphicsEnvironment.isHeadless()) {
            log.info("Headless-режим — трей отключён");
            return;
        }

        try {
            setupTray();
            log.info("SystemTray инициализирован");
        } catch (Exception e) {
            log.warn("Не удалось инициализировать SystemTray: {}", e.getMessage());
        }
    }

    private void setupTray() throws Exception {
        SystemTray tray = SystemTray.getSystemTray();

        Image image;
        try {
            image = ImageIO.read(Objects.requireNonNull(
                    TrayService.class.getResource("/tray.png")));
        } catch (Exception e) {
            image = createDefaultIcon();
        }

        trayIcon = new TrayIcon(image, "Бот MAX " + VersionUtil.getFullInfo());
        trayIcon.setImageAutoSize(true);

        PopupMenu popup = new PopupMenu();

        MenuItem exitItem = new MenuItem("Выход");
        exitItem.addActionListener(e -> shutdown());
        popup.add(exitItem);

        trayIcon.setPopupMenu(popup);

        try {
            tray.add(trayIcon);
        } catch (AWTException e) {
            log.warn("Не удалось добавить иконку в трей: {}", e.getMessage());
        }

        trayIcon.displayMessage("Бот запущен", "Работает в фоне", TrayIcon.MessageType.INFO);
    }

    private void shutdown() {
        log.info("Выход через трей — останавливаем Spring-контекст");
        if (trayIcon != null) {
            SystemTray.getSystemTray().remove(trayIcon);
        }
        // Корректный shutdown Spring: вызовет @PreDestroy на всех бинах
        int exitCode = SpringApplication.exit(context, () -> 0);
        System.exit(exitCode);
    }

    private Image createDefaultIcon() {
        BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Graphics g = img.getGraphics();
        g.setColor(Color.GREEN);
        g.fillRect(0, 0, 16, 16);
        g.dispose();
        return img;
    }

    /**
     * Spring вызовет при shutdown контекста — убираем иконку из трея.
     */
    @jakarta.annotation.PreDestroy
    public void cleanup() {
        if (trayIcon != null) {
            try {
                SystemTray.getSystemTray().remove(trayIcon);
            } catch (Exception ignored) {}
        }
    }
}