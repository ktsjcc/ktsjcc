package com.example.stockmonitor.web;

import com.example.stockmonitor.model.StockModels.AnalysisResult;
import com.example.stockmonitor.model.StockModels.GreedIndex;
import com.example.stockmonitor.model.StockModels.TradePlan;
import com.example.stockmonitor.service.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Controller
public class DashboardController {

    private final SchedulerService schedulerService;
    private final MarketDataService marketData;
    private final AggregationService aggregationService;
    private final MonteCarloService monteCarloService;

    public DashboardController(SchedulerService schedulerService, MarketDataService marketData,
                               AggregationService aggregationService, MonteCarloService monteCarloService) {
        this.schedulerService = schedulerService;
        this.marketData = marketData;
        this.aggregationService = aggregationService;
        this.monteCarloService = monteCarloService;
    }

    @GetMapping("/")
    public String index(Model model) {
        Collection<AnalysisResult> items = schedulerService.getLatestAnalyses();
        GreedIndex gi = schedulerService.getLatestGreedIndex();
        model.addAttribute("analyses", items);
        model.addAttribute("greedIndex", gi);
        model.addAttribute("hotspots", aggregationService.computeSectorHotspots(items));
        model.addAttribute("topPicks", aggregationService.pickTopByValue(items, 5));
        return "index";
    }

    @GetMapping("/api/analyses")
    @ResponseBody
    public Collection<AnalysisResult> analyses() {
        return schedulerService.getLatestAnalyses();
    }

    @GetMapping("/api/hotspots")
    @ResponseBody
    public Object hotspots() {
        return aggregationService.computeSectorHotspots(schedulerService.getLatestAnalyses());
    }

    @GetMapping("/api/top-picks")
    @ResponseBody
    public Object topPicks() {
        return aggregationService.pickTopByValue(schedulerService.getLatestAnalyses(), 5);
    }

    @PostMapping("/api/trade-plan")
    @ResponseBody
    public TradePlan tradePlan(@RequestParam String symbol, @RequestParam double capital) {
        // very simple volatility-based sizing using ATR if available
        TradePlan tp = new TradePlan();
        tp.symbol = symbol;
        tp.capital = capital;
        double price = Double.NaN;
        double atr = Double.NaN;
        AnalysisResult ar = null;
        for (AnalysisResult r : schedulerService.getLatestAnalyses()) {
            if (r.symbol.equalsIgnoreCase(symbol)) { ar = r; break; }
        }
        if (ar != null && ar.quote != null) {
            price = ar.quote.price;
        }
        if (ar != null && ar.technicals != null) {
            atr = ar.technicals.atr14;
        }
        if (Double.isNaN(price) || price <= 0) price = marketData.fetchQuote(symbol).price;
        if (Double.isNaN(atr) || atr <= 0) atr = 0.02 * price; // fallback 2%
        double riskPerShare = Math.max(atr, 0.01 * price);
        double riskPerTrade = 0.01 * capital; // 1% risk per trade
        int qty = (int)Math.floor(riskPerTrade / riskPerShare);
        if (qty < 1) qty = 1;
        tp.quantity = qty;
        tp.suggestedEntry = price;
        tp.stopLoss = price - 1.5 * atr;
        tp.takeProfit = price + 2.5 * atr;
        tp.method = "VolatilityTarget(ATR)";
        tp.notes = "Risk 1% of capital; SL 1.5x ATR, TP 2.5x ATR";
        return tp;
    }

    public static class McResponse { public double prob; public double exp; public double var95; }

    @PostMapping("/api/monte-carlo")
    @ResponseBody
    public McResponse monteCarlo(@RequestParam String symbol,
                                 @RequestParam(defaultValue = "20") double horizonDays,
                                 @RequestParam(defaultValue = "0.03") double dailyVolatility) {
        AnalysisResult ar = null;
        for (AnalysisResult r : schedulerService.getLatestAnalyses()) {
            if (r.symbol.equalsIgnoreCase(symbol)) { ar = r; break; }
        }
        double price = (ar != null && ar.quote != null) ? ar.quote.price : marketData.fetchQuote(symbol).price;
        double tp = price * 1.05;
        double sl = price * 0.97;
        MonteCarloService.SimulationResult sr = monteCarloService.simulate(price, dailyVolatility, horizonDays, tp, sl, 5000);
        McResponse m = new McResponse();
        m.prob = sr.probabilityOfProfit;
        m.exp = sr.expectedReturn;
        m.var95 = sr.var95;
        return m;
    }
}