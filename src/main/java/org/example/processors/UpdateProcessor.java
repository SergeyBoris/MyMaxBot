package org.example.processors;

import org.example.constants.Const;
import org.example.constants.ConstPfi;
import org.example.db.MapDB;
import org.example.entity.Attachment;
import org.example.entity.Message;
import org.example.entity.Req;
import org.example.entity.Update;
import org.example.services.MessageService;
import org.example.services.RequestService;
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
    private final RequestService requestService;

    public UpdateProcessor(MessageService messageService, CallbackProcessor callbackProcessor, Map<Long, UserUploadSession> usersSessions, MapDB db, RequestService requestService) {
        this.messageService = messageService;
        this.callbackProcessor = callbackProcessor;
        this.usersSessions = usersSessions;
        this.db = db;
        this.requestService = requestService;

    }

    public void startProcess(Update update) throws IOException {
        System.out.println(update);
        boolean hasPhoto = false;
        long chatId = update.getMessage().getRecipient().getChatId();
        Message message = update.getMessage();
        if (update.getMessage().getBody().getAttachments() != null || update.getMessage().getLink() != null) {
            List<Attachment> attachments = update.getMessage().getBody().getAttachments();
            if (update.getMessage().getLink() != null) {
                attachments = update.getMessage().getLink().getOriginalMessage().getAttachments();
            }

            for (Attachment attachment : attachments) {
                if (attachment.getType().equals("image")) {
                    hasPhoto = true;
                    break;
                }
            }
        }
        System.out.println(hasPhoto);
        if (update.getCallBack() == null && !hasPhoto) {

            String text = message.getBody().getText();
            long senderId = message.getSender().getUserId();
            System.out.println("Новое сообщение от " + senderId + ": " + text);
            if (usersSessions.containsKey(senderId)) {
                UserUploadSession userUploadSession = usersSessions.get(senderId);
                    String nextMessageToUser = userUploadSession.getNextMessageToUser();
                    if(nextMessageToUser !=null && !nextMessageToUser.isEmpty()) {
                        userUploadSession.setText(text);
                        requestService.specialContragentAction(userUploadSession.getContragent(),nextMessageToUser,userUploadSession);
                        messageService.sendSimpleMessage(chatId, "текст приложен\n" + userUploadSession.getNextMessageToUser(), userUploadSession.getNextKeyBoard());
                    }else {
                        messageService.sendSimpleMessage(chatId, "текст приложен \n для отправки нажмите \"готово\"", Const.KEYBOARD_END_PHOTO);
                    }

            } else {
                messageService.sendSimpleMessage(chatId, "МЕНЮ", Const.KEYBOARD_ALL_REQ);
            }


        } else if (update.getCallBack() != null && !hasPhoto) {
            String text = message.getBody().getText();
            long senderId = update.getCallBack().getUser().getUserId();
//            usersSessions.computeIfPresent(senderId, (key, session) -> {
//                session.setText(text);
//                session.setChanged(true);
//                return session;
//            });
            System.out.println("Новое сообщение c колбэк от " + senderId + ": " + update.getCallBack().getPayload());
            callbackProcessor.process(update);

        } else if (hasPhoto) {
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
        } else {
            List<Long> toUsersId = req.getToUsersId();
            for (Long userId : toUsersId) {
                String text = req.getRequestNumber() + "\n" +
                        req.getRequestAddress() + "\n" +
                        req.getRequestText();
                if (db.getUserChatId(userId) == null) {
                    System.out.println("Пользователь не найден в базе ИД = " + userId);
                } else {
                    messageService.sendSimpleMessage(db.getUserChatId(userId), text, Const.KEYBOARD_ATTACHMENT_TO_ALL_REQ);
                }
            }


        }
    }
    private String userUploadSessionHasSomeUndefinedParam(UserUploadSession userUploadSession){
        Map<String, Object> params = userUploadSession.getParams();
        if (params == null) return "";

        for (Map.Entry<String, Object> entry : params.entrySet()) {
            if (entry.getValue().equals(ConstPfi.NEW_PARAM_INSTANCE)) {
                return entry.getKey();
            }
        }
        return "";

    }
}
