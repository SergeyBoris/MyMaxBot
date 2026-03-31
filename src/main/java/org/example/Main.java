package org.example;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.constants.Const;
import org.example.db.MapDB;
import org.example.entity.Update;
import org.example.processors.*;
import org.example.services.*;
import org.example.util.Config;
import org.example.util.ConfigLoader;
import org.example.util.UserUploadSession;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.net.http.HttpClient;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class Main {
    public static final Map<Long, UserUploadSession> usersSessions = new ConcurrentHashMap<>();
    private static final HttpClient client = HttpClient.newHttpClient();
    private static final ObjectMapper mapper = new ObjectMapper();
    private static TrayIcon trayIcon;
    private final static MapDB db = new MapDB(mapper);
    private static boolean running = true;


    public static void main(String[] args) throws Exception {
        if (!SystemTray.isSupported()) {
            System.err.println("Системный трей не поддерживается на этой платформе.");
            return;
        }
        ConfigLoader.load("config.json", Config.class);

        EmailMonitorService emailMonitor = new EmailMonitorService();
        MessageService messageService = new MessageService(client);
        UpdatesService updatesService = new UpdatesService(client, mapper, db, messageService);
        ContragentFactory contragentFactory = new ContragentFactory(messageService,db,emailMonitor);
        RequestService requestService = new RequestService(contragentFactory,client,mapper);
        CallbackProcessor callbackProcessor = new CallbackProcessor(messageService, db, usersSessions,requestService);
        UpdateProcessor updateProcessor = new UpdateProcessor(messageService, callbackProcessor, usersSessions, db);
        FileMonitoringProcessor fileMonitoringProcessor = new FileMonitoringProcessor(db, mapper, updateProcessor);

        FindReqProcessor findReqProcessor = new FindReqProcessor(contragentFactory,client,mapper);
        EmailMonitoringProcessor emailMonitoringProcessor = new EmailMonitoringProcessor(emailMonitor,contragentFactory);
        // Создаём трей и иконку


        Thread monitoringThread = new Thread(findReqProcessor);
        Thread emailThread = new Thread(emailMonitoringProcessor);

        monitoringThread.start();
        emailThread.start();
        // Запускаем бесконечный long-poll
        setupSystemTray(findReqProcessor);
        while (running) {
            try {


                List<Update> updates = updatesService.getUpdate();
                if (updates != null && !updates.isEmpty()) {
                    for (Update update : updates) {
                        long userId = update.getMessage().getSender().getUserId();
                        long chatId = update.getMessage().getRecipient().getChatId();
                        if(update.getCallBack() !=null){
                            userId = update.getCallBack().getUser().getUserId();
                        }
                        String password = update.getMessage().getBody().getText();
                        if(password != null && password.equals(Const.BOT_PASSWORD)) {
                            db.addUser(userId,chatId);
                        }
                        if (db.checkUserPermissions(userId)) {
                            updateProcessor.startProcess(update);

                        }else messageService.sendSimpleMessage(chatId,"Вы не авторизованы, запросите пароль у руководителя",null);

                        // Небольшая пауза, чтобы не перегружать CPU
                        Thread.sleep(Const.UPDATE_BOT_DELAY_TIME);
                    }
                }
                } catch(Exception e){
                    e.printStackTrace();
                }

            }
        }

        private static void setupSystemTray (FindReqProcessor findReqProcessor){
            SystemTray tray = SystemTray.getSystemTray();

            // Загружаем иконку (положите файл icon.png в resources или рядом с jar)
            Image image;
            try {
                image = ImageIO.read(Objects.requireNonNull(Main.class.getResource("/tray.png")));
            } catch (Exception e) {
                image = createDefaultIcon(); // создаём простую иконку, если файл не найден
            }

            // Создаём TrayIcon с подсказкой
            trayIcon = new TrayIcon(image, "Бот MAX");

            // Настройка иконки (масштабирование, прозрачность)
            trayIcon.setImageAutoSize(true);

            // Создаём меню трея
            PopupMenu popup = new PopupMenu();


            MenuItem exitItem = new MenuItem("Выход");
            exitItem.addActionListener(e -> {
                findReqProcessor.shutdown();
                running = false;
                try {
                    Thread.sleep(Const.CHECK_NEW_REQUEST_TIME_DELAY + 500);
                } catch (InterruptedException ex) {
                    throw new RuntimeException(ex);
                }
                System.exit(0);
            });
            popup.add(exitItem);

            trayIcon.setPopupMenu(popup);

            // Добавляем иконку в трей
            try {
                tray.add(trayIcon);
            } catch (AWTException e) {
                System.err.println("Ошибка добавления иконки в трей: " + e);
            }

            // Сообщение о запуске
            trayIcon.displayMessage("Бот запущен", "Работает в фоне", TrayIcon.MessageType.INFO);
        }

        // Создаёт простую иконку по умолчанию (если файл не найден)
        private static Image createDefaultIcon () {
            BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
            Graphics g = img.getGraphics();
            g.setColor(Color.GREEN);
            g.fillRect(0, 0, 16, 16);
            g.dispose();
            return img;
        }


    }