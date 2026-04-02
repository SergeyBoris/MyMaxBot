package org.example.services;

import org.example.constants.Const;
import org.example.entity.User;
import org.example.util.PhotosToPdfConverter;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SaveFileService {
    public static void saveFile(User sender, String reqNumber, String text, List<String> photoUrls, String status) {
        String fileNameText = Const.ANSWER_FILE_NAME_PREFIX + reqNumber + "/" + reqNumber + ".txt";

        if (text == null || text.isEmpty()) {
            System.out.println("Текст отсутствует");
            text = "Текст отсутствует";
        }
        Path filePathText = Paths.get(fileNameText);
        try {
            Files.createDirectories(filePathText.getParent());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        text = "Имя отправителя: " + sender.getName() + "\n" +
                "ID Отправителя: " + sender.getUserId() + "\n" +
                "Статус: " + status + "\n" +
                "Время получения: " + LocalDateTime.now() + "\n" +
                text;


        try {


            // 3. Записываем с явной кодировкой
            try (OutputStream out = Files.newOutputStream(
                    filePathText,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING)) {

                out.write(text.getBytes(StandardCharsets.UTF_8));
            }

            System.out.println("Файл сохранён: " + filePathText.toAbsolutePath());

        } catch (FileAlreadyExistsException e) {
            System.err.println("Файл уже существует (но мы его перезаписали): " + e.getMessage());
        } catch (AccessDeniedException e) {
            System.err.println("Нет прав на запись: " + e.getMessage());
        } catch (NoSuchFileException e) {
            System.err.println("Не найдена директория: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("Ошибка ввода‑вывода: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            System.err.println("Ошибка данных: " + e.getMessage());
        }
        if (photoUrls != null) {
            int count = 0;
            List<String> photoUrlsList = new ArrayList<>();
            for (String photoUrl : photoUrls) {
                count++;
                String fileNamePhoto = Const.ANSWER_ATTACHMENT_FILE_NAME_PREFIX + reqNumber + "/" + reqNumber + "_" + count + ".webp";
                photoUrlsList.add(fileNamePhoto);
                Path filePathPhoto = Paths.get(fileNamePhoto);
                try {
                    Files.createDirectories(filePathPhoto.getParent());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                try (InputStream in = new URL(photoUrl).openStream()) {

                    // Копируем файл
                    Files.copy(in, filePathPhoto, StandardCopyOption.REPLACE_EXISTING);

                    System.out.println("Изображение сохранено как " + filePathPhoto.toAbsolutePath());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            PhotosToPdfConverter.convertPhotosToPdf(photoUrlsList,Const.ANSWER_ATTACHMENT_FILE_NAME_PREFIX + reqNumber + "/" + reqNumber + ".pdf");
        }
    }
}
