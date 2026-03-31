package org.example.db;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.constants.Const;
import org.example.entity.Req;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class MapDB {

    private final ObjectMapper mapper;
    private final File dbFile;

    private final ConcurrentHashMap<String, Req> requestsMap;
    private final ConcurrentHashMap<String, Long> lastMarkerMap;
    private final ConcurrentHashMap<Long, Long> usersMap;

    public MapDB(ObjectMapper mapper) {
        this.mapper = mapper;
        this.dbFile = new File(Const.DB_FILE_NAME);

        // Потокобезопасные карты
        this.requestsMap = new ConcurrentHashMap<>();
        this.lastMarkerMap = new ConcurrentHashMap<>();
        this.usersMap = new ConcurrentHashMap<>();

        loadDb();
    }

    private synchronized void loadDb() {
        if (!dbFile.exists()) {
            System.out.println("Файл базы данных не найден. Создаём новый.");
            return;
        }

        try {
            Map<String, Object> root = mapper.readValue(dbFile, new TypeReference<>() {});

            // requests
            Map<String, Req> requests = mapper.convertValue(root.get("requests"),
                    new TypeReference<>() {});
            if (requests != null) requestsMap.putAll(requests);

            // lastMarker
            Map<String, Long> lastMarker = mapper.convertValue(root.get("lastMarker"),
                    new TypeReference<>() {});
            if (lastMarker != null) lastMarkerMap.putAll(lastMarker);

            // users
            Map<Long, Long> users = mapper.convertValue(root.get("users"),
                    new TypeReference<>() {});
            if (users != null) usersMap.putAll(users);

        } catch (IOException e) {
            throw new RuntimeException("Ошибка чтения базы данных: " + e.getMessage(), e);
        }
    }

    private synchronized void saveDb() {
        Map<String, Object> export = Map.of(
                "requests", requestsMap,
                "lastMarker", lastMarkerMap,
                "users", usersMap
        );

        try {
            String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(export);
            Files.write(dbFile.toPath(), json.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException("Ошибка сохранения базы данных: " + e.getMessage(), e);
        }
    }

    // --- Методы работы с requests ---
    public Map<String, Req> getAllRequests() {
        return Map.copyOf(requestsMap);
    }

    public void addNewRequest(Req req) {
        requestsMap.put(req.getRequestNumber(), req);
        saveDb();
    }

    public void removeRequest(String requestNumber) {
        requestsMap.remove(requestNumber);
        saveDb();
    }

    // --- Методы работы с lastMarker ---
    public Long getLastMarker() {
        return lastMarkerMap.get("lastMarker");
    }

    public void setLastMarker(long lastMarker) {
        lastMarkerMap.put("lastMarker", lastMarker);
        saveDb();
    }

    // --- Методы работы с users ---
    public boolean checkUserPermissions(long userId) {
        return usersMap.containsKey(userId);
    }

    public void addUser(long userId, long chatId) {
        usersMap.put(userId, chatId);
        saveDb();
    }

    public Set<Long> getAllUsers() {
        return usersMap.keySet();
    }

    public Long getUserChatId(long userId) {
        return usersMap.get(userId);
    }

    public Set<Long> getAllUserChats() {
        return new HashSet<>(usersMap.values());
    }
}
