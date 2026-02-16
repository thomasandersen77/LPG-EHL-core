# Known Limitations

Per spec §14:

- **Single terminal:** One terminal per server instance. Connect@Cloud can deliver messages for multiple TIDs; the server filters by configured `terminalId`.
- **Cloud dependency:** Server requires network connectivity to Connect@Cloud. Network failures map to `vendor_call_failure` (500).
- **No BAXI DLL:** This server does not use `baxi_dotnet.dll` or `baxi.ini`. It is Connect@Cloud only.
- **PCI-grade redaction:** Basic PAN redaction in receipts; not a full PCI DSS solution.
- **No multi-terminal:** Cannot manage multiple terminals in one process.
- **No automatic credential rotation:** Credentials must be updated manually or via config/env refresh.
