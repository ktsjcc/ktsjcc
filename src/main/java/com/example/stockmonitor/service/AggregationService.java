package com.example.stockmonitor.service;

import com.example.stockmonitor.model.StockModels.AnalysisResult;
import com.example.stockmonitor.model.StockModels.SectorHotspot;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class AggregationService {

    public List<SectorHotspot> computeSectorHotspots(Collection<AnalysisResult> analyses) {
        Map<String, SectorHotspot> map = new HashMap<String, SectorHotspot>();
        for (AnalysisResult ar : analyses) {
            String sector = ar.sector != null ? ar.sector : "Unknown";
            SectorHotspot s = map.get(sector);
            if (s == null) {
                s = new SectorHotspot();
                s.sector = sector;
                map.put(sector, s);
            }
            s.losersCount++;
            if (ar.quote != null) {
                s.totalVolume += ar.quote.volume;
                s.averageDropPercent += ar.quote.changePercent;
            }
        }
        List<SectorHotspot> list = new ArrayList<SectorHotspot>(map.values());
        for (SectorHotspot s : list) {
            if (s.losersCount > 0) s.averageDropPercent = s.averageDropPercent / s.losersCount;
        }
        Collections.sort(list, new Comparator<SectorHotspot>() {
            @Override
            public int compare(SectorHotspot a, SectorHotspot b) {
                int cmp = Long.compare(b.totalVolume, a.totalVolume);
                if (cmp != 0) return cmp;
                return Double.compare(a.averageDropPercent, b.averageDropPercent);
            }
        });
        return list;
    }

    public List<AnalysisResult> pickTopByValue(Collection<AnalysisResult> analyses, int limit) {
        List<AnalysisResult> list = new ArrayList<AnalysisResult>(analyses);
        Collections.sort(list, new Comparator<AnalysisResult>() {
            @Override
            public int compare(AnalysisResult a, AnalysisResult b) {
                double sa = a.qualityScore * 0.5 + a.valuationScore * 0.3 + a.momentumScore * 0.2;
                double sb = b.qualityScore * 0.5 + b.valuationScore * 0.3 + b.momentumScore * 0.2;
                return Double.compare(sb, sa);
            }
        });
        if (list.size() > limit) return new ArrayList<AnalysisResult>(list.subList(0, limit));
        return list;
    }
}