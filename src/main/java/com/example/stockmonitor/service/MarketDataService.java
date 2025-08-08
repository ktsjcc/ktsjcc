package com.example.stockmonitor.service;

import com.example.stockmonitor.model.StockModels.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Instant;
import java.util.*;

@Service
public class MarketDataService {

    private final YahooFinanceClient yahoo;
    private final IndicatorService indicators;

    @Value("${monitor.chart.range:1mo}")
    private String chartRange;
    @Value("${monitor.chart.interval:5m}")
    private String chartInterval;

    public MarketDataService(YahooFinanceClient yahoo, IndicatorService indicators) {
        this.yahoo = yahoo;
        this.indicators = indicators;
    }

    public List<Candidate> fetchTopLoserCandidates(int limit) {
        try {
            List<Map<String, Object>> items = yahoo.getTopLosers(limit);
            List<Candidate> result = new ArrayList<Candidate>();
            for (Map<String, Object> m : items) {
                Candidate c = new Candidate();
                c.symbol = (String) m.get("symbol");
                c.shortName = (String) m.get("shortName");
                Object cp = m.get("regularMarketChangePercent");
                c.dropPercent = cp == null ? 0.0 : ((Number) cp).doubleValue();
                result.add(c);
            }
            return result;
        } catch (IOException e) {
            return Collections.emptyList();
        }
    }

    public TickerInfo fetchTickerInfo(String symbol) {
        try {
            Map<String, Object> m = yahoo.getQuoteSummary(symbol);
            TickerInfo info = new TickerInfo();
            info.symbol = symbol;
            info.shortName = (String) m.get("shortName");
            info.longName = (String) m.get("longName");
            info.sector = (String) m.get("sector");
            info.industry = (String) m.get("industry");
            info.exchange = (String) m.get("exchange");
            Number mc = (Number) m.get("marketCap");
            info.marketCap = mc == null ? 0L : mc.longValue();
            return info;
        } catch (IOException e) {
            return null;
        }
    }

    public Quote fetchQuote(String symbol) {
        try {
            Map<String, Object> m = yahoo.getQuoteSummary(symbol);
            Quote q = new Quote();
            q.symbol = symbol;
            Number price = (Number) m.get("regularMarketPrice");
            q.price = price == null ? Double.NaN : price.doubleValue();
            Number chg = (Number) m.get("regularMarketChangePercent");
            q.changePercent = chg == null ? Double.NaN : chg.doubleValue();
            Number pc = (Number) m.get("previousClose");
            q.previousClose = pc == null ? Double.NaN : pc.doubleValue();
            Number vol = (Number) m.get("regularMarketVolume");
            q.volume = vol == null ? 0L : vol.longValue();
            Number avgVol = (Number) m.get("averageDailyVolume3Month");
            q.averageVolume = avgVol == null ? 0L : avgVol.longValue();
            Number open = (Number) m.get("open");
            q.open = open == null ? Double.NaN : open.doubleValue();
            Number dayLow = (Number) m.get("dayLow");
            q.dayLow = dayLow == null ? Double.NaN : dayLow.doubleValue();
            Number dayHigh = (Number) m.get("dayHigh");
            q.dayHigh = dayHigh == null ? Double.NaN : dayHigh.doubleValue();
            Number mc = (Number) m.get("marketCap");
            q.marketCap = mc == null ? 0L : mc.longValue();
            q.asOf = Instant.now();
            return q;
        } catch (IOException e) {
            return null;
        }
    }

    public Fundamentals fetchFundamentals(String symbol) {
        try {
            Map<String, Object> m = yahoo.getQuoteSummary(symbol);
            Fundamentals f = new Fundamentals();
            f.peRatio = (Double) m.get("peRatio");
            f.pbRatio = (Double) m.get("pbRatio");
            f.profitMargins = (Double) m.get("profitMargins");
            f.debtToEquity = (Double) m.get("debtToEquity");
            f.revenueGrowth = (Double) m.get("revenueGrowth");
            f.freeCashflow = (Double) m.get("freeCashflow");
            f.operatingMargins = (Double) m.get("operatingMargins");
            f.returnOnEquity = (Double) m.get("returnOnEquity");
            return f;
        } catch (IOException e) {
            return null;
        }
    }

    public List<IntradayBar> fetchChartBars(String symbol) {
        try {
            List<Map<String, Object>> rows = yahoo.getChart(symbol, chartRange, chartInterval);
            List<IntradayBar> bars = new ArrayList<IntradayBar>();
            for (Map<String, Object> r : rows) {
                IntradayBar b = new IntradayBar();
                b.time = (Instant) r.get("time");
                b.open = ((Number) r.get("open")).doubleValue();
                b.high = ((Number) r.get("high")).doubleValue();
                b.low = ((Number) r.get("low")).doubleValue();
                b.close = ((Number) r.get("close")).doubleValue();
                b.volume = ((Number) r.get("volume")).longValue();
                bars.add(b);
            }
            return bars;
        } catch (IOException e) {
            return Collections.emptyList();
        }
    }

    public TechnicalSnapshot buildTechnicals(String symbol) {
        List<IntradayBar> bars = fetchChartBars(symbol);
        return indicators.computeTechnicalSnapshot(bars);
    }
}