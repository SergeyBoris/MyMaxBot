package org.example.util;

import static org.example.Main.usersSessions;

public class UserSessionUtil {

    public static void addText(long senderId, String text) {
        usersSessions.compute(senderId, (key, session) -> {
            if (session == null) {
                // Сессии нет — создаём новую
                session = new UserUploadSession(); // замените на ваш конструктор
            }

            // Получаем текущий текст (если null — заменяем на пустую строку)
            String currentText = (session.getText() != null) ? session.getText() : "";
            String newText = "";
            // Объединяем с новым текстом
            if (currentText.isEmpty()) {
                 newText = text;
            }else {
                newText = currentText + "\n" + text;
            }

            // Обновляем сессию
            session.setText(newText);
            session.setChanged(true);

            return session;
        });
    }

    public static void changed(long senderId, boolean changed){
        addText(senderId,"");
        usersSessions.get(senderId).setChanged(changed);

    }
}
