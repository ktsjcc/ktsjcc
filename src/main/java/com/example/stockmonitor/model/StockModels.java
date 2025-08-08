package com.example.stockmonitor.model;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public class StockModels {

    public static class TickerInfo {
        public String symbol;
        public String shortName;
        public String longName;
        public String sector;
        public String industry;
        public String exchange;
        public long marketCap;
    }

    public static class Quote {
        public String symbol;
        public double price;
        public double changePercent;
        public double previousClose;
        public long volume;
        public long averageVolume;
        public double open;
        public double dayLow;
        public double dayHigh;
        public long marketCap;
        public Instant asOf;
    }

    public static class IntradayBar {
        public Instant time;
        public double open;
        public double high;
        public double low;
        public double close;
        public long volume;
    }

    public static class TechnicalSnapshot {
        public double rsi14;
        public double obv;
        public double volumeZScore;
        public double ema20;
        public double ema50;
        public double atr14;
    }

    public static class Fundamentals {
        public Double peRatio;
        public Double pbRatio;
        public Double profitMargins;
        public Double debtToEquity;
        public Double revenueGrowth;
        public Double freeCashflow;
        public Double operatingMargins;
        public Double returnOnEquity;
    }

    public static class AnalysisResult {
        public String symbol;
        public String name;
        public String sector;
        public String industry;
        public Quote quote;
        public TechnicalSnapshot technicals;
        public Fundamentals fundamentals;
        public double qualityScore;
        public double valuationScore;
        public double momentumScore;
        public String signal; // BUY, WATCH, AVOID
        public String reason;
        public Map<String, Object> extras;
    }

    public static class SectorHotspot {
        public String sector;
        public int losersCount;
        public long totalVolume;
        public double averageDropPercent;
    }

    public static class TradePlan {
        public String symbol;
        public double capital;
        public double suggestedEntry;
        public double stopLoss;
        public double takeProfit;
        public int quantity;
        public String method; // e.g. Kelly, VolatilityTarget
        public String notes;
    }

    public static class GreedIndex {
        public int value; // 0-100
        public String label; // Extreme Fear -> Extreme Greed
        public Instant asOf;
    }

    public static class ScreenerItem {
        public String symbol;
        public String shortName;
        public double changePercent;
        public long volume;
        public String sector;
    }

    public static class Candidate {
        public String symbol;
        public String shortName;
        public double dropPercent;
    }

}