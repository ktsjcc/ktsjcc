package com.example.stockmonitor.service;

import com.example.stockmonitor.model.StockModels.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

@Service
public class ScreeningService {

    @Value("${monitor.drop.threshold.primary:10}")
    private double primaryDropThreshold;

    public boolean isQualityFundamentally(Fundamentals f, TickerInfo info) {
        if (info == null) return false;
        if (info.marketCap < 2_000_000_000L) return false; // prefer mid/large caps
        if (f == null) return true; // fallback if no data
        if (f.profitMargins != null && f.profitMargins < 0) return false;
        if (f.debtToEquity != null && f.debtToEquity > 2.0) return false;
        if (f.returnOnEquity != null && f.returnOnEquity < 0.05) return false;
        return true;
    }

    public String deriveSignal(Quote q, TechnicalSnapshot t, GreedIndex gi) {
        if (q == null || t == null) return "WATCH";
        boolean oversold = !Double.isNaN(t.rsi14) && t.rsi14 < 30.0;
        boolean volumeSpike = !Double.isNaN(t.volumeZScore) && t.volumeZScore > 1.5;
        boolean priceBelowEMAs = !Double.isNaN(t.ema20) && !Double.isNaN(t.ema50)
                && q.price < t.ema20 && q.price < t.ema50;
        boolean fear = gi != null && gi.value < 40;

        if (oversold && volumeSpike && fear) return "BUY";
        if (oversold && volumeSpike) return "BUY";
        if (priceBelowEMAs && !oversold) return "WATCH";
        return "WATCH";
    }

    public double computeQualityScore(Fundamentals f) {
        double score = 0.5;
        if (f == null) return score;
        if (f.profitMargins != null) score += Math.min(0.3, f.profitMargins * 0.5);
        if (f.returnOnEquity != null) score += Math.min(0.3, f.returnOnEquity * 0.5);
        if (f.debtToEquity != null) score -= Math.max(0.0, (f.debtToEquity - 1.0) * 0.1);
        if (f.revenueGrowth != null) score += Math.min(0.3, f.revenueGrowth * 0.5);
        return Math.max(0.0, Math.min(1.0, score));
    }

    public double computeMomentumScore(TechnicalSnapshot t) {
        if (t == null) return 0.0;
        double score = 0.0;
        if (!Double.isNaN(t.rsi14)) score += (50.0 - Math.min(50.0, Math.abs(50.0 - t.rsi14))) / 50.0 * 0.4;
        if (!Double.isNaN(t.volumeZScore)) score += Math.min(1.0, Math.max(0.0, t.volumeZScore / 3.0)) * 0.3;
        return Math.max(0.0, Math.min(1.0, score));
    }

    public double computeValuationScore(Fundamentals f) {
        if (f == null) return 0.5;
        double score = 0.5;
        if (f.peRatio != null && f.peRatio > 0 && f.peRatio < 20) score += 0.2;
        if (f.pbRatio != null && f.pbRatio < 3) score += 0.1;
        if (f.freeCashflow != null && f.freeCashflow > 0) score += 0.2;
        return Math.max(0.0, Math.min(1.0, score));
    }

    public AnalysisResult buildAnalysis(String symbol, String name, String sector, String industry,
                                        Quote q, TechnicalSnapshot t, Fundamentals f, GreedIndex gi) {
        AnalysisResult ar = new AnalysisResult();
        ar.symbol = symbol;
        ar.name = name;
        ar.sector = sector;
        ar.industry = industry;
        ar.quote = q;
        ar.technicals = t;
        ar.fundamentals = f;
        ar.qualityScore = computeQualityScore(f);
        ar.momentumScore = computeMomentumScore(t);
        ar.valuationScore = computeValuationScore(f);
        ar.signal = deriveSignal(q, t, gi);
        StringBuilder reason = new StringBuilder();
        if (q != null) {
            reason.append(String.format("Drop %.2f%%", q.changePercent));
        }
        if (t != null && !Double.isNaN(t.rsi14)) reason.append(" | RSI ").append(String.format("%.1f", t.rsi14));
        if (t != null && !Double.isNaN(t.volumeZScore)) reason.append(" | VolZ ").append(String.format("%.2f", t.volumeZScore));
        if (gi != null) reason.append(" | Market ").append(gi.label).append(" (").append(gi.value).append(")");
        ar.reason = reason.toString();
        return ar;
    }
}