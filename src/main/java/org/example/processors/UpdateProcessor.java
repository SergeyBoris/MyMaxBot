package org.example.processors;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.constants.Const;
import org.example.entity.Attachment;
import org.example.entity.Message;
import org.example.entity.Update;
import org.example.entity.User;
import org.example.spring.repo.UserRepo;
import org.example.services.BotRequestService;
import org.example.services.MessageService;
import org.example.util.UserUploadSession;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class UpdateProcessor {
    private final MessageService messageService;
    private final CallbackProcessor callbackProcessor;
    private final Map<Long, UserUploadSession> usersSessions;
    private final BotRequestService botRequestService;
    private final UserRepo userRepo;



    public void startProcess(Update update) throws IOException {
        System.out.println(update);
        long senderId;
        if(update.getCallBack() != null) {
            senderId = update.getCallBack().getUser().getUserBotId();
        }else {
            senderId = update.getMessage().getSender().getUserBotId();
        }


        if(update.getMessage().getBody().getText().equals(Const.BOT_PASSWORD)){
            User user = update.getMessage().getSender();
            long chatId = update.getMessage().getRecipient().getChatId();
            user.setChatId(chatId);

            userRepo.save(user);
        }
        long chatId = update.getMessage().getRecipient().getChatId();
        // 3. Проверка авторизации
        Set<Long> allUserBotIds = userRepo.getAllUserBotIds();
        if (allUserBotIds != null && allUserBotIds.contains(senderId)) {
            // ✅ авторизован — обрабатываем
            continueProcess(update, senderId, chatId);
        } else {
            // ❌ не авторизован
            log.info("Пользователь {} не авторизован", senderId);
             messageService.sendSimpleMessage(chatId, "Вы не авторизованы...", null);
        }


    }

    private void continueProcess(Update update, Long senderId, long chatId) {
        boolean hasPhoto = false;

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
            System.out.println("Новое сообщение от " + senderId + ": " + text);
            if (usersSessions.containsKey(senderId)) {
                UserUploadSession userUploadSession = usersSessions.get(senderId);
                String nextMessageToUser = userUploadSession.getNextMessageToUser();
                if (nextMessageToUser != null && !nextMessageToUser.isEmpty()) {
                    userUploadSession.setText(text);
                    botRequestService.specialContragentAction(userUploadSession.getContragent(), nextMessageToUser, userUploadSession);
                    messageService.sendSimpleMessage(chatId, "текст приложен\n" + userUploadSession.getNextMessageToUser(), userUploadSession.getNextKeyBoard());
                } else {
                    messageService.sendSimpleMessage(chatId, "текст приложен \n для отправки нажмите \"готово\"", Const.KEYBOARD_END_PHOTO);
                }

            } else {
                messageService.sendSimpleMessage(chatId, "МЕНЮ", Const.KEYBOARD_ALL_REQ);
            }


        } else if (update.getCallBack() != null && !hasPhoto) {
            String text = message.getBody().getText();
//            usersSessions.computeIfPresent(senderId, (key, session) -> {
//                session.setText(text);
//                session.setChanged(true);
//                return session;
//            });
            System.out.println("Новое сообщение c колбэк от " + senderId + ": " + update.getCallBack().getPayload());
            callbackProcessor.process(update);

        } else if (hasPhoto) {
            String text = message.getBody().getText();
            System.out.println("Новое сообщение c фото " + senderId + ": ");
            usersSessions.computeIfPresent(senderId, (key, session) -> {
                if (session.getText() == null) {
                    session.setText(text);
                } else {
                    String oldText = session.getText();
                    session.setText(oldText + text);
                    session.setChanged(true);
                }
                return session;
            });
            callbackProcessor.photoProcess(update);
        }
    }


}
