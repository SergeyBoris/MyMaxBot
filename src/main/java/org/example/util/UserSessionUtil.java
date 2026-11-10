package org.example.util;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
@Getter
public class UserSessionUtil {
    private final  Map<Long, UserUploadSession> usersSessions;

    public void addText(long senderId, String text) {
        usersSessions.compute(senderId, (key, session) -> {
            if (session == null) {
                // Сессии нет — создаём новую
                session = new UserUploadSession(); // замените на ваш конструктор
            }

            // Получаем текущий текст (если null — заменяем на пустую строку)
            String currentText = (session.getText() != null && !session.getText().equals("null")) ? session.getText() : "";
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

    public void changed(long senderId, boolean changed){
        addText(senderId,"");
        usersSessions.get(senderId).setChanged(changed);

    }
}
