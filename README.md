# US Stocks Real-time Drop Monitor (Java 8)

A Java 8 Spring Boot app that monitors US stocks in real-time, screens large drops for quality stocks, computes technicals (RSI/ATR/OBV), fetches Fear & Greed index, and provides trade plan suggestions. Built with Maven and a minimal dashboard UI.

## Run

- Java 8 and Maven required
- Configure optional API keys via env vars (if available): `ALPHAVANTAGE_API_KEY`, `FINNHUB_API_KEY`
- Build and run:

```
mvn spring-boot:run
```

Then open `http://localhost:8080`.

## Notes

- Data via Yahoo Finance public endpoints; rate-limited: app limits symbols per cycle and caches responses.
- Scheduler runs only during US market hours (9:30-16:00 ET) by default.
