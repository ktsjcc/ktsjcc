package com.example.stockmonitor.service;

import com.example.stockmonitor.model.StockModels.IntradayBar;
import com.example.stockmonitor.model.StockModels.TechnicalSnapshot;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class IndicatorService {

    public TechnicalSnapshot computeTechnicalSnapshot(List<IntradayBar> bars) {
        TechnicalSnapshot t = new TechnicalSnapshot();
        if (bars == null || bars.size() < 15) {
            return t;
        }
        List<Double> closes = new ArrayList<Double>();
        List<Long> volumes = new ArrayList<Long>();
        for (IntradayBar b : bars) {
            closes.add(b.close);
            volumes.add(b.volume);
        }
        t.rsi14 = rsi(closes, 14);
        t.ema20 = ema(closes, 20);
        t.ema50 = ema(closes, 50);
        t.atr14 = atr(bars, 14);
        t.obv = obv(bars);
        t.volumeZScore = zscore(volumes, Math.min(100, volumes.size()));
        return t;
    }

    public double rsi(List<Double> closes, int period) {
        if (closes.size() < period + 1) return Double.NaN;
        double gain = 0.0;
        double loss = 0.0;
        for (int i = 1; i <= period; i++) {
            double change = closes.get(i) - closes.get(i - 1);
            if (change >= 0) gain += change; else loss -= change;
        }
        double avgGain = gain / period;
        double avgLoss = loss / period;
        for (int i = period + 1; i < closes.size(); i++) {
            double change = closes.get(i) - closes.get(i - 1);
            double g = Math.max(0, change);
            double l = Math.max(0, -change);
            avgGain = (avgGain * (period - 1) + g) / period;
            avgLoss = (avgLoss * (period - 1) + l) / period;
        }
        if (avgLoss == 0) return 100.0;
        double rs = avgGain / avgLoss;
        return 100.0 - (100.0 / (1.0 + rs));
    }

    public double ema(List<Double> values, int period) {
        if (values.isEmpty()) return Double.NaN;
        double k = 2.0 / (period + 1.0);
        double ema = values.get(0);
        for (int i = 1; i < values.size(); i++) {
            ema = values.get(i) * k + ema * (1.0 - k);
        }
        return ema;
    }

    public double atr(List<IntradayBar> bars, int period) {
        if (bars.size() < period + 1) return Double.NaN;
        List<Double> trs = new ArrayList<Double>();
        for (int i = 1; i < bars.size(); i++) {
            IntradayBar c = bars.get(i);
            IntradayBar p = bars.get(i - 1);
            double tr = Math.max(c.high - c.low,
                    Math.max(Math.abs(c.high - p.close), Math.abs(c.low - p.close)));
            trs.add(tr);
        }
        // Wilder's smoothing
        double atr = 0.0;
        for (int i = 0; i < period; i++) atr += trs.get(i);
        atr /= period;
        for (int i = period; i < trs.size(); i++) {
            atr = (atr * (period - 1) + trs.get(i)) / period;
        }
        return atr;
    }

    public double obv(List<IntradayBar> bars) {
        if (bars.isEmpty()) return 0.0;
        double obv = 0.0;
        for (int i = 1; i < bars.size(); i++) {
            double change = bars.get(i).close - bars.get(i - 1).close;
            if (change > 0) obv += bars.get(i).volume;
            else if (change < 0) obv -= bars.get(i).volume;
        }
        return obv;
    }

    public double zscore(List<Long> values, int window) {
        if (values.isEmpty()) return Double.NaN;
        int n = Math.min(window, values.size());
        double mean = 0.0;
        for (int i = values.size() - n; i < values.size(); i++) mean += values.get(i);
        mean /= n;
        double var = 0.0;
        for (int i = values.size() - n; i < values.size(); i++) {
            double d = values.get(i) - mean;
            var += d * d;
        }
        var /= n;
        double std = Math.sqrt(var);
        if (std == 0) return 0.0;
        double last = values.get(values.size() - 1);
        return (last - mean) / std;
    }
}