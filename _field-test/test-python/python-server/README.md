# python-server (EHL RS-485 HTTP server)

Small, **no-pip-deps** HTTP server for controlling / observing an EHL dispenser over RS-485 via curl.

This is **not** a customer UI. It is a field/server tool:
- logs every HTTP request it receives
- logs every frame it sends / receives
- ignores payment entirely (you decide when to call UNBLOCK/BLOCK)

Everything in this folder is self-contained; it imports the existing `python-test/` protocol helpers.

---

## Run

From the repo root:

```bash
cd python-test/python-server
python3 server.py
```

Configuration is read from `config.json` in this folder.

---

## Endpoints (curl)

### Health

```bash
curl -s http://localhost:8080/health | jq .
```

### Read state / volume / price

```bash
curl -s http://localhost:8080/state  | jq .
curl -s http://localhost:8080/volume | jq .
curl -s http://localhost:8080/price  | jq .
```

### Control (dangerous)

```bash
curl -s -X POST http://localhost:8080/unblock | jq .
curl -s -X POST http://localhost:8080/block   | jq .
curl -s -X POST http://localhost:8080/reset   | jq .
```

### Set price (program display)

```bash
curl -s -X POST http://localhost:8080/price \
  -H 'content-type: application/json' \
  -d '{"price":"15.90"}' | jq .
```

### Product select (VB6 default)

This always sends the configured default product byte (typically `0x30`).

```bash
curl -s -X POST http://localhost:8080/product-select | jq .
```

### Start/stop flow (recommended)

Start flow does: `PRODUCT_SELECT -> PROG_PRC -> UNBLOCK -> poll STATE for effect`

```bash
curl -s -X POST http://localhost:8080/flow/start | jq .
curl -s -X POST http://localhost:8080/flow/stop  | jq .
```

---

## Logs

The server writes logs to `python-test/python-server/logs/` with a timestamped filename.

