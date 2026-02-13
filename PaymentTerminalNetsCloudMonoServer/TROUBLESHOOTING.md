# TROUBLESHOOTING

## Connect@Cloud Error Mapping

### MethodRejected Codes

| Code | Meaning | API Error | HTTP |
|------|---------|-----------|------|
| 7100 | Processing previous command | terminal_busy | 409 |
| 7101 | Unable to process | operation_rejected | 422 |
| 7102 | Already open | invalid_request | 400 |
| 7103 | Not active | terminal_not_ready | 503 |
| 7104 | Terminal busy (ADM) | terminal_busy | 409 |
| 7506 | Invalid OperId | operation_rejected | 422 |
| 7507 | Invalid admin code | operation_rejected | 422 |

### Dfs13Error Codes

| Code | Meaning | API Error |
|------|---------|-----------|
| 8013 | Terminal not found | terminal_not_ready |
| 9100 | Unauthorized / invalid login | vendor_call_failure |
| 9000 | Fatal | vendor_call_failure |

### Network / TLS

- **Connection timeout:** Check firewall, DNS, reachability of Connect@Cloud host
- **TLS handshake failed:** Set `MONO_TLS_PROVIDER=btls` or update Mono/certs
- **WebSocket disconnect:** Adapter will attempt reconnect with exponential backoff

## Priming Behaviour

If `Open` does not receive `Dfs13TerminalReady` or `MethodRejected(7102)` within `openReadyTimeoutSeconds`, the server sends a harmless `lastResult` (AdmCode 12605) to elicit readiness. If still no response, it fails with `terminal_not_ready`.

## References

- Spec: `agent-os/specs/2026-02-11-payment-terminal-nets-cloud-mono-server/spec.md`
- Python sample: `references/nets-cloud/python_ws-sample/cc_ws_json_sample.py`
