package org.example.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.constants.Const;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class GetLastMarker {
    private ObjectMapper mapper;
    private HttpClient client;

    public GetLastMarker(ObjectMapper mapper, HttpClient client){
        this.mapper = mapper;
        this.client = client;
    }
    // Получаем текущий маркер
    public long fetchInitialMarker() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(Const.BOT_URL + "?limit=100&dir=newer")) // вернёт только одно обновление
                .header("Authorization", Const.BOT_TOKEN)
                .timeout(Duration.ofSeconds(130))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new RuntimeException("Ошибка API: " + response.body());
        }

        JsonNode root = mapper.readTree(response.body());
        return root.has("marker") ? root.get("marker").asLong() : 0L;
    }
}
