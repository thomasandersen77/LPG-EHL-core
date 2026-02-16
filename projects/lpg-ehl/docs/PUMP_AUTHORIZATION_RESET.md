### Bakgrunn
Kontrollpanelet har en reset-funksjon på `/control` som kaller backend for å rydde opp i pumper og betaling. I dag blir transaksjoner markert som betalt, men autorisasjoner i tabellen `pump_authorization` kan bli stående som `STOPPED`. Siden `STOPPED` regnes som aktiv status, blokkeres ny fylling selv om betaling er simulert.

### Hva som skjer i dag
1. **Betaling via kontrollpanelet**
   - `TransactionController` oppdaterer `transactions.payment_status` til `PAID`.
   - `pump_authorization.status` oppdateres kun dersom betaling går via `PumpController.confirmPayment` og autorisasjonen allerede er `STOPPED`.
   - Hvis betaling skjer via transaksjons-endepunkter, blir `STOPPED` stående og ny kortdragning blokkeres.
2. **Full reset via kontrollpanelet**
   - `AdminController.fullSystemReset` markerer `transactions` som `PAID` og kansellerer stuck-autorisasjoner.
   - `STOPPED` autorisasjoner blir ikke fullført (`COMPLETED`) og blir derfor igjen som aktive.

### Foreslått løsning
1. **Koble transaksjon og autorisasjon tettere**
   - Når `UNBLOCK` er bekreftet, settes `pump_authorization.transaction_id` ved å sende `transactionId` inn i `markPumping`.
2. **Oppdater `pump_authorization` ved betaling**
   - Når `transactions.payment_status` settes til `PAID`, forsøkes det å fullføre tilhørende `STOPPED` autorisasjon via `transactionId` (og fallback på dispenser).
3. **Fullfør `STOPPED` autorisasjoner ved reset**
   - `AdminController.fullSystemReset` og `mark-all-paid` fullfører alle `STOPPED` autorisasjoner før kansellering av stuck-autorisasjoner.

### Forventet effekt
- `STOPPED` autorisasjoner kvitteres ut til `COMPLETED` når betaling simuleres.
- Full reset rydder også opp i `STOPPED`, slik at nye fyllinger ikke blokkeres.
- Kontrollpanelet får konsistent oppførsel mellom transaksjoner og autorisasjonstabell.