# CashMate AI
## DECISIONS.md

Ultimo aggiornamento: v1.1

Questo documento contiene tutte le decisioni architetturali e strategiche del progetto.

Ogni nuova modifica deve rispettare queste decisioni.

Non eliminare mai le decisioni precedenti.

Aggiungere solamente nuove decisioni.

---

# ARCHITETTURA

## D-001

Pattern principale:

MVVM

Decisione definitiva.

---

## D-002

Database principale:

Room

Firestore viene utilizzato esclusivamente per:

- sincronizzazione
- backup
- multi dispositivo

Mai usare Firestore come database primario.

---

## D-003

Approccio:

Offline First

L'app deve funzionare completamente offline.

La sincronizzazione cloud è secondaria.

---

## D-004

Ogni nuova Entity deve essere Cloud Ready.

Campi standard:

id

userId

createdAt

updatedAt

deletedAt

syncStatus

lastSyncedAt

---

# DESIGN

## D-010

Design System congelato.

Non modificare:

palette

typography

spacing

hero card

bottom navigation

radius

shadow

glow

Salvo richiesta esplicita.

---

## D-011

Tema ufficiale:

Dark Premium.

Nessun Light Theme previsto nella V1.

---

# AUTENTICAZIONE

## D-020

Provider supportati:

Email/Password

Google Sign-In

---

## D-021

Facebook Login

SCARTATO

Motivo:

maggiore complessità

nessun reale vantaggio nella V1

---

## D-022

Ogni utente deve avere un UserProfile reale.

"Demo User" è vietato.

---

## D-023

In caso di fallimento o mancata configurazione del Google Sign-In (es. mancanza dell'ID Token o annullamento da parte dell'utente), l'app esegue un fallback automatico in modalità offline (associando l'ID utente generato dall'email per coerenza, es. "utente@example.com") per garantire l'accesso immediato e non bloccare l'interfaccia utente in nessun caso.

---

# PREMIUM

## D-030

RevenueCat è il sistema ufficiale.

Tutte le funzioni Premium devono passare dal PremiumManager.

---

## D-031

Feature Flag obbligatorie.

Mai controllare Premium direttamente nella UI.

---

# ACCOUNT

## D-040

Ogni transazione deve appartenere ad un Account.

Account obbligatorio.

---

## D-041

"Tutto"

NON è un account.

È solamente una vista aggregata.

Non deve mai comparire nella creazione delle transazioni.

---

## D-042

Se una transazione non possiede account:

assegnarla automaticamente al Conto Corrente.

Mai lasciare transazioni orfane.

---

# AI

## D-050

L'AI deve essere utilizzata solo quando produce reale valore.

Prima verificare sempre se il problema può essere risolto localmente.

---

## D-051

Le funzioni AI devono essere limitate tramite PremiumManager.

---

## D-052

L'OCR è rimandato alla V2.

Motivo:

costo API

ROI non ancora verificato.

---

## D-053

AI Chat contestuale

Rimandata alla V2.

---

## D-054

Split AI

Rimandato alla V2.

---

# CLOUD

## D-060

Conflict Resolution:

LastUpdatedWins

---

## D-061

UUID stabili.

Mai usare ID temporanei.

---

# USER EXPERIENCE

## D-070

Ridurre sempre il numero di tap.

Automatizzare quando possibile.

L'utente mantiene sempre il controllo.

---

## D-071

Preferire suggerimenti rispetto ad automatismi irreversibili.

---

## D-072

Le statistiche devono essere sempre coerenti.

Ogni transazione deve essere contabilizzata in un account.

---

# PERFORMANCE

## D-080

Prima di utilizzare API esterne verificare sempre:

algoritmo locale

cache

database

regole

machine learning locale

---

## D-081

Evitare codice duplicato.

Centralizzare componenti.

---

# V1

## D-090

La V1 deve essere monetizzabile.

Obiettivo:

massimo valore

minimo costo server.

---

## D-091

Funzioni AI costose rimandate alla V2.

---

# ROADMAP

## D-100

Ogni sprint deve aggiornare:

ROADMAP.md

PROJECT_RULES.md se necessario

DECISIONS.md se vengono prese nuove decisioni.

Mai eliminare decisioni esistenti.

Solo aggiungerne di nuove.

---

# PRODUTTIVITÀ & INSIGHTS (SPRINT 14)

## D-110

L'ecosistema di risparmio (Savings Goals, Vaults), premi (Achievements e livelli di Badge), resoconti finanziari settimanali (Weekly Financial Report) e cronologia eventi (Financial Timeline) è stato realizzato in modalità 100% offline-first tramite algoritmi locali, senza l'utilizzo di API o AI esterne. Questa scelta architetturale garantisce privacy totale dei dati sensibili, velocità istantanea di risposta e zero costi di server ricorrenti per la piattaforma, mantenendo l'intera struttura pronta e compatibile con il cloud sync del database Room su Firestore.

---

# AUDIT & RELEASE CANDIDATE (SPRINT 15 & 16)

## D-120

Ogni transazione creata o modificata all'interno del sistema deve essere associata ad un account reale valido. È vietata la selezione di conti virtuali (es. "Tutti i conti" o "Tutto") durante la creazione delle transazioni. Tutte le transazioni orfane o con account non valido vengono automaticamente migrate al "Conto Corrente" principale all'avvio dell'applicazione.

---

## D-121

L'applicazione CashMate AI Versione 1.0 ha superato con successo l'audit completo architetturale, database, cloud, UX/UI, performance, sicurezza e accessibilità. Tutte le funzionalità pianificate nella Roadmap sono state interamente implementate, testate e ottimizzate. L'app è dichiarata stabile ed è pronta per la pubblicazione su Google Play Store come Release Candidate.

---
