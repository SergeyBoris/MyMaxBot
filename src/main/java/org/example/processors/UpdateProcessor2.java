package org.example.processors;

import org.example.constants.Const;
import org.example.db.MapDB;
import org.example.entity.Attachment;
import org.example.entity.Req;
import org.example.entity.Update;
import org.example.services.MessageService;
import org.example.services.SaveFileService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class UpdateProcessor2 {

    private final MessageService messageService;
    private final MapDB db;

    public UpdateProcessor2(MessageService messageService, MapDB db) {
        this.messageService = messageService;
        this.db = db;
    }

    public void startProcess(Update update) throws IOException {
        long chatId = update.getMessage().getRecipient().getChatId();
        if (db.checkUserPermissions(update.getMessage().getSender().getUserId())) {
            if (update.getMessage().getLink() != null) {
                String[] split = update.getMessage().getLink().getOriginalMessage().getText().split("\n");
                String reqNumber = split[0];
                String status = ""; // todo это заглушка
                if (db.getAllRequests().containsKey(reqNumber)) {
                    String text = update.getMessage().getBody().getText();
                    List<Attachment> attachmentsList = update.getMessage().getBody().getAttachments();
                    List<String> photoUrls = new ArrayList<>();
                    if (attachmentsList != null && !attachmentsList.isEmpty()) {
                        for (Attachment attachment : attachmentsList) {
                            photoUrls.add(attachment.getPayload().getUrl());
                        }
                    }
                    SaveFileService.saveFile(update.getMessage().getSender(), reqNumber, text, photoUrls,status);
                    db.removeRequest(reqNumber);
                    messageService.sendSimpleMessage(chatId, "Ответ отправлен", null);
                    messageService.deleteMessage(update.getMessage().getLink().getOriginalMessage().getMid());
                } else {
                    messageService.sendSimpleMessage(chatId, "Заявка в базе не найдена", null);
                }
            } else {
                messageService.sendSimpleMessage(chatId, "Вы не выбрали ни одной заявки для закрытия", null);
            }
        } else {
            if (update.getMessage().getBody().getText().equals(Const.BOT_PASSWORD)) {
                messageService.sendSimpleMessage(chatId, "Вы успешно авторизованы!", null);
                db.addUser(update.getMessage().getSender().getUserId(), update.getMessage().getRecipient().getChatId());
                Map<String, Req> allRequests = db.getAllRequests();

                for (Map.Entry<String, Req> requestEntry : allRequests.entrySet()) {
                    String text = requestEntry.getKey() + "\n"
                            + requestEntry.getValue().getRequestAddress() + "\n"
                            + requestEntry.getValue().getRequestText();
                    messageService.sendSimpleMessage(chatId, text, null);

                }
            } else {
                messageService.sendSimpleMessage(chatId, "Вы не авторизованы, запросите пароль у руководства", null);
            }

        }
    }

    public void sendMessageNewRequest(Req req) {
        Set<Long> allUsers = db.getAllUsers();
        for (Long userId : allUsers) {
            String text = req.getRequestNumber() + "\n"
                    + req.getRequestAddress() + "\n"
                    + req.getRequestText();
            messageService.sendSimpleMessage(db.getUserChatId(userId), text, null);
        }
    }

}
