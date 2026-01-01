# Filmanifest - Terminal Integrasjon

## Oversikt
Samlet: $(date)
Total filer: $(find Terminal -type f | wc -l)

## Struktur

### Markdown-filer (Terminal/markdown/)
$(ls -1 Terminal/markdown/ | wc -l) filer:
$(ls -1 Terminal/markdown/)

### Shell-skript (Terminal/scripts/)
$(ls -1 Terminal/scripts/ | wc -l) filer:
$(ls -1 Terminal/scripts/)

### Kotlin-filer (Terminal/kotlin/)
$(ls -1 Terminal/kotlin/ | wc -l) filer:
$(ls -1 Terminal/kotlin/)

### Konfigurasjon (Terminal/config/)
$(ls -1 Terminal/config/ | wc -l) filer:
$(ls -1 Terminal/config/)

### Docker (Terminal/docker/)
$(ls -1 Terminal/docker/ | wc -l) filer:
$(ls -1 Terminal/docker/)

## Bruksområder

### For ChatGPT/Gemini:
Last opp terminal-kunnskap.zip for å få full kontekst om:
- BAX protokoll implementasjon (NetsBaxProtocol.kt)
- ECR Server implementasjon (EcrServer.kt)
- Payment Terminal integrasjon (PaymentTerminal.kt)
- Test og demo skript
- Implementeringsguider og dokumentasjon

### Viktige filer:
- **NetsBaxProtocol.kt**: BAX protokoll parsing og encoding
- **EcrServer.kt**: ECR server for terminalkommunikasjon
- **PaymentTerminal.kt**: Høynivå terminal API
- **run-ecr-server.sh**: Start ECR server
- **test-ekte-betaling.sh**: Test med ekte terminal
- **PAYMENT_TERMINAL.md**: Komplett guide
- **WARP.md**: Prosjektoversikt

