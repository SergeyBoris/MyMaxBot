package org.example.entity.Contragents;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;

public class test {
        public static void main(String[] args) throws IOException {
            String base64 = Files.readString(Paths.get("multipart_base64.txt"))
                    .replaceAll("\\s+", ""); // убираем пробелы и переносы строк
            byte[] decoded = Base64.getDecoder().decode(base64);

            // Сохраняем как бинарный файл
            Files.write(Paths.get("multipart_request.bin"), decoded);
            System.out.println("Сохранено в multipart_request.bin");
        }
}
