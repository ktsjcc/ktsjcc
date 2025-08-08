package com.example.stockmonitor.service;

import com.example.stockmonitor.model.StockModels.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SchedulerService {

    private final MarketDataService marketData;
    private final ScreeningService screening;
    private final GreedIndexService greedIndexService;

    @Value("${monitor.max.symbols.per.cycle:30}")
    private int maxSymbolsPerCycle;

    private final Map<String, AnalysisResult> latestAnalyses = new ConcurrentHashMap<String, AnalysisResult>();
    private volatile GreedIndex latestGreedIndex;

    public SchedulerService(MarketDataService marketData, ScreeningService screening, GreedIndexService greedIndexService) {
        this.marketData = marketData;
        this.screening = screening;
        this.greedIndexService = greedIndexService;
    }

    private boolean isUsMarketOpenNow() {
        ZonedDateTime nowUtc = ZonedDateTime.now(ZoneOffset.UTC);
        ZonedDateTime nowEt = nowUtc.withZoneSameInstant(ZoneId.of("America/New_York"));
        DayOfWeek d = nowEt.getDayOfWeek();
        if (d == DayOfWeek.SATURDAY || d == DayOfWeek.SUNDAY) return false;
        LocalTime t = nowEt.toLocalTime();
        LocalTime open = LocalTime.of(9, 30);
        LocalTime close = LocalTime.of(16, 0);
        return !t.isBefore(open) && !t.isAfter(close);
    }

    @Scheduled(fixedDelayString = "${monitor.fetch.interval.seconds:120}000")
    public void tick() {
        if (!isUsMarketOpenNow()) return;
        try {
            latestGreedIndex = greedIndexService.fetchFearGreed();
        } catch (Exception ignored) {}

        List<Candidate> candidates = marketData.fetchTopLoserCandidates(maxSymbolsPerCycle);
        int processed = 0;
        for (Candidate c : candidates) {
            if (processed >= maxSymbolsPerCycle) break;
            String symbol = c.symbol;
            TickerInfo info = marketData.fetchTickerInfo(symbol);
            Quote quote = marketData.fetchQuote(symbol);
            if (quote == null) continue;
            if (quote.changePercent > 0) continue; // only drops
            Fundamentals f = marketData.fetchFundamentals(symbol);
            if (!screening.isQualityFundamentally(f, info)) continue;
            TechnicalSnapshot t = marketData.buildTechnicals(symbol);
            AnalysisResult ar = screening.buildAnalysis(symbol,
                    info != null ? (info.shortName != null ? info.shortName : info.longName) : symbol,
                    info != null ? info.sector : null,
                    info != null ? info.industry : null,
                    quote, t, f, latestGreedIndex);
            latestAnalyses.put(symbol, ar);
            processed++;
        }
    }

    public Collection<AnalysisResult> getLatestAnalyses() {
        return latestAnalyses.values();
    }

    public GreedIndex getLatestGreedIndex() {
        return latestGreedIndex;
    }
}