# Credentials Test Results

## Test 1: Credentials fra Gemini-oppsummering

```
Username: cranberries_shared
Password: Gf&DW*8-IN7Lx6pE
```

**Resultat:** ❌ 401 Unauthorized - Invalid username or password

## Test 2: Credentials fra server.json (Thomas sitt repo)

```
Username: cloudberries_shared
Password: B8PnVjmVq-SMM9QD
```

**Resultat:** ✅ 200 OK - Login successful!

**Token mottatt:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "username": "cloudberries_shared",
  "terminals": ["42696609"]
}
```

---

## Observasjoner

1. **Usernames er forskjellige:**
   - E-post: `cranberries_shared`
   - server.json: `cloudberries_shared`

2. **KONKLUSJON:**
   - ✅ `cloudberries_shared` er RIKTIG
   - ❌ `cranberries_shared` var skrivefeil i e-posten

3. **WebSocket-testing:**
   - ✅ WebSocket-tilkobling fungerer
   - ✅ JSON-format fungerer
   - ❌ Terminal 42696609 er OFFLINE (ErrorCode 8013)

## 🎯 Konklusjon

**KORREKTE CREDENTIALS:**
```
Username: cloudberries_shared
Password: B8PnVjmVq-SMM9QD
Terminal ID: 42696609
```

**Status:**
- HTTP Login: ✅ Fungerer
- WebSocket: ✅ Fungerer  
- JSON Protocol: ✅ Fungerer
- Terminal: ❌ Offline (må konfigureres av Nets)

**Se full rapport:** `NETS_CLOUD_CONNECT_TESTING_REPORT.md`

---

## Terminal Info

Fra e-post:
- Terminal ID: BAX-1229329
- Type: PROD terminal (EKTE PENGER!)
- Status: Skal konfigureres av Nets

Konfigurasjon som skal settes på terminalen:
- Host IP: 91.102.24.142
- Host Port: 9670
- ECR IP: 3.33.230.243 / 15.197.206.182
- ECR Port: 6001
