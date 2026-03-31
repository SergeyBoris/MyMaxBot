package org.example.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.constants.Const;
import org.example.db.MapDB;
import org.example.entity.Update;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Collections;
import java.util.List;


public class UpdatesService {
    private final HttpClient client;
    private final ObjectMapper mapper;
    private final MapDB db;
    private final MessageService messageService;

    boolean firstRun = true; //ToDo получить последний макркер

    public UpdatesService(HttpClient client, ObjectMapper mapper, MapDB db, MessageService messageService) {
        this.client = client;
        this.mapper = mapper;
        this.db = db;
        this.messageService = messageService;
    }


    public List<Update> getUpdate() throws Exception {

        if (firstRun) {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(Const.BOT_URL + "?limit=1"))
                    .header("Authorization", Const.BOT_TOKEN)
                    .timeout(Duration.ofSeconds(40))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());


                long latestMarker = Integer.MAX_VALUE;
                db.setLastMarker(latestMarker);  // Сохраняем маркер последнего update
                System.out.println("Инициализирован маркер: " + latestMarker);
                firstRun = false;
                return Collections.emptyList();



        }




        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(Const.BOT_URL + "?marker=" + db.getLastMarker()))
                .header("Authorization", Const.BOT_TOKEN)
                .timeout(Duration.ofSeconds(60)) // long-poll таймаут
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            System.err.println("Ошибка API: " + response.body());
            return Collections.emptyList();
        }
        System.out.println(response.body());
        JsonNode root = mapper.readTree(response.body());

        // обновляем маркер, чтобы не получать старые сообщения
        if (root.has("marker")) {
            System.out.println("маркер обновления: " + root.get("marker").asLong());
            db.setLastMarker(root.get("marker").asLong());
        }

        JsonNode updatesNode = root.get("updates");
        if (updatesNode == null || !updatesNode.isArray() || updatesNode.size() == 0) {
            return Collections.emptyList();
        }

        return mapper.convertValue(updatesNode, new TypeReference<List<Update>>() {});
    }
}
