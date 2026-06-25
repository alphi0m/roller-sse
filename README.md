# Apache Roller — Sustainable Software Engineering Project

**University of Salerno** — Master's Degree in Software Engineering & IT Management  
**Course**: Sustainable Software Engineering  
**A.Y.**: 2025-2026  
**Author**: Alfio  

---

## Project Overview

This repository contains a sustainability-focused analysis and refactoring of **Apache Roller**, an open-source Java blogging platform (~70,000 LOC). The project applies the three pillars of Sustainable Software Engineering:

- **Technical sustainability** — performance benchmarking (JMH) and tech debt analysis (SonarCloud + Creedengo)
- **Environmental sustainability** — energy profiling (EnergyBridge, EcoIndex/GreenIT) and green refactoring
- **Social sustainability** — algorithmic fairness assessment (AIF360) and explainability

---

## What Was Added

A comment moderation subsystem was introduced under `org.apache.roller.weblogger.moderation`:

| Class | Role |
|-------|------|
| `ModerationDecision` | Outcome of moderation (APPROVED, PENDING, SPAM) with reason |
| `ModerationPolicy` | Interface for pluggable moderation strategies |
| `KeywordModerationPolicy` | Keyword-based filtering with O(1) HashSet lookup |
| `RateLimitModerationPolicy` | Rate limiting per IP/user |
| `CommentModerationService` | Orchestrates policies, logs decisions for explainability |

---

## Green Refactoring Applied

### 1. KeywordModerationPolicy — O(1) HashSet lookup
**Energy smell detected by Creedengo**: Suboptimal Data Structure + Inefficient Loop  
Original implementation used `List<String>` with O(n·m) sequential scan.  
Refactored to `Set<String>` with tokenization and constant-time lookup.  
**Result**: −18.6% CPU energy consumption (EnergyBridge).

### 2. CommentModerationService — Explainability Logging
Added structured SLF4J logging for every moderation decision, recording the exact rule triggered.  
**Result**: full accountability and transparency, zero "black box" effect.

### 3. Jetty Configuration — Network Efficiency
Enabled Gzip compression, Cache-Control headers (`max-age=31536000`), and HTTP/2 support.  
**Result**: reduced payload size and eliminated redundant HTTP requests.

---

## Toolchain

| Pillar | Tool |
|--------|------|
| Energy profiling (backend) | EnergyBridge (RAPL) |
| Energy smells (static) | Creedengo + SonarCloud |
| Frontend eco-analysis | GreenIT Analysis / EcoIndex |
| Performance benchmarks | JMH (Java Microbenchmark Harness) |
| Test coverage | JaCoCo |
| Mutation testing | PiTest |
| Fairness assessment | AIF360 (AI Fairness 360) |
| Containerization | Docker + Docker Compose |
| CI/CD | GitHub Actions |

---

## Key Results

| Metric | Before | After |
|--------|--------|-------|
| CPU Energy (full test cycle) | 1063.97 J | 865.20 J (−18.6%) |
| Statistical Parity Difference | −0.18 | −0.04 ✅ |
| Disparate Impact | 0.74 ❌ | within range ✅ |
| EcoIndex score | A (80.06) | A (improved) |

---

## Test Results

Tests run: 36, Failures: 0, Errors: 0 — BUILD SUCCESS

---

## How to Run

```bash
# Build and run tests
mvn clean test

# Run with Docker
docker-compose up --build
```
---