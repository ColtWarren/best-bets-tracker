# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Best Bets Tracker — a companion application to [sports-betting-analytics](https://github.com/ColtWarren/sports-betting-analytics) that tracks the accuracy and profitability of AI-generated best bet recommendations. The goal is to determine if the AI picks are reliable enough to wager real money on Missouri-legal sportsbooks.

## AI Behavior Rules

- Do not hallucinate APIs, endpoints, or SDKs
- Ask before introducing new dependencies
- Prefer correctness and clarity over cleverness
- Explain reasoning when making architectural changes
- Respect existing package structure
- All monetary values use BigDecimal — never use float/double for money
- All odds are in American format (e.g., +150, -110)

## Development Commands

### Running the Application
```bash
./mvnw spring-boot:run
```
Application runs on http://localhost:8081 (port 8081 to avoid conflict with main app on 8080).

### Building
```bash
./mvnw clean install
```

### Running Tests
```bash
./mvnw test
```

### Running a Single Test
```bash
./mvnw test -Dtest=ClassName#methodName
```

### Cleaning Build Artifacts
```bash
./mvnw clean
```

## Environment Setup

Sensitive configuration is stored in a `.env` file (gitignored). The `spring-dotenv` library (v5.1.0, `springboot4-dotenv` artifact) auto-loads it at startup.

### Required `.env` Variables
```
DB_HOST=localhost
DB_PORT=3306
DB_NAME=best_bets_tracker
DB_USERNAME=root
DB_PASSWORD=root
MAIN_APP_BASE_URL=http://localhost:8080
```

### Active API Keys (wired into application.properties)
```
CLAUDE_API_KEY=       # For future AI-powered accuracy insights
ODDS_API_KEY=         # For fetching closing lines
```
ESPN API is public — no key required.

### Reserved API Keys (in .env for future use)
```
OPENWEATHERMAP_API_KEY, WEATHER_API_KEY, APIFOOTBALL_API_KEY,
CBB_API_KEY, CFB_API_KEY, BALLDONTLIE_API_KEY,
GOOGLE_CLIENT_ID, GOOGLE_CLIENT_SECRET
```

### Database
- MySQL 8.0 on localhost:3306
- Database: `best_bets_tracker` (auto-created via `createDatabaseIfNotExist=true`)
- Schema managed by Hibernate `ddl-auto=update`
- Default credentials: root/root

## Architecture

### Package Structure
```
com.coltwarren.best_bets_tracker/
├── config/
│   ├── WebConfig.java             # CORS for React frontend
│   ├── DataSeeder.java            # Seeds 8 Missouri sportsbooks on startup
│   └── ScheduledJobs.java         # Daily capture, outcome resolution, weekly reports
├── controller/
│   ├── PredictionController.java  # Capture + query predictions
│   ├── OutcomeController.java     # Resolve + query outcomes
│   ├── SnapshotController.java    # Daily snapshot endpoints
│   ├── AnalyticsController.java   # Accuracy & profitability stats
│   ├── SimulationController.java  # Simulated bankroll endpoints
│   └── SportsbookController.java  # Missouri sportsbook CRUD
├── model/
│   ├── Prediction.java            # AI best bet recommendation
│   ├── Outcome.java               # Actual game result
│   ├── DailySnapshot.java         # Day-level pick summary
│   ├── MissouriSportsbook.java    # MO-legal sportsbook reference
│   ├── SimulatedBet.java          # Hypothetical bet for bankroll sim
│   ├── AccuracyReport.java        # Pre-computed accuracy rollups
│   └── enums/
│       ├── BetResult.java         # PENDING, WON, LOST, PUSH, CANCELLED
│       ├── BetType.java           # MONEYLINE, SPREAD, TOTAL_OVER, etc.
│       └── Sport.java             # NFL, NBA, MLB, etc. with ESPN paths
├── repository/
│   ├── PredictionRepository.java  # Win rate, accuracy by sport/type/confidence
│   ├── OutcomeRepository.java     # Profit aggregates, CLV stats
│   ├── DailySnapshotRepository.java
│   ├── MissouriSportsbookRepository.java
│   ├── SimulatedBetRepository.java
│   └── AccuracyReportRepository.java
└── service/
    ├── PredictionCaptureService.java    # Pulls picks from main app API
    ├── OutcomeResolutionService.java    # Checks ESPN for scores, settles bets
    ├── AccuracyAnalyticsService.java    # Computes win rates, ROI, trends
    ├── DailySnapshotService.java        # Snapshot lifecycle & aggregates
    └── SimulationService.java           # Simulated bankroll (flat + Kelly)
```

