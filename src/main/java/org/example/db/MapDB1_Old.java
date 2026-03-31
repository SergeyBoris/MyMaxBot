package org.example.db;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.constants.Const;
import org.example.entity.Req;
import org.example.util.RequestSerializer;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.HTreeMap;
import org.mapdb.Serializer;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class MapDB1_Old {

    private final ObjectMapper mapper;
    private HTreeMap<String, Req> requestsMap;
    private HTreeMap<String, Long> lastMarkerMap;
    private HTreeMap<Long, Long> usersMap;
    private DB db;

    public MapDB1_Old(ObjectMapper mapper) {
        this.mapper = mapper;
        initDb();
    }
    public void initDb(){
        boolean exists = new File(Const.DB_FILE_NAME).exists(); // проверяем, есть ли файл

        // создаём или открываем БД
        db = DBMaker.fileDB("bot_data.db")
                .fileMmapEnable()
                .closeOnJvmShutdown()  // MapDB закроет DB при завершении JVM
                .make();

        // карта заявок
        requestsMap = db.hashMap("requests")
                .keySerializer(Serializer.STRING)
                .valueSerializer(new RequestSerializer()) // вместо Serializer.STRING
                .createOrOpen();


        // карта для маркера
        lastMarkerMap = db.hashMap("marker")
                .keySerializer(Serializer.STRING)
                .valueSerializer(Serializer.LONG)
                .createOrOpen();

        usersMap = db.hashMap("users")
                .keySerializer(Serializer.LONG)
                .valueSerializer(Serializer.LONG)
                .createOrOpen();

        // Если база новая, добавляем тестовые данные
//        if (!exists) {
//            Request request = new Request("Заявка 1","Текст 1 ", "Адрес 1");
//            Request request1 = new Request("Req 2", "Test 2", "Address 2");
//            requestsMap.put(request.getRequestNumber(), request);
//            requestsMap.put(request1.getRequestNumber(), request1);
//            lastMarkerMap.put("lastMarker", 0L); // начальное значение маркера
//            db.commit(); // сохраняем на диск
//        }





    }

    public Map<String, Req> getAllRequests(){
       return requestsMap;
    }

    public Long getLastMarker(){
        return lastMarkerMap.get("lastMarker");
    }

    public void addNewRequest(Req req){
        requestsMap.put(req.getRequestNumber(), req);
        db.commit();
    }
    public void removeRequest(String requestNumber){
        requestsMap.remove(requestNumber);
        db.commit();
    }
    public void setLastMarker(long lastMarker){
        lastMarkerMap.put("lastMarker", lastMarker);
        db.commit();
    }
    public boolean checkUserPermissions(long userId){
        return usersMap.containsKey(userId);
    }

    public void addUser(long userId, long chatId){
        usersMap.put(userId, chatId);
    }

    public Set<Long> getAllUsers(){
        return usersMap.keySet();
    }

    public Long getUserChatId(long userId){
        return usersMap.get(userId);
    }

    public void commit(){
        db.commit();
    }

    public String exportAllDataAsJson() throws Exception {
        Map<String, Object> export = new HashMap<>();
        export.put("requests", new HashMap<>(requestsMap));
        export.put("lastMarker", new HashMap<>(lastMarkerMap));
        export.put("users", new HashMap<>(usersMap));

        return mapper.writerWithDefaultPrettyPrinter()
                .writeValueAsString(export);
    }

}
