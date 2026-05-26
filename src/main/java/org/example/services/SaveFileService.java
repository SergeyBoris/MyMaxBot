package org.example.services;

import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.io.source.ByteArrayOutputStream;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Image;
import com.zakgof.webp4j.Webp4j;
import org.example.constants.Const;
import org.example.entity.User;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class SaveFileService {

    private static final float JPEG_QUALITY = 80f;

    public static List<Path> saveFile(User sender, String reqNumber, String text, List<String> photoUrls, String status) {
        String fileNameText = Const.ANSWER_FILE_NAME_PREFIX + reqNumber + "/" + reqNumber + ".txt";
        List<Path> jpgPaths = new ArrayList<>();
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

        // Сохраняем текстовый файл
        try (OutputStream out = Files.newOutputStream(
                filePathText,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING)) {
            out.write(text.getBytes(StandardCharsets.UTF_8));
            System.out.println("Файл сохранён: " + filePathText.toAbsolutePath());
        } catch (Exception e) {
            System.err.println("Ошибка сохранения текстового файла: " + e.getMessage());
        }

        // Сохраняем фото и создаём PDF
        if (photoUrls != null && !photoUrls.isEmpty()) {

            int count = 0;

            for (String photoUrl : photoUrls) {
                count++;
                // Сохраняем сразу в JPG (не в WebP)
                String fileNameJpg = Const.ANSWER_ATTACHMENT_FILE_NAME_PREFIX + reqNumber + "/" + reqNumber + "_" + count + ".jpg";
                Path jpgPath = Paths.get(fileNameJpg);
                jpgPaths.add(jpgPath);

                try {
                    Files.createDirectories(jpgPath.getParent());
                    downloadAndSaveAsJpg(photoUrl, jpgPath);
                    System.out.println("Изображение сохранено как JPG: " + jpgPath.toAbsolutePath());
                } catch (Exception e) {
                    System.err.println("Ошибка обработки изображения: " + photoUrl);
                    e.printStackTrace();
                }
            }

            // Создаём PDF из JPG файлов
            if (!jpgPaths.isEmpty()) {
                String pdfPath = Const.ANSWER_ATTACHMENT_FILE_NAME_PREFIX + reqNumber + "/" + reqNumber + ".pdf";
                convertJpgToPdf(jpgPaths, pdfPath);
            }
        }
        return jpgPaths;
    }

    /**
     * Скачивает изображение и сохраняет в формате JPG
     */
    private static void downloadAndSaveAsJpg(String imageUrl, Path outputPath) throws Exception {
        try (InputStream in = new URL(imageUrl).openStream()) {
            byte[] imageBytes = in.readAllBytes();
            byte[] jpegBytes;

            if (isWebP(imageBytes)) {
                // Конвертируем WebP в JPG
                jpegBytes = convertWebPToJpegBytes(imageBytes, JPEG_QUALITY);
                System.out.println("WebP конвертирован в JPG");
            } else {
                // Конвертируем PNG/другое в JPG
                BufferedImage img = ImageIO.read(new ByteArrayInputStream(imageBytes));
                if (img != null) {
                    jpegBytes = convertBufferedImageToJPEG(img, JPEG_QUALITY);
                } else {
                    jpegBytes = imageBytes;
                }
            }

            Files.write(outputPath, jpegBytes);
        }
    }

    /**
     * Конвертирует WebP байты в JPEG байты
     */
    private static byte[] convertWebPToJpegBytes(byte[] webPBytes, float quality) throws IOException {
        BufferedImage webPImage = Webp4j.decode(webPBytes);
        return convertBufferedImageToJPEG(webPImage, quality);
    }

    /**
     * Конвертирует BufferedImage в массив байтов JPEG с заданным качеством
     */
    private static byte[] convertBufferedImageToJPEG(BufferedImage image, float quality) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
        ImageWriteParam param = writer.getDefaultWriteParam();

        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        param.setCompressionQuality(quality / 100f);

        writer.setOutput(ImageIO.createImageOutputStream(baos));
        writer.write(null, new IIOImage(image, null, null), param);
        writer.dispose();

        return baos.toByteArray();
    }

    /**
     * Конвертирует список JPG файлов в PDF
     */
    private static void convertJpgToPdf(List<Path> jpgPaths, String outputPdfPath) {
        try (PdfWriter writer = new PdfWriter(outputPdfPath);
             PdfDocument pdf = new PdfDocument(writer);
             Document document = new Document(pdf, PageSize.A4)) {

            for (Path jpgPath : jpgPaths) {
                if (Files.exists(jpgPath) && Files.size(jpgPath) > 0) {
                    try {
                        Image image = new Image(ImageDataFactory.create(jpgPath.toString()));

                        // Поворачиваем, если нужно
                        if (shouldRotateImage(jpgPath)) {
                            image.setRotationAngle(Math.PI / 2);
                        }

                        // Масштабируем под страницу
                        scaleImageToPage(image, document);
                        document.add(image);
                        document.add(new com.itextpdf.layout.element.Paragraph(" "));

                        System.out.println("Добавлено в PDF: " + jpgPath.getFileName());
                    } catch (Exception e) {
                        System.err.println("Ошибка добавления файла в PDF: " + jpgPath);
                        e.printStackTrace();
                    }
                }
            }

            System.out.println("PDF успешно создан: " + outputPdfPath);
        } catch (IOException e) {
            System.err.println("Ошибка создания PDF: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Определяет, является ли изображение WebP
     */
    private static boolean isWebP(byte[] imageBytes) {
        // Проверка сигнатуры WebP: RIFF....WEBP
        return imageBytes.length > 12 &&
                imageBytes[0] == 0x52 && imageBytes[1] == 0x49 &&
                imageBytes[2] == 0x46 && imageBytes[3] == 0x46 &&
                imageBytes[8] == 0x57 && imageBytes[9] == 0x45 &&
                imageBytes[10] == 0x42 && imageBytes[11] == 0x50;
    }

    /**
     * Проверяет, нужно ли повернуть изображение
     */
    private static boolean shouldRotateImage(Path imagePath) {
        try {
            byte[] bytes = Files.readAllBytes(imagePath);
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));

            if (image == null) {
                return false;
            }

            int width = image.getWidth();
            int height = image.getHeight();

            System.out.println("Размер изображения " + imagePath.getFileName() + ": " +
                    width + "x" + height +
                    (width > height ? " (альбомная → поворачиваем)" : " (портрет → не поворачиваем)"));

            return width > height;
        } catch (IOException e) {
            System.err.println("Ошибка чтения файла: " + imagePath);
            return false;
        }
    }

    /**
     * Масштабирует изображение под размер страницы
     */
    private static void scaleImageToPage(Image image, Document document) {
        float pageWidth = document.getPageEffectiveArea(PageSize.A4).getWidth();
        float maxHeight = document.getPageEffectiveArea(PageSize.A4).getHeight() * 0.8f;
        image.scaleToFit(pageWidth, maxHeight);
    }
}