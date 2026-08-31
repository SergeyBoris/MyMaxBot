package org.example.processors;

import lombok.extern.slf4j.Slf4j;
import org.example.constants.Const;
import org.example.db.MapDB;
import org.example.entity.Attachment;
import org.example.entity.Contragents.Contragent;
import org.example.entity.Contragents.Pfi;
import org.example.entity.InlineKeyboard;
import org.example.entity.Req;
import org.example.entity.Update;
import org.example.services.ContragentFactory;
import org.example.services.MessageService;
import org.example.services.RequestService;
import org.example.services.SaveFileService;
import org.example.util.UserUploadSession;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
@Slf4j
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
            long userId = update.getCallBack().getUser().getUserId();
            switch (payload) {
                case "all_requests_alfa" -> {
                    List<Req> allReq = requestService.getAllRequests("PBF");
                    if (allReq != null && !allReq.isEmpty()) {
                        for (Req req : allReq) {
                            messageService.sendSimpleMessage(senderChatId, req.toString(), Const.KEYBOARD_ATTACHMENT_TO_ALL_REQ);
                        }

                    } else
                        messageService.sendSimpleMessage(senderChatId, "нет заявок", Const.KEYBOARD_ALL_REQ);

                }
                case "close_request", "localized" -> {

                    String[] split = update.getMessage().getBody().getText().split("\n");
                    String reqNumber = split[0];
                    String contragent = split[1];
                    log.info("button close_request ALFA (localized) {}\n {}", reqNumber, LocalDateTime.now());
                    System.out.println("КОНТРАГЕНТ : " + contragent);
                    usersSessions.put(userId, new UserUploadSession(reqNumber, contragent));


                    if (payload.equals("close_request")) {
                        log.info("button close_request (Закрыто) {}\n {}", reqNumber, LocalDateTime.now());
                        usersSessions.get(userId).setStatus("Закрыто");
                        requestService.specialContragentAction(contragent, "Закрыто", usersSessions.get(userId));
//                    String text = "Статус: Закрыто\n";
//                    UserSessionUtil.addText(userId,text);

                    } else {
                        usersSessions.get(userId).setStatus("Локализовано");
                        requestService.specialContragentAction(contragent, "Локализовано", usersSessions.get(userId));
//                    String text = "Статус: Локализовано\n";
//                    UserSessionUtil.addText(userId,text);

                    }

                    extractNextMessageToUser(update, userId);
                }

                case "end_photo" -> {
                    if (usersSessions != null && usersSessions.containsKey(update.getCallBack().getUser().getUserId())) {
                        messageService.sendSimpleMessage(update.getMessage().getRecipient().getChatId(), "ОБРАБОТКА...",null);

                        UserUploadSession userUploadSession = usersSessions.get(userId);
                        String text = usersSessions.get(userId).getText();
                        String status = usersSessions.get(userId).getStatus();
                        String contragent = usersSessions.get(userId).getContragent();
                        System.out.println(userId);
                        List<String> photoUrls = userUploadSession.getPhotoUrls();
                        String requestNumber = userUploadSession.getRequestNumber();
                        log.info("button end_photo for req {}",requestNumber);
                        if (photoUrls != null) {
                            userUploadSession.setSavedJpgPaths(SaveFileService.saveFile(update.getCallBack().getUser(), requestNumber, text, photoUrls, status));
                        }

                        boolean successClosed = requestService.closeRequest(userUploadSession);
                        if (successClosed) {
                            log.info("button end_photo (success response) {}\n {}", requestNumber, LocalDateTime.now());

                            messageService.sendSimpleMessage(update.getMessage().getRecipient().getChatId(), "Заявка закрыта", Const.KEYBOARD_ALL_REQ);
                        } else {
                            log.info("button end_photo (NO success response){}\n {}", requestNumber, LocalDateTime.now());
                            messageService.sendSimpleMessage(update.getMessage().getRecipient().getChatId(), "Ошибка закрытия заявки", Const.KEYBOARD_ALL_REQ);
                        }
                        log.info("count of photos {}", photoUrls != null ? photoUrls.size() : 0);
                        usersSessions.remove(userId);

                    } else {
                        messageService.sendSimpleMessage(update.getMessage().getRecipient().getChatId(), "МЕНЮ", Const.KEYBOARD_ALL_REQ);
                    }
                }
                case "all_requests_hendz" -> {
                    messageService.sendSimpleMessage(update.getMessage().getRecipient().getChatId(), "ОБРАБОТКА...",null);
                    List<Req> allReq = requestService.getAllRequests("HENDZ");
                    if (allReq != null && !allReq.isEmpty()) {
                        for (Req req : allReq) {
                            messageService.sendSimpleMessage(senderChatId, req.toString(), Const.KEYBOARD_ALL_REQ);
                        }
                    } else {
                        messageService.sendSimpleMessage(senderChatId, "нет заявок", Const.KEYBOARD_ALL_REQ);
                    }

                }
                case "cancel_work_with_req" -> {
                    UserUploadSession userUploadSession = usersSessions.get(userId);
                    log.info("cancel_work_with_req {}",  userUploadSession.getRequestNumber() == null? null : userUploadSession.getRequestNumber());
                    usersSessions.remove(update.getCallBack().getUser().getUserId());
                    messageService.sendSimpleMessage(update.getMessage().getRecipient().getChatId(), "Работа с заявкой прекращена", Const.KEYBOARD_ALL_REQ);
                }
                case "all_requests_pfi" -> {
                    messageService.sendSimpleMessage(update.getMessage().getRecipient().getChatId(), "ОБРАБОТКА...",null);
                    List<Req> pfi = requestService.getAllRequests("PFI");
                    if (pfi != null && !pfi.isEmpty()) {
                        for (Req req : pfi) {
                            messageService.sendSimpleMessage(senderChatId, req.toString(), Const.KEYBOARD_ATTACHMENT_TO_ALL_REQ_PFI);
                        }
                    } else {
                        messageService.sendSimpleMessage(senderChatId, "нет заявок", Const.KEYBOARD_ALL_REQ);

                    }
                }
                case "p_n_r" -> {

                    String[] split = update.getMessage().getBody().getText().split("\n");
                    String reqNumber = split[0];
                    String contragent = split[1];
                    log.info("button P_N_R reqNum = {}\n {}", reqNumber, LocalDateTime.now());
                    usersSessions.put(userId, new UserUploadSession(reqNumber, contragent));
                    messageService.sendSimpleMessage(senderChatId,"Выберите статус заявки",Const.KEYBOARD_ATTACHMENT_TO_PFI_PNR); // следующее сообщение
                }
                case "p_n_r_worked" -> {
                    UserUploadSession userUploadSession = usersSessions.get(userId);
                    String reqNumber = userUploadSession.getRequestNumber();
                    log.info("button P_N_R reqNum = {}\n {}", reqNumber, LocalDateTime.now());
                    requestService.specialContragentAction("PFI", "p_n_r_worked", usersSessions.get(userId));

                    extractNextMessageToUser(update, userId);
                } case "p_n_r_not_allowed" -> {
                    UserUploadSession userUploadSession = usersSessions.get(userId);
                    String reqNumber = userUploadSession.getRequestNumber();
                    log.info("button P_N_R reqNum = {}\n {}", reqNumber, LocalDateTime.now());
                    requestService.specialContragentAction("PFI", "p_n_r_not_allowed", usersSessions.get(userId));

                    extractNextMessageToUser(update, userId);
                }

                    default -> System.out.println("какимто чудом узерсессия нулевая");
                }


            }


        }

    private void extractNextMessageToUser(Update update, long userId) {
        String nextMessageToUser = usersSessions.get(userId).getNextMessageToUser();
        InlineKeyboard nextKeyBoard = usersSessions.get(userId).getNextKeyBoard();
        if (nextMessageToUser != null && !nextMessageToUser.isEmpty() && nextKeyBoard != null) {
            messageService.sendSimpleMessage(update.getMessage().getRecipient().getChatId(), nextMessageToUser, nextKeyBoard);
        } else {
            messageService.sendSimpleMessage(update.getMessage().getRecipient().getChatId(), nextMessageToUser, nextKeyBoard); // todo разобраться почему тожесамое
        }
    }

    public void photoProcess (Update update){

            long userId = update.getMessage().getSender().getUserId();
            long chatId = update.getMessage().getRecipient().getChatId();
            System.out.println("Сессия содержитт ключ " + userId + usersSessions.containsKey(update.getMessage().getSender().getUserId()));
            if (usersSessions.containsKey(userId)) {

                List<Attachment> attachments = update.getMessage().getBody().getAttachments();
                if (update.getMessage().getLink() != null) {
                    attachments = update.getMessage().getLink().getOriginalMessage().getAttachments();
                }

                for (Attachment attachment : attachments) {
                    String photoUrl = attachment.getPayload().getUrl();
                    usersSessions.get(userId).setPhotoUrls(photoUrl);
                }
                int countPhotoInSession = usersSessions.get(userId).getPhotoUrls().size();
                messageService.sendSimpleMessage(chatId, "Приложено " + countPhotoInSession + " фото \n для отправки нажмите \"готово\"", Const.KEYBOARD_END_PHOTO);
            } else {
                messageService.sendSimpleMessage(update.getMessage().getRecipient().getChatId(), "Выберете заявку сначала", null);
            }
        }

        public void textProcess (Update update){
            long userId = update.getMessage().getSender().getUserId();
            long chatId = update.getMessage().getRecipient().getChatId();
            System.out.println("Сессия содержитт ключ " + userId + usersSessions.containsKey(update.getMessage().getSender().getUserId()));
            if (usersSessions.containsKey(userId)) {

            }
        }

    }
