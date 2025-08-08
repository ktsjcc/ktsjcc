package com.example.stockmonitor.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@Component
public class YahooFinanceClient {

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    public YahooFinanceClient() {
        this.httpClient = new OkHttpClient.Builder()
                .retryOnConnectionFailure(true)
                .build();
        this.objectMapper = new ObjectMapper();
    }

    public List<Map<String, Object>> getTopLosers(int count) throws IOException {
        HttpUrl url = HttpUrl.parse("https://query1.finance.yahoo.com/v1/finance/screener/predefined/saved")
                .newBuilder()
                .addQueryParameter("formatted", "false")
                .addQueryParameter("scrIds", "day_losers")
                .addQueryParameter("count", String.valueOf(count))
                .build();
        Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0")
                .build();
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response);
            }
            JsonNode root = objectMapper.readTree(response.body().byteStream());
            List<Map<String, Object>> items = new ArrayList<Map<String, Object>>();
            JsonNode quotes = root.path("finance").path("result").get(0).path("quotes");
            if (quotes.isArray()) {
                for (JsonNode q : quotes) {
                    Map<String, Object> map = new HashMap<String, Object>();
                    map.put("symbol", q.path("symbol").asText());
                    map.put("shortName", q.path("shortName").asText(null));
                    map.put("regularMarketChangePercent", q.path("regularMarketChangePercent").asDouble());
                    map.put("regularMarketVolume", q.path("regularMarketVolume").asLong());
                    items.add(map);
                }
            }
            return items;
        }
    }

    public Map<String, Object> getQuoteSummary(String symbol) throws IOException {
        HttpUrl url = HttpUrl.parse("https://query1.finance.yahoo.com/v10/finance/quoteSummary/" + symbol)
                .newBuilder()
                .addQueryParameter("modules", "price,assetProfile,defaultKeyStatistics,financialData,summaryDetail")
                .build();
        Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0")
                .build();
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response);
            }
            JsonNode root = objectMapper.readTree(response.body().byteStream());
            JsonNode result = root.path("quoteSummary").path("result").get(0);
            Map<String, Object> map = new HashMap<String, Object>();
            if (result != null) {
                JsonNode price = result.path("price");
                JsonNode assetProfile = result.path("assetProfile");
                JsonNode financialData = result.path("financialData");
                JsonNode summaryDetail = result.path("summaryDetail");
                JsonNode defaultKeyStatistics = result.path("defaultKeyStatistics");

                map.put("shortName", text(price, "shortName"));
                map.put("longName", text(price, "longName"));
                map.put("exchange", text(price, "exchangeName"));
                map.put("marketCap", number(price, "marketCap"));
                map.put("previousClose", number(summaryDetail, "previousClose"));
                map.put("open", number(summaryDetail, "open"));
                map.put("dayLow", number(summaryDetail, "dayLow"));
                map.put("dayHigh", number(summaryDetail, "dayHigh"));
                map.put("regularMarketPrice", number(price, "regularMarketPrice"));
                map.put("regularMarketChangePercent", number(price, "regularMarketChangePercent"));
                map.put("regularMarketVolume", number(price, "regularMarketVolume"));
                map.put("averageDailyVolume3Month", number(summaryDetail, "averageVolume"));

                map.put("sector", text(assetProfile, "sector"));
                map.put("industry", text(assetProfile, "industry"));

                map.put("peRatio", number(summaryDetail, "trailingPE"));
                map.put("pbRatio", number(defaultKeyStatistics, "priceToBook"));
                map.put("profitMargins", number(summaryDetail, "profitMargins"));
                map.put("debtToEquity", number(financialData, "debtToEquity"));
                map.put("revenueGrowth", number(financialData, "revenueGrowth"));
                map.put("freeCashflow", number(financialData, "freeCashflow"));
                map.put("operatingMargins", number(financialData, "operatingMargins"));
                map.put("returnOnEquity", number(financialData, "returnOnEquity"));
            }
            return map;
        }
    }

    public List<Map<String, Object>> getChart(String symbol, String range, String interval) throws IOException {
        HttpUrl url = HttpUrl.parse("https://query1.finance.yahoo.com/v8/finance/chart/" + symbol)
                .newBuilder()
                .addQueryParameter("range", range)
                .addQueryParameter("interval", interval)
                .build();
        Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0")
                .build();
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response);
            }
            JsonNode root = objectMapper.readTree(response.body().byteStream());
            JsonNode result = root.path("chart").path("result").get(0);
            List<Map<String, Object>> bars = new ArrayList<Map<String, Object>>();
            if (result != null) {
                JsonNode timestamps = result.path("timestamp");
                JsonNode indicators = result.path("indicators").path("quote").get(0);
                JsonNode opens = indicators.path("open");
                JsonNode highs = indicators.path("high");
                JsonNode lows = indicators.path("low");
                JsonNode closes = indicators.path("close");
                JsonNode volumes = indicators.path("volume");
                int n = timestamps.size();
                for (int i = 0; i < n; i++) {
                    Map<String, Object> b = new HashMap<String, Object>();
                    long ts = timestamps.get(i).asLong();
                    b.put("time", Instant.ofEpochSecond(ts));
                    b.put("open", opens.get(i).asDouble());
                    b.put("high", highs.get(i).asDouble());
                    b.put("low", lows.get(i).asDouble());
                    b.put("close", closes.get(i).asDouble());
                    b.put("volume", volumes.get(i).asLong());
                    bars.add(b);
                }
            }
            return bars;
        }
    }

    private String text(JsonNode node, String field) {
        if (node == null) return null;
        JsonNode v = node.path(field);
        if (v == null || v.isMissingNode() || v.isNull()) return null;
        if (v.has("fmt")) return v.path("fmt").asText(null);
        return v.asText(null);
    }

    private Double number(JsonNode node, String field) {
        if (node == null) return null;
        JsonNode v = node.path(field);
        if (v == null || v.isMissingNode() || v.isNull()) return null;
        if (v.has("raw")) return v.path("raw").asDouble();
        return v.asDouble();
    }
}