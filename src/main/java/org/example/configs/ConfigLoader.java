package org.example.configs;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

public class ConfigLoader {

    public static <T> T load(String configFile, Class<T> tClass) throws Exception {
        ObjectMapper mapper = new ObjectMapper();


        File file = new File(configFile);
        if (!file.exists()) {
            // если не нашли рядом с рабочей папкой — ищем рядом с exe
            String exe = System.getProperty("jpackage.app-path");
            if (exe != null) {
                file = new File(new File(exe).getParentFile(), configFile);
            }
        }

        try {
            return mapper.readValue(file, tClass);
        } catch (JsonParseException e) {
            throw new IOException("Ошибка парсинга JSON в файле " + configFile, e);
        } catch (JsonMappingException e) {
            throw new IOException(
                    "Ошибка маппинга JSON в класс " + tClass.getSimpleName() + " из файла " + configFile, e);
        }
    }


}