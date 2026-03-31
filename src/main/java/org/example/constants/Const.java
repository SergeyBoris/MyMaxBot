package org.example.constants;

import org.example.entity.Button;
import org.example.entity.InlineKeyboard;
import org.example.entity.KeyBoard;
import org.example.util.Config;
import org.example.util.ConfigLoader;

import java.util.List;

public class Const {

    public static final long UPDATE_BOT_DELAY_TIME;
    public static final long CHECK_NEW_REQUEST_TIME_DELAY;
    public static final String ANSWER_FILE_NAME_PREFIX;
    public static final String NEW_REQUEST_FILE_NAME;
    public static final String DB_FILE_NAME;
    public static final String ANSWER_ATTACHMENT_FILE_NAME_PREFIX;
    public static final String BOT_PASSWORD;
    public static final String DB_FILE = "bot_data.db"; //todo delete
    public static final String BOT_URL;
    public static final String BOT_TOKEN;
    public static String MAIL_DEBUG;
    public static String MAIL_IMAP_HOST;
    public static int MAIL_IMAP_PORT;
    public static String MAIL_IMAPS_SSL_ENABLE;
    public static String MAIL_IMAPS_SSL_PROTOCOLS;
    public static long MAIL_IMAP_TIMEOUT;
    public static long MAIL_IMAP_CONNECTION_TIMEOUT;
    public static String MAIL_IMAP_AUTH;
    public static String MAIL_FOLDER_TO_SCAN;
    public static String MAIL_USER_NAME;
    public static String MAIL_PASSWORD;
    public static final Button BTN_ALL_REQ_ALFA = new Button("callback","Альфа","all_requests_alfa");
    public static final Button BTN_ALL_REQ_HENDZ = new Button("callback","Хендз","all_requests_hendz");
    public static final Button BTN_CLOSE_REQ = new Button("callback","Закрыть заявку","close_request");
    public static final Button BTN_LOCALIZED = new Button("callback", "Локализовано", "localized");
    public static final Button BTN_SEND_ALL_ATTACHMENTS = new Button("callback","готово","end_photo");
    public static final Button BTN_CANCEL = new Button("callback" , "Отмена", "cancel_work_with_req");
    public static final InlineKeyboard KEYBOARD_ALL_REQ = new InlineKeyboard("inline_keyboard", new KeyBoard(List.of(BTN_ALL_REQ_ALFA,BTN_ALL_REQ_HENDZ)));
    public static final InlineKeyboard KEYBOARD_ATTACHMENT_TO_ALL_REQ = new InlineKeyboard("inline_keyboard", new KeyBoard(List.of(BTN_CLOSE_REQ,BTN_LOCALIZED)));
    public static final InlineKeyboard KEYBOARD_END_PHOTO = new InlineKeyboard("inline_keyboard", new KeyBoard(List.of(BTN_SEND_ALL_ATTACHMENTS,BTN_CANCEL)));
    public static final InlineKeyboard KEYBOARD_CANCEL = new InlineKeyboard("inline_keyboard", new KeyBoard(List.of(BTN_CANCEL)));
    static {

        try {
            Config config =  ConfigLoader.load("config.json", Config.class);

            UPDATE_BOT_DELAY_TIME = config.getUpdateDelayTime();
            CHECK_NEW_REQUEST_TIME_DELAY = config.getCheckNewRequestTimeDelay();
            NEW_REQUEST_FILE_NAME = config.getNewRequestFileName();
            ANSWER_FILE_NAME_PREFIX = config.getAnswerFileNamePrefix();
            ANSWER_ATTACHMENT_FILE_NAME_PREFIX = config.getAnswerAttachmentFileNamePrefix();
            DB_FILE_NAME = config.getDbFileName();
            BOT_PASSWORD = config.getBotPassword();
            BOT_URL = config.getBotUrl();
            BOT_TOKEN = config.getBotToken();
            MAIL_DEBUG = config.getMailDebug();
            MAIL_IMAP_HOST = config.getMailImapHost();
            MAIL_IMAP_PORT = config.getMailImapPort();
            MAIL_IMAPS_SSL_ENABLE = config.getMailImapsSslEnable();
            MAIL_IMAPS_SSL_PROTOCOLS = config.getMailImapsSslProtocols();
            MAIL_IMAP_TIMEOUT = config.getMailImapTimeout();
            MAIL_IMAP_CONNECTION_TIMEOUT = config.getMailImapConnectionTimeout();
            MAIL_IMAP_AUTH = config.getMailImapAuth();
            MAIL_FOLDER_TO_SCAN = config.getMailFolderToScan();
            MAIL_USER_NAME = config.getMailUserName();
            MAIL_PASSWORD = config.getMailPassword();


        } catch (Exception e) {
            throw new RuntimeException("Ошибка при инициализации констант: " + e.getMessage(), e);
        }
    }
}
