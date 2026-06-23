# Warden

[![CI](https://github.com/edward-lcl/warden/actions/workflows/ci.yml/badge.svg)](https://github.com/edward-lcl/warden/actions/workflows/ci.yml)

Autonomous exploit detection plugin for Paper/Spigot Minecraft servers. Warden watches for item
duplication, suspicious inventory activity, and TNT-dupe rigs, logs inventory transactions for
forensic review, and alerts your staff in real time.

## What it does

- **Dupe detection** — tracks per-player item gains in a one-second sliding window and flags anyone
  who exceeds a configurable threshold (item type, count, and location are included in the alert).
- **TNT-dupe detection** — watches explosions for the classic piston + observer signature where a
  TNT block reappears in the same chunk after detonating.
- **Transaction logging** — appends inventory-state changes to a rolling JSON-lines file
  (`plugins/Warden/warden-transactions.jsonl`) for after-the-fact investigation.
- **Admin alerts** — broadcasts to online staff with the `warden.admin` permission and can
  optionally post to a Discord webhook. Alerts are throttled per player per type so staff aren't
  spammed.

## Installation

1. Download the latest `warden-*.jar` from the [Releases](https://github.com/edward-lcl/warden/releases)
   page (or grab the artifact from a CI build).
2. Drop the jar into your server's `plugins/` folder.
3. Restart the server (a full restart is recommended over `/reload`).
4. Edit `plugins/Warden/config.yml` to taste, then run `/warden reload`.

Requires Java 17+ and a Paper/Spigot server on Minecraft 1.20+.

## Configuration

`plugins/Warden/config.yml`:

```yaml
detection:
  tnt-dupe: true          # detect TNT duplication rigs
  carpet-dupe: true       # reserved for carpet/rail dupes
  inventory-clone: true   # reserved for inventory-clone exploits

thresholds:
  max-items-gained-per-second: 64   # flag when a player gains more than this in 1s
  alert-cooldown-seconds: 30        # minimum seconds between repeat alerts per player+type

logging:
  log-to-file: true
  log-file: plugins/Warden/warden.log
  log-transactions: true

alerts:
  notify-ops: true
  discord-webhook: ""     # optional Discord webhook URL for alerts
```

## Commands & permissions

| Command          | Description                                  |
|------------------|----------------------------------------------|
| `/warden status` | Show detection counts and uptime             |
| `/warden reload` | Reload `config.yml`                          |
| `/warden stats`  | Show per-player flag counts                  |

| Permission     | Default | Grants                                  |
|----------------|---------|------------------------------------------|
| `warden.admin` | op      | Access to all `/warden` commands & alerts |

## Reporting issues

Warden is a detection aid, not a ban hammer — it surfaces suspicious activity for **your** staff to
review. False positives are expected on servers with legitimately high item throughput (mob farms,
auto-smelters); tune `max-items-gained-per-second` for your economy before treating a flag as proof.

If you hit a bug or a persistent false positive, please
[open an issue](https://github.com/edward-lcl/warden/issues) and include:

- Your server version (`/version`) and Warden version.
- The relevant lines from `plugins/Warden/warden-transactions.jsonl` and the alert text.
- What the player was actually doing (farm, trade, exploit?).

This context is what makes a report actionable. Issues without it may be closed pending detail.

## Building from source

```bash
./gradlew build      # compiles, runs tests, produces build/libs/warden-*.jar
./gradlew test       # run the unit test suite only
```

## License

See repository for license details.
