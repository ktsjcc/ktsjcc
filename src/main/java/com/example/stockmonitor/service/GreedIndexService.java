package com.example.stockmonitor.service;

import com.example.stockmonitor.model.StockModels.GreedIndex;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Instant;

@Service
public class GreedIndexService {

    private final OkHttpClient httpClient = new OkHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public GreedIndex fetchFearGreed() {
        Request req = new Request.Builder()
                .url("https://production.dataviz.cnn.io/index/fearandgreed/graphdata")
                .header("User-Agent", "Mozilla/5.0")
                .build();
        try (Response resp = httpClient.newCall(req).execute()) {
            if (!resp.isSuccessful()) return null;
            JsonNode root = objectMapper.readTree(resp.body().byteStream());
            JsonNode now = root.path("fear_and_greed").path("now");
            GreedIndex gi = new GreedIndex();
            gi.value = now.path("value").asInt();
            gi.label = now.path("label").asText();
            gi.asOf = Instant.now();
            return gi;
        } catch (IOException e) {
            return null;
        }
    }
}