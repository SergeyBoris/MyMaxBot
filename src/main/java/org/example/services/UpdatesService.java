package org.example.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.example.constants.Const;
import org.example.entity.Update;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UpdatesService {
    private final HttpClient client;
    private final ObjectMapper mapper;
    private final MessageService messageService;
    private long latestMarker=0;

    boolean firstRun = true; //ToDo получить последний макркер




    public List<Update> getUpdate() throws Exception {

        if (firstRun) {

                latestMarker = Integer.MAX_VALUE;
                System.out.println("Инициализирован маркер: " + latestMarker);
                firstRun = false;
                return Collections.emptyList();



        }




        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(Const.BOT_URL + "?marker=" +latestMarker))
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
            latestMarker = (root.get("marker").asLong());
        }

        JsonNode updatesNode = root.get("updates");
        if (updatesNode == null || !updatesNode.isArray() || updatesNode.size() == 0) {
            return Collections.emptyList();
        }

        return mapper.convertValue(updatesNode, new TypeReference<List<Update>>() {});
    }
}
