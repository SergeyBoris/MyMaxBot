package org.example.processors;

import jakarta.mail.Address;
import jakarta.mail.Flags;
import jakarta.mail.Message;
import lombok.RequiredArgsConstructor;
import org.example.entity.Contragents.Contragent;
import org.example.services.ContragentFactory;
import org.example.services.EmailMonitorService;

@RequiredArgsConstructor
public class EmailMonitoringProcessor implements Runnable {
    private final EmailMonitorService monitor;
    private final ContragentFactory contragentFactory;
    private volatile boolean running = true;

    @Override
    public void run() {
        // Если монитор не подключен — подключаемся
        if (!monitor.isConnected()) {
            try {
                monitor.connect();
            } catch (Exception e) {
                System.err.println("Ошибка подключения к почте: " + e.getMessage());
                return;
            }
        }

        try {
            Message[] newMessages = monitor.checkNewMessages();
            for (Message message : newMessages) {
                String address = message.getFrom()[0].toString();
                System.out.println("\n ------------ \n" + address + "\n ------------ \n");
                System.out.println(message.getFrom());

                if (address.contains("<strike_mf@mail.ru>")) {
                    Contragent strike = contragentFactory.getContragentMap().get("STRIKE");
                    strike.processNewEmail(message);
                } else if (address.contains("sd4@hendz.ru")) {
                    contragentFactory.getContragentMap().get("HENDZ").processNewEmail(message);
                }
                // Помечаем как прочитанное
                message.setFlag(Flags.Flag.SEEN, true);
            }
        } catch (Exception e) {
            System.err.println("Ошибка при проверке почты: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void stop() {
        running = false;
        try {
            monitor.disconnect();
        } catch (Exception ignored) {}
    }
}