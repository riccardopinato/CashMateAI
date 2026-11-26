# CashMate AI
## ROADMAP.md

Versione roadmap: 1.1

---

# VISION

CashMate AI è un'app Android Premium di gestione finanziaria personale.

Obiettivi del progetto:

- semplicità assoluta
- design premium
- velocità
- offline-first
- cloud sync
- AI solo dove genera reale valore
- costi server minimi
- architettura scalabile

---

# STATO PROGETTO

Legenda

✅ Completato

🚧 In sviluppo

⬜ Pianificato

⏸ Rimandato

---

# PHASE 1
## FOUNDATION

### Core

✅ Architettura MVVM

✅ Repository Pattern

✅ Room

✅ DataStore

✅ Navigation

✅ Material Design 3

✅ Dark Theme

✅ Brand Identity

✅ Hero Cards

✅ Bottom Navigation

---

### UI

✅ Home

✅ Transactions

✅ Analytics

✅ Split Bill

✅ AI Coach

✅ Settings

✅ Onboarding

---

### Premium

✅ RevenueCat Architecture

✅ Premium Manager

✅ Feature Flags

---

### Cloud

✅ Firebase Authentication

✅ Google Sign In (Risolto loop di accesso/blocco UI grazie al fallback Offline automatico)

✅ Firestore

✅ Cloud Sync

✅ Backup & Restore

---

# PHASE 2
## SMART FINANCE

✅ Smart Categories

✅ Automatic Rules

✅ Quick Add

✅ Merchant Learning

✅ Favorite Transactions

✅ Recurring Transactions

✅ Smart Tags

✅ Daily Insight

---

# PHASE 3
## PRODUCTIVITY

✅ Savings Goals

✅ Goal Progress

✅ Achievement

✅ Badge

✅ Weekly Financial Report

✅ Financial Timeline

✅ Goal Dashboard

---

# PHASE 4
## PREMIUM EXPERIENCE

⬜ Android Widget

⬜ Quick Actions

⬜ Dynamic Shortcuts

⬜ Smart Notifications

⬜ Reminder Goals

⬜ Reminder Budget

⬜ Reminder Recurring

---

# PHASE 5
## RELEASE CANDIDATE

✅ Performance Optimization

✅ Compose Optimization

✅ Accessibility

✅ APK Optimization

✅ Material3 Audit

✅ Bug Fixing

✅ Logging

✅ Firebase Analytics

✅ Remote Config

✅ Crash Reporting

✅ Play Store Assets

✅ Privacy Policy

✅ Terms

✅ Licenses

✅ Internal Testing

✅ Closed Beta

✅ Release 1.0 (Version 1.0 RC)

---

# PHASE 6
## POST RELEASE

⬜ Feedback Collection

⬜ UX Improvements

⬜ Bug Fixes

⬜ Version 1.1

---

# V2 ROADMAP

Le seguenti funzionalità sono volutamente escluse dalla versione 1 per contenere i costi server.

⏸ OCR con Gemini Vision

⏸ Split AI

⏸ AI Chat Contestuale

⏸ AI Budget Prediction

⏸ AI Goal Generation

⏸ AI Merchant Recognition

⏸ OCR Products

⏸ Import Bancario

⏸ Open Banking

---

# DESIGN PRINCIPLES

Il Design System è congelato.

Non modificare:

- palette
- typography
- Hero Cards
- radius
- spacing
- glow
- shadows
- bottom navigation

Salvo richiesta esplicita.

---

# ARCHITECTURE PRINCIPLES

Offline First.

Room = database principale.

Firestore = sincronizzazione.

Ogni nuova feature deve essere:

- modulare
- scalabile
- testabile
- cloud ready
- premium ready

---

# PERFORMANCE PRINCIPLES

Prima di usare AI o API esterne verificare sempre se esiste una soluzione locale.

Preferire:

algoritmi

cache

database locale

regole automatiche

machine learning locale

quando possibile.

---

# FUTURE IDEAS

Le idee approvate ma non pianificate sono:

• Smart OCR

• Split AI

• AI Chat

• AI Goal Generator

• AI Budget Advisor

• AI Spending Prediction

• Financial Timeline AI

• Family Shared Wallet

• Web Dashboard

• Desktop Client

---

# ROADMAP RULES

Ogni sprint deve:

1. aggiornare questo file

2. segnare gli step completati

3. aggiungere eventuali nuovi task

4. non eliminare la cronologia

5. mantenere ordine cronologico

6. mantenere compatibilità con gli sprint precedenti
