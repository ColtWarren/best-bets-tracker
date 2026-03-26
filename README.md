# Best Bets Tracker

A companion application to [sports-betting-analytics](https://github.com/ColtWarren/sports-betting-analytics) that tracks the accuracy and profitability of AI-generated best bet recommendations. The goal: determine if the AI picks are reliable enough to wager real money on Missouri-legal sportsbooks.

## How It Works

1. **Capture** — Every day, the tracker pulls AI best bet recommendations from the main app via its internal API
2. **Settle** — Every 30 minutes, it checks ESPN's public scoreboard API for final scores and settles predictions as WON, LOST, or PUSH
3. **Analyze** — The dashboard provides real-time accuracy stats, profitability tracking, and performance trends across sports, bet types, and confidence tiers

## Features

- **Command Center Dashboard** — Win rate, ROI, net units, record, streak, and visual breakdowns at a glance
- **Predictions Browser** — Filter all captured picks by sport and status
- **Accuracy Analysis** — Breakdowns by sport, bet type, and AI confidence tier
- **Profitability Tracking** — Simulated bankroll growth, cumulative P/L charts, and sportsbook-level performance
- **Daily History** — Day-by-day snapshots with win/loss records
- **Missouri Sportsbooks** — Reference for all 8 legally licensed sportsbooks
- **Google OAuth2** — Dashboard protected behind Google login, restricted to a single authorized email
- **Automated Scheduling** — Daily capture (9 AM CT), outcome resolution (every 30 min), weekly reports (Monday 7 AM CT)

## Tech Stack

### Backend
- **Java 17** with **Spring Boot 4.0.1**
- **Spring Data JPA** / Hibernate with **MySQL 8.0**
- **Spring Security** with **Google OAuth2** login
- **Spring WebFlux** (WebClient) for external API calls
- **Lombok** for boilerplate reduction
- **spring-dotenv** for environment variable management

### Frontend
- **React 19** with **Vite 8**
- **react-router-dom v7** for client-side routing
- **Recharts** for data visualization
- **Axios** for API communication
- Dark cyberpunk theme matching the main app

### AI & Data
- **Claude AI** (Anthropic) — Powers the bet analysis and recommendations in the main app; picks are captured and tracked here
- **ESPN API** — Public scoreboard data for automated outcome resolution
- **The Odds API** — Live odds and closing line data
- **Claude Code** — AI pair programmer used to build this entire application

### Infrastructure
- **MySQL 8.0** — Persistent storage for predictions, outcomes, snapshots, and reports
- **Vite Dev Proxy** — Routes API calls through the frontend dev server for seamless OAuth2 session handling

## Architecture

```
Main App (port 8080)                    Best Bets Tracker (port 8081)
+------------------------+              +----------------------------+
|  Sports Betting        |   Internal   |  Spring Boot Backend       |
|  Analytics             |   API Key    |                            |
|                        | <----------> |  PredictionCaptureService  |
|  Claude AI Analysis    |              |  OutcomeResolutionService  |
|  Odds API Integration  |              |  AccuracyAnalyticsService  |
|  Best Bets Engine      |              |  SimulationService         |
+------------------------+              +----------------------------+
                                                    |
                                                    v
                                        +----------------------------+
                                        |  React Frontend (port 5173)|
                                        |                            |
                                        |  Dashboard    Predictions  |
                                        |  Accuracy     Profitability|
                                        |  Snapshots    Sportsbooks  |
                                        +----------------------------+
```

## Data Flow

```
1. PredictionCaptureService.captureToday()
   -> GET main-app/api/best-bets/all-sports (with internal API key)
   -> Dedup against existing predictions (homeTeam + awayTeam + selection)
   -> Creates DailySnapshot + Prediction records

2. OutcomeResolutionService.resolveAll()  (every 30 min)
   -> GET ESPN scoreboard for each sport (with game date parameter)
   -> Matches games by team name, checks STATUS_FINAL
   -> Creates Outcome records, calculates profit units
   -> Updates Prediction status + DailySnapshot tallies

3. React Dashboard
   -> Authenticated via Google OAuth2
   -> Displays accuracy, profitability, and trend analytics
```

## Getting Started

### Prerequisites
- Java 17+
- MySQL 8.0
- Node.js 18+
- The [main app](https://github.com/ColtWarren/sports-betting-analytics) running on port 8080

### Setup

1. Clone the repository
   ```bash
   git clone https://github.com/ColtWarren/best-bets-tracker.git
   cd best-bets-tracker
   ```

2. Copy and configure environment variables
   ```bash
   cp .env.example .env
   # Edit .env with your database credentials, API keys, and Google OAuth2 config
   ```

3. Start the backend
   ```bash
   ./mvnw spring-boot:run
   ```

4. Start the frontend
   ```bash
   cd frontend
   npm install
   npm run dev
   ```

5. Open http://localhost:5173 and sign in with Google

### Google OAuth2 Setup

1. Create OAuth2 credentials in [Google Cloud Console](https://console.cloud.google.com/apis/credentials)
2. Add `http://localhost:5173/login/oauth2/code/google` as an authorized redirect URI
3. Set `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`, and `ALLOWED_EMAIL` in your `.env`

## Missouri Legal Sportsbooks

The tracker monitors picks across all 8 Missouri-licensed sportsbooks:

DraftKings | FanDuel | BetMGM | Caesars Sportsbook | bet365 | Fanatics Sportsbook | Circa Sports | theScore Bet

## License

Private repository. All rights reserved.

---

Built with [Claude Code](https://claude.ai/code) by Colt Warren
