package org.example.processors;

import jakarta.mail.Address;
import jakarta.mail.Flags;
import jakarta.mail.Message;
import lombok.AllArgsConstructor;
import org.example.entity.Contragents.Contragent;
import org.example.services.ContragentFactory;
import org.example.services.EmailMonitorService;

@AllArgsConstructor
public class EmailMonitoringProcessor implements Runnable{
    private final EmailMonitorService monitor;
    private final ContragentFactory contragentFactory;
    @Override
    public void run() {
        try {
            monitor.connect();

            while (true) {
                Message[] newMessages = monitor.checkNewMessages();
                for (Message message : newMessages) {
                    String address = message.getFrom()[0].toString();
                    System.out.println("\n ------------ \n" + address + "\n ------------ \n");
                    System.out.println(message.getFrom());
                    if (address.contains("<strike_mf@mail.ru>")) {
                        Contragent strike = contragentFactory.getContragentMap().get("STRIKE");
                        strike.processNewEmail(message);
                    }else if (address.contains("sd4@hendz.ru")) {
                        contragentFactory.getContragentMap().get("HENDZ").processNewEmail(message);
                    }
                    // Помечаем как прочитанное
                    message.setFlag(Flags.Flag.SEEN, true);
                }
                Thread.sleep(30000); // Проверка каждые 30 секунд
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                monitor.disconnect();
            } catch (Exception ignored) {}
        }
    }
}
