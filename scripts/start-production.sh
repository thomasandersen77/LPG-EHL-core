#!/bin/bash
# ═══════════════════════════════════════════════════════════════════════════════
# PRODUKSJONS-OPPSTART for LPG-EHL
# ═══════════════════════════════════════════════════════════════════════════════
#
# Dette skriptet viser BEST PRACTICE for å starte applikasjonen i produksjon.
#
# Konfigurasjon kommer fra:
#   1. application-field.yaml (i JAR) - miljø-defaults
#   2. config/application.yaml       - installasjons-spesifikk config
#   3. Environment variables          - hemmeligheter (DB passwords, etc.)
#   4. Kommandolinje (dette skriptet) - JVM parametere og profil
#
# ═══════════════════════════════════════════════════════════════════════════════

set -e

# ───────────────────────────────────────────────────────────────────────────────
# KONFIGURASJON
# ───────────────────────────────────────────────────────────────────────────────

# Hvilken app skal kjøres? (webapp eller headless)
APP_TYPE="${1:-webapp}"  # Default: webapp

# Hvor ligger JAR-filen?
APP_HOME="/opt/lpg-ehl"  # Endre dette til din installasjonspath
JAR_FILE="$APP_HOME/lpg-ehl-${APP_TYPE}.jar"

# Spring profil (field eller lab)
SPRING_PROFILE="field"

# ───────────────────────────────────────────────────────────────────────────────
# JVM PARAMETERE (Memory Management)
# ───────────────────────────────────────────────────────────────────────────────

# Minne for ARK-3600 eller liten server (512MB max)
JVM_MEMORY="-Xms256m -Xmx512m"

# Minne for større server (1GB max)
# JVM_MEMORY="-Xms512m -Xmx1024m"

# Garbage Collection (G1GC anbefalt for server-applikasjoner)
JVM_GC="-XX:+UseG1GC -XX:MaxGCPauseMillis=200"

# Crash dumps og debugging
JVM_DEBUG="-XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=$APP_HOME/logs/heap-dump.hprof"

# ───────────────────────────────────────────────────────────────────────────────
# ENVIRONMENT VARIABLES (Hemmeligheter - sett via systemd eller .env)
# ───────────────────────────────────────────────────────────────────────────────

# Eksempel - disse bør settes utenfor skriptet i produksjon:
# export DB_PASSWORD="secret-password"
# export AZURE_STORAGE_CONNECTION_STRING="..."

# ───────────────────────────────────────────────────────────────────────────────
# VALIDERING
# ───────────────────────────────────────────────────────────────────────────────

if [[ ! -f "$JAR_FILE" ]]; then
    echo "ERROR: JAR file not found: $JAR_FILE"
    echo ""
    echo "Usage: $0 [webapp|headless]"
    exit 1
fi

if [[ ! -f "$APP_HOME/config/application.yaml" ]]; then
    echo "WARNING: No external config found at $APP_HOME/config/application.yaml"
    echo "         Using defaults from JAR file only."
    echo ""
fi

# ───────────────────────────────────────────────────────────────────────────────
# START APPLIKASJONEN
# ───────────────────────────────────────────────────────────────────────────────

echo "════════════════════════════════════════════════════════════"
echo "  Starting LPG-EHL ($APP_TYPE)"
echo "════════════════════════════════════════════════════════════"
echo ""
echo "  JAR:     $JAR_FILE"
echo "  Profile: $SPRING_PROFILE"
echo "  Memory:  $JVM_MEMORY"
echo "  Config:  $APP_HOME/config/application.yaml"
echo ""
echo "════════════════════════════════════════════════════════════"
echo ""

cd "$APP_HOME"

exec java \
    $JVM_MEMORY \
    $JVM_GC \
    $JVM_DEBUG \
    -Djava.security.egd=file:/dev/./urandom \
    -Dspring.config.additional-location=optional:file:./config/ \
    -jar "$JAR_FILE" \
    --spring.profiles.active=$SPRING_PROFILE

# ───────────────────────────────────────────────────────────────────────────────
# NOTATER
# ───────────────────────────────────────────────────────────────────────────────
#
# For systemd service, lag /etc/systemd/system/lpg-ehl.service:
#
# [Unit]
# Description=LPG EHL Dispenser Control
# After=network.target postgresql.service
#
# [Service]
# Type=simple
# User=lpg
# WorkingDirectory=/opt/lpg-ehl
# Environment="DB_PASSWORD=secret"
# ExecStart=/opt/lpg-ehl/start-production.sh headless
# Restart=always
# RestartSec=10
#
# [Install]
# WantedBy=multi-user.target
#
# ───────────────────────────────────────────────────────────────────────────────
