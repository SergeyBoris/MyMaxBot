package org.example.processors;

import org.example.constants.Const;
import org.example.db.MapDB;
import org.example.entity.Attachment;
import org.example.entity.Req;
import org.example.entity.Update;
import org.example.services.MessageService;
import org.example.services.RequestService;
import org.example.services.SaveFileService;
import org.example.util.UserUploadSession;

import java.util.List;
import java.util.Map;

public class CallbackProcessor {
    private final MessageService messageService;
    private final MapDB db;
    private final RequestService requestService;
    private final Map<Long, UserUploadSession> usersSessions;

    public CallbackProcessor(MessageService messageService, MapDB db, Map<Long, UserUploadSession> usersSessions, RequestService requestService) {
        this.messageService = messageService;
        this.db = db;
        this.usersSessions = usersSessions;
        this.requestService = requestService;

    }

    public void process(Update update) {
        long senderUserId = update.getCallBack().getUser().getUserId();
        long senderChatId = update.getMessage().getRecipient().getChatId();


        if (update.getCallBack() != null) {
            String payload = update.getCallBack().getPayload();

            if (payload.equals("all_requests_alfa")) {
                List<Req> allReq = requestService.getAllRequests("PBF");
                if (allReq != null && !allReq.isEmpty()) {
                    for (Req req : allReq) {
                        messageService.sendSimpleMessage(senderChatId, req.toString(), Const.KEYBOARD_ATTACHMENT_TO_ALL_REQ);
                    }

//                Map<String, Req> allRequests = db.getAllRequests();
//
//                if (!allRequests.isEmpty()) {
//                    for (Map.Entry<String, Req> entry : allRequests.entrySet()) {
//                        if (entry.getValue().getToUsersId() != null && !entry.getValue().getToUsersId().isEmpty()) {
//                            for (Long toUsersId : entry.getValue().getToUsersId()) {
//                                if (senderUserId == toUsersId) {
//                                    String text = entry.getKey() + "\n" +
//                                            entry.getValue().getRequestAddress() + "\n" +
//                                            entry.getValue().getRequestText();
//                                    messageService.sendSimpleMessage(senderChatId, text, Const.KEYBOARD_ATTACHMENT_TO_ALL_REQ);
//                                }
//                            }
//                        }else {
//                            String text = entry.getKey() + "\n" +
//                                    entry.getValue().getRequestAddress() + "\n" +
//                                    entry.getValue().getRequestText();
//                            messageService.sendSimpleMessage(senderChatId, text, Const.KEYBOARD_ATTACHMENT_TO_ALL_REQ);
//                        }
//
//                    }
                } else
                    messageService.sendSimpleMessage(senderChatId, "нет заявок", Const.KEYBOARD_ALL_REQ);

            } else if (payload.equals("close_request") || payload.equals("localized")) {
                long userId = update.getCallBack().getUser().getUserId();
                String[] split = update.getMessage().getBody().getText().split("\n");
                String reqNumber = split[0];
                usersSessions.put(userId, new UserUploadSession(reqNumber));


                if (payload.equals("close_request")) {
                    usersSessions.get(userId).setStatus("Закрыто");
//                    String text = "Статус: Закрыто\n";
//                    UserSessionUtil.addText(userId,text);

                } else if (payload.equals("localized")) {
                    usersSessions.get(userId).setStatus("Локализовано");
//                    String text = "Статус: Локализовано\n";
//                    UserSessionUtil.addText(userId,text);

                }
                messageService.sendSimpleMessage(update.getMessage().getRecipient().getChatId(), "приложите фото и нажмите \"готово\"", Const.KEYBOARD_END_PHOTO);

            } else if (payload.equals("end_photo")) {
                if (usersSessions != null && usersSessions.containsKey(update.getCallBack().getUser().getUserId())) {

                        long userId = update.getCallBack().getUser().getUserId();
                        UserUploadSession userUploadSession = usersSessions.get(userId);
                        String text = usersSessions.get(userId).getText();
                        String status = usersSessions.get(userId).getStatus();

                        System.out.println(userId);
                        List<String> photoUrls = userUploadSession.getPhotoUrls();
                        String requestNumber = userUploadSession.getRequestNumber();
                        if (photoUrls != null) {
                            SaveFileService.saveFile(update.getCallBack().getUser(), requestNumber, text, photoUrls, status);
                            db.removeRequest(requestNumber);
                        }
                        usersSessions.remove(userId);
                        messageService.sendSimpleMessage(update.getMessage().getRecipient().getChatId(), "Заявка закрыта", Const.KEYBOARD_ALL_REQ);

                } else {
                    messageService.sendSimpleMessage(update.getMessage().getRecipient().getChatId(), "МЕНЮ", Const.KEYBOARD_ALL_REQ);
                }
            } else if (payload.equals("all_requests_hendz")) {
                List<Req> allReq = requestService.getAllRequests("HENDZ");
                if (allReq != null && !allReq.isEmpty()) {
                    for (Req req : allReq) {
                        messageService.sendSimpleMessage(senderChatId, req.toString(), Const.KEYBOARD_ALL_REQ);
                    }
                }else {
                    messageService.sendSimpleMessage(senderChatId,"нет заявок", Const.KEYBOARD_ALL_REQ);
                }

            } else if (payload.equals("cancel_work_with_req")) {
                usersSessions.remove(update.getCallBack().getUser().getUserId());
                messageService.sendSimpleMessage(update.getMessage().getRecipient().getChatId(), "Работа с заявкой прекращена", Const.KEYBOARD_ALL_REQ);
            } else {
                System.out.println("какимто чудом узерсессия нулевая");
            }


        }


    }

    public void photoProcess(Update update) {

        long userId = update.getMessage().getSender().getUserId();
        long chatId = update.getMessage().getRecipient().getChatId();
        System.out.println("Сессия содержитт ключ " + userId + usersSessions.containsKey(update.getMessage().getSender().getUserId()));
        if (usersSessions.containsKey(userId)) {
            for (Attachment attachment : update.getMessage().getBody().getAttachments()) {
                String photoUrl = attachment.getPayload().getUrl();
                usersSessions.get(userId).setPhotoUrls(photoUrl);
            }
            int countPhotoInSession = usersSessions.get(userId).getPhotoUrls().size();
            messageService.sendSimpleMessage(chatId, "Приложено " + countPhotoInSession + " фото \n для отправки нажмите \"готово\"", Const.KEYBOARD_END_PHOTO);
        } else {
            messageService.sendSimpleMessage(update.getMessage().getRecipient().getChatId(), "Выберете заявку сначала", null);
        }
    }

    public void textProcess(Update update) {
        long userId = update.getMessage().getSender().getUserId();
        long chatId = update.getMessage().getRecipient().getChatId();
        System.out.println("Сессия содержитт ключ " + userId + usersSessions.containsKey(update.getMessage().getSender().getUserId()));
        if (usersSessions.containsKey(userId)) {

        }
    }

}
