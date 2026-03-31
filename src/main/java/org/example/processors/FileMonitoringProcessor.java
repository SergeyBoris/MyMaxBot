package org.example.processors;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.constants.Const;
import org.example.db.MapDB;
import org.example.entity.Req;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.List;

public class FileMonitoringProcessor implements Runnable {
    private MapDB db;
    private final ObjectMapper mapper;
    private final UpdateProcessor updateProcessor;
    private volatile boolean running = true;



    public FileMonitoringProcessor(MapDB db, ObjectMapper mapper, UpdateProcessor updateProcessor) {
        this.db = db;
        this.mapper = mapper;
        this.updateProcessor = updateProcessor;
    }

    public void run() {

        Path filePath = Paths.get(Const.NEW_REQUEST_FILE_NAME).toAbsolutePath();
        System.out.println("Мониторинг корня программы: " + filePath);
        System.out.println("Мониторинг директории: " + filePath);

        // Бесконечный цикл для отслеживания событий
        while (running) {
            try {

                if (Files.exists(filePath)) {
                    File jsonFile = new File(Const.NEW_REQUEST_FILE_NAME);
                    List<Req> reqs = mapper.readValue(jsonFile, mapper.getTypeFactory().constructCollectionType(List.class, Req.class));
                    for (Req req : reqs) {
                        db.addNewRequest(req);
                        updateProcessor.sendMessageNewRequest(req);
                    }
                    jsonFile.delete();
                }
                Thread.sleep(Const.CHECK_NEW_REQUEST_TIME_DELAY);
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }

        }

    }
    public void shutdown() {
        running = false;
        System.out.println("Отправлена остановка потока мониторинга фалов");
    }

}

