package org.example.util;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.Main;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

public class ConfigLoader {

    public static <T> T load(String configFile, Class<T> tClass) throws Exception {
        ObjectMapper mapper = new ObjectMapper();


        File file = new File(configFile);
        if (!file.exists()) {
            throw new FileNotFoundException("Конфигурационный файл не найден: " + configFile);
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