### Core Data Flow
```
1. PredictionCaptureService.captureToday()
   → GET http://localhost:8080/api/best-bets/all-sports
   → Creates DailySnapshot + Prediction records

2. OutcomeResolutionService.resolveAll()
   → GET https://site.api.espn.com/apis/site/v2/sports/{sport}/scoreboard
   → Matches games by team name, checks STATUS_FINAL
   → Creates Outcome records, updates Prediction status + DailySnapshot tallies

3. SimulationService.settleResolvedBets()
   → Settles SimulatedBets based on resolved Predictions

4. AccuracyAnalyticsService
   → Queries all data for dashboard stats
   → Generates AccuracyReport records for caching
```

### Entity Relationships
```
DailySnapshot  ←──1:N──→  Prediction
Prediction     ←──1:1──→  Outcome
Prediction     ←──1:N──→  SimulatedBet
MissouriSportsbook ←──1:N──→  SimulatedBet
AccuracyReport (standalone, pre-computed rollups)
```

## Key Design Decisions

1. **1-unit flat betting** — all accuracy metrics assume 1 unit per pick for apples-to-apples comparison
2. **4-hour settlement window** — predictions become eligible for ESPN score checks 4 hours after game start
3. **Idempotent capture** — calling capture twice for the same day skips duplicates (matched by homeTeam + awayTeam + selection)
4. **Sport-specific ESPN paths** — each Sport enum maps to its ESPN scoreboard URL segment
5. **Team matching** — case-insensitive partial match (any 3+ char word) to handle name variations between APIs
6. **Confidence tiers** — 6-7 (Low), 7-8 (Medium), 8-9 (High), 9-10 (Elite)

## Scheduled Jobs

| Job | Schedule | Purpose |
|-----|----------|---------|
| `captureDailyBestBets()` | 9:00 AM CT daily | Pull best bets from main app |
| `resolveOutcomes()` | Every 30 minutes | Check ESPN for final scores |
| `generateWeeklyReports()` | 7:00 AM CT Mondays | Generate accuracy reports |

## Main App Integration

This app consumes the main sports-betting-analytics app's API:
- **Endpoint:** `GET /api/best-bets/all-sports`
- **Base URL:** Configured via `MAIN_APP_BASE_URL` env var (default: `http://localhost:8080`)
- **Full context:** See `BEST_BETS_TRACKER_CONTEXT.md` in the sports-betting-analytics repo

## Missouri Legal Sportsbooks (8)

Pre-seeded on startup by DataSeeder:
DraftKings, FanDuel, BetMGM, Caesars Sportsbook, bet365, Fanatics Sportsbook, Circa Sports, theScore Bet

## API Endpoints Summary

### Predictions
- `POST /api/predictions/capture` — Capture today's picks
- `GET /api/predictions` — All predictions (?sport=, ?status=)
- `GET /api/predictions/today` — Today's predictions
- `GET /api/predictions/pending` — Unsettled predictions

### Outcomes
- `POST /api/outcomes/resolve` — Settle via ESPN scores
- `GET /api/outcomes/recent?limit=20` — Recent settled outcomes

### Analytics
- `GET /api/analytics/accuracy` — Overall win rate, ROI, CLV
- `GET /api/analytics/accuracy/by-sport` — Breakdown by sport
- `GET /api/analytics/accuracy/by-type` — Breakdown by bet type
- `GET /api/analytics/accuracy/by-confidence` — Breakdown by confidence tier
- `GET /api/analytics/trend/comparison` — 7/14/30/90-day trends
- `GET /api/analytics/dashboard` — Combined dashboard payload

### Simulation
- `POST /api/simulation/create-bets` — Create simulated bets (?strategy=flat|kelly)
- `GET /api/simulation/bankroll?startingBankroll=1000` — Bankroll summary

### Snapshots
- `GET /api/snapshots` — All daily snapshots
- `GET /api/snapshots/streak` — Current win/loss streak

### Sportsbooks
- `GET /api/sportsbooks/active` — Active Missouri sportsbooks

## Tech Stack

- Java 17
- Spring Boot 4.0.1
- Spring Data JPA / Hibernate
- MySQL 8.0
- Spring WebFlux (WebClient for external API calls)
- Lombok
- spring-dotenv (springboot4-dotenv 5.1.0) for .env file support
- React frontend (planned, in /frontend)

## Important Notes

- Runs on port **8081** (main app uses 8080)
- ESPN API is public and requires no authentication
- The main app must be running for prediction capture to work
- All times in scheduled jobs use Central Time (America/Chicago)
- `.env` file is gitignored — see `.env.example` for required variables
