package com.example.stockmonitor.service;

import org.apache.commons.math3.distribution.NormalDistribution;
import org.springframework.stereotype.Service;

import java.util.Random;

@Service
public class MonteCarloService {

    private final Random random = new Random();
    private final NormalDistribution nd = new NormalDistribution(0, 1);

    public static class SimulationResult {
        public double probabilityOfProfit;
        public double expectedReturn;
        public double var95;
    }

    public SimulationResult simulate(double price, double dailyVolatility, double horizonDays, double takeProfit, double stopLoss, int paths) {
        SimulationResult r = new SimulationResult();
        if (price <= 0 || dailyVolatility <= 0 || paths <= 0) return r;
        int wins = 0;
        double sumRet = 0.0;
        double[] finals = new double[paths];
        double dt = 1.0;
        for (int p = 0; p < paths; p++) {
            double s = price;
            boolean hit = false;
            for (int d = 0; d < (int)horizonDays; d++) {
                double z = nd.sample();
                double change = s * dailyVolatility * Math.sqrt(dt) * z; // drift ~0 for simplicity
                s += change;
                if (s >= takeProfit) { wins++; hit = true; break; }
                if (s <= stopLoss) { hit = true; break; }
            }
            if (!hit) {
                if (s > price) wins++;
            }
            sumRet += (s - price) / price;
            finals[p] = (s - price) / price;
        }
        r.probabilityOfProfit = (double) wins / paths;
        r.expectedReturn = sumRet / paths;
        java.util.Arrays.sort(finals);
        int idx = (int)Math.floor(paths * 0.05);
        r.var95 = finals[idx];
        return r;
    }
}