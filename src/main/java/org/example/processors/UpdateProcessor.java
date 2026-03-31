package org.example.processors;

import org.example.constants.Const;
import org.example.db.MapDB;
import org.example.entity.Attachment;
import org.example.entity.Message;
import org.example.entity.Req;
import org.example.entity.Update;
import org.example.services.MessageService;
import org.example.util.UserSessionUtil;
import org.example.util.UserUploadSession;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class UpdateProcessor {
    private final MessageService messageService;
    private final CallbackProcessor callbackProcessor;
    private Map<Long, UserUploadSession> usersSessions;
    private MapDB db;

    public UpdateProcessor(MessageService messageService, CallbackProcessor callbackProcessor, Map<Long, UserUploadSession> usersSessions,MapDB db) {
        this.messageService = messageService;
        this.callbackProcessor = callbackProcessor;
        this.usersSessions = usersSessions;
        this.db = db;

    }
    public void startProcess(Update update) throws IOException {
        System.out.println(update);
        boolean hasPhoto = false;
        long chatId = update.getMessage().getRecipient().getChatId();
        Message message = update.getMessage();
        if(update.getMessage().getBody().getAttachments() != null){
            for (Attachment attachment : update.getMessage().getBody().getAttachments()) {
                if (attachment.getType().equals("image")) {
                    hasPhoto = true;
                    break;
                }
            }
        }
        System.out.println(hasPhoto);
        if(update.getCallBack() == null && !hasPhoto) {

            String text = message.getBody().getText();
            long senderId = message.getSender().getUserId();
            System.out.println("Новое сообщение от " + senderId + ": " + text);
            if(usersSessions.containsKey(senderId)){


            UserSessionUtil.addText(senderId, text);
            messageService.sendSimpleMessage(chatId, "текст приложен \n для отправки нажмите \"готово\"", Const.KEYBOARD_END_PHOTO);
            usersSessions.get(senderId).setChanged(false);
            }else {
                messageService.sendSimpleMessage(chatId,"МЕНЮ", Const.KEYBOARD_ALL_REQ);
            }


        }else if(update.getCallBack() != null && !hasPhoto) {
            String text = message.getBody().getText();
            long senderId = update.getCallBack().getUser().getUserId();
//            usersSessions.computeIfPresent(senderId, (key, session) -> {
//                session.setText(text);
//                session.setChanged(true);
//                return session;
//            });
            System.out.println("Новое сообщение c колбэк от " + senderId + ": " + update.getCallBack().getPayload());
            callbackProcessor.process(update);

        }else if(hasPhoto){
            String text = message.getBody().getText();
            long senderId = message.getSender().getUserId();
            System.out.println("Новое сообщение c фото " + senderId + ": ");
            usersSessions.computeIfPresent(senderId, (key, session) -> {
                String oldText = session.getText();
                session.setText(oldText + text);
                session.setChanged(true);
                return session;
            });
            callbackProcessor.photoProcess(update);
        }

    }



    public void sendMessageNewRequest(Req req) {
        if (req.getToUsersId() == null || req.getToUsersId().isEmpty()) {
            Set<Long> allUsers = db.getAllUsers();
            for (Long userId : allUsers) {
                String text = req.getRequestNumber() + "\n"
                        + req.getRequestAddress() + "\n"
                        + req.getRequestText();
                messageService.sendSimpleMessage(db.getUserChatId(userId), text, Const.KEYBOARD_ATTACHMENT_TO_ALL_REQ);
            }
        }else {
            List<Long> toUsersId = req.getToUsersId();
            for (Long userId : toUsersId) {
                String text = req.getRequestNumber() + "\n" +
                        req.getRequestAddress() + "\n" +
                        req.getRequestText();
                if (db.getUserChatId(userId)== null){
                    System.out.println("Пользователь не найден в базе ИД = " + userId);
                }else {
                    messageService.sendSimpleMessage(db.getUserChatId(userId), text, Const.KEYBOARD_ATTACHMENT_TO_ALL_REQ);
                }
            }


        }
    }
}
