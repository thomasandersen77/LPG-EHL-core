# PaymentTerminalNetsCloudMonoServer

This folder will contain a Mono-compatible .NET server that exposes the **same REST API** as `PaymentTerminalMonoServer`, but implements terminal operations via **Nets Connect@Cloud** (REST auth + websocket frontend API) instead of a local BAXI vendor DLL.

Source-of-truth spec lives in:
- `agent-os/specs/2026-02-11-payment-terminal-nets-cloud-mono-server/spec.md`

