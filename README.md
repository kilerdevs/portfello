# Portfello

Android portfolio tracker for stocks, crypto, precious metals, retail bonds, and foreign currencies — all valued in a single base currency in real time.

## Features

- **Multi-asset valuation** — stocks (Yahoo Finance / Stooq), crypto (CoinGecko), FX (NBP / Frankfurter), bullion (spot gold/silver × weight × purity), Polish retail bonds (accrued-interest formula), manual assets
- **Encrypted at rest** — SQLCipher AES-256 database; key derived from PIN via Argon2id (64 MB memory cost, 3 iterations)
- **Background sync** — WorkManager `CoroutineWorker` refreshes prices and snapshots portfolio value on a configurable schedule
- **Price caching** — in-memory TTL cache sits in front of all network clients to avoid redundant API calls
- **Portfolio history** — timestamped `PortfolioSnapshot` rows power the value-over-time chart (Vico)
- **Onboarding + lock screen** — first-launch PIN setup; subsequent launches require PIN to decrypt the database

## Tech stack

| Layer | Technology |
|---|---|
| UI | Jetpack Compose · Material 3 · Navigation Compose |
| Architecture | MVVM · Repository · Domain (ValuationEngine) |
| DI | Hilt (KSP code generation) |
| Persistence | Room 2.6 · SQLCipher 4.6 |
| Security | Argon2id (argon2kt) · Android Keystore |
| Network | OkHttp 4 · Moshi (KSP codegen) |
| Async | Kotlin Coroutines · Flow |
| Background | WorkManager · HiltWorker |
| Charts | Vico 2 |
| Tests | JUnit 4 · MockK · kotlinx-coroutines-test |
| Build | Gradle 8 · KSP · R8 (release minification) |

## Architecture

```
UI (Compose screens + ViewModels)
        ↓ StateFlow
Domain (ValuationEngine, BondRetailCalculator, BullionValuator, CurrencyConverter)
        ↓
Repository (AssetRepository, PriceRepository)
        ↓                        ↓
Room + SQLCipher          Network clients (CoinGecko, Yahoo, Stooq, NBP, Frankfurter)
```

`ValuationEngine` dispatches per asset type, converts every result to the user's base currency, and falls back to the last cached price snapshot on network failure.

## Price sources

| Asset type | Source |
|---|---|
| Stocks (PL) | Stooq |
| Stocks (global) | Yahoo Finance |
| Crypto | CoinGecko |
| FX (PLN cross-rates) | NBP Table A |
| FX (other) | Frankfurter (ECB) |
| Bullion | spot price × troy-oz conversion × coin purity |

## Security model

1. On first launch the user sets a PIN.
2. PIN → Argon2id → 32-byte key stored as hash + salt in `crypto_config.json`.
3. The same derived key is passed to SQLCipher as the database encryption key.
4. On lock, the key is zeroed in memory (`ByteArray.fill(0)`).
5. The database is inaccessible without a correct PIN — no plaintext fallback.

## Building

```bash
./gradlew assembleDebug
./gradlew test          # unit tests (no emulator needed)
```

Requires Android SDK 26+. No API keys needed — all price sources are public.
