package org.example.util;

import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.io.source.ByteArrayOutputStream;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Image;
import com.zakgof.webp4j.Webp4j;


import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class PhotosToPdfConverter {
    public static void convertPhotosToPdf(List<String> imagePaths, String outputPdfPath) {
        PdfWriter writer = null;
        try {
            writer = new PdfWriter(outputPdfPath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf, PageSize.A4);

        for (String imagePath : imagePaths) {
            System.out.println("Обрабатываем: " + new File(imagePath).getAbsolutePath());

            try {
                Image image;

                if (isWebPFormat(imagePath)) {
                    File file = new File(imagePath);
                    byte[] webPBytes = Files.readAllBytes(Paths.get(file.toURI()));
                    BufferedImage webPImage = Webp4j.decode(webPBytes);
                    // Загружаем WebP как BufferedImage


                    // Конвертируем в JPEG с качеством 80 %
                    byte[] jpegBytes = convertBufferedImageToJPEG(webPImage, 80f);
                    image = new Image(ImageDataFactory.create(jpegBytes));
                    System.out.println("Конвертировано из WebP: " + imagePath);
                } else {
                    // Обычный случай для поддерживаемых форматов
                    image = new Image(ImageDataFactory.create(imagePath));
                }

                // Применяем поворот, если нужно
                if (shouldRotateImage(imagePath)) {
                    image.setRotationAngle(Math.PI / 2); // 90 градусов
                }

                // Масштабируем под страницу
                scaleImageToPage(image, document);
                document.add(image);
                document.add(new com.itextpdf.layout.element.Paragraph(" "));
            } catch (IOException e) {
                System.err.println("Ошибка обработки файла: " + imagePath);
                e.printStackTrace();
            }
        }

        document.close();
        System.out.println("PDF успешно создан: " + outputPdfPath);
    }

    private static boolean isWebPFormat(String imagePath) {
        return imagePath.toLowerCase().endsWith(".webp");
    }

    /**
     * Конвертирует BufferedImage в массив байтов JPEG с заданным качеством.
     */
    private static byte[] convertBufferedImageToJPEG(BufferedImage image, float quality) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        // Получаем ImageWriter для формата JPEG
        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();

        // Создаём ImageWriteParam через getDefaultWriteParam()
        ImageWriteParam param = writer.getDefaultWriteParam();

        // Настраиваем качество (0.0 — худшее, 1.0 — лучшее)
        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        param.setCompressionQuality(quality / 100f);

        // Устанавливаем выходной поток
        writer.setOutput(ImageIO.createImageOutputStream(baos));

        // Записываем изображение с параметрами качества
        writer.write(null, new IIOImage(image, null, null), param);

        // Освобождаем ресурсы
        writer.dispose();

        return baos.toByteArray();
    }

    private static boolean shouldRotateImage(String imagePath) {
        try {
            // Декодируем WebP в BufferedImage
            byte[] webPBytes = Files.readAllBytes(Paths.get(new File(imagePath).toURI()));
            BufferedImage image = Webp4j.decode(webPBytes);

            if (image == null) {
                System.err.println("Не удалось декодировать WebP: " + imagePath);
                return false;
            }

            int width = image.getWidth();
            int height = image.getHeight();

            System.out.println("Размер изображения " + imagePath + ": " +
                    width + "x" + height +
                    (width > height ? " (альбомная → поворачиваем)" : " (портрет → не поворачиваем)"));

            // Поворачиваем, если ширина больше высоты (альбомная ориентация)
            return width > height;
        } catch (IOException e) {
            System.err.println("Ошибка чтения файла: " + imagePath);
            e.printStackTrace();
            return false;
        }
    }

    private static boolean useAspectRatioRotation(String imagePath) {
        try {
            BufferedImage image = ImageIO.read(new File(imagePath));
            if (image != null) {
                return image.getWidth() > image.getHeight();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false; // По умолчанию не поворачивать
    }

    private static void scaleImageToPage(Image image, Document document) {
        float pageWidth = document.getPageEffectiveArea(PageSize.A4).getWidth();
        float maxHeight = document.getPageEffectiveArea(PageSize.A4).getHeight() * 0.8f;
        image.scaleToFit(pageWidth, maxHeight);
    }
}
