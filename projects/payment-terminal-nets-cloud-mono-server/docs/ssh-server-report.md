# SSH Server Discovery Report

**Host:** saklink.tplinkdns.com:2222
**User:** thomas
**Date:** 2026-02-10

---

## System Information

| Property | Value |
|----------|-------|
| Hostname | debian |
| OS | Linux debian 6.1.0-42-amd64 x86_64 |
| Kernel | #1 SMP PREEMPT_DYNAMIC Debian 6.1.159-1 (2025-12-30) |
| Architecture | x86_64, GNU/Linux |

---

## Network Interfaces

### lo (loopback)
- **IPv4:** 127.0.0.1/8
- **Status:** UP

### enp0s25 (primary ethernet)
- **IPv4:** 192.168.1.190/24
- **MAC:** 00:1e:c9:59:f5:98
- **Status:** UP

### ens32
- **IPv4:** 192.168.50.1/24
- **Status:** UP

### enp1s7
- **Status:** DOWN (no IP assigned)

---

## Listening Ports (ss -tlnp)

| Port | Bind Address | Protocol | Process |
|------|-------------|----------|---------|
| 22 | 0.0.0.0 / [::] | TCP | sshd |
| 6001 | 0.0.0.0 | TCP | Unknown |
| 18080 | 127.0.0.1 | TCP | Mono-HTTPAPI/1.0 |

---

## Curl Request & Response Results

### Port 80 — HTTP (localhost)
- **Request:** `curl -s http://localhost`
- **Response:** Connection refused — no web server on port 80

### Port 443 — HTTPS (localhost)
- **Request:** `curl -sk https://localhost`
- **Response:** Connection refused — no HTTPS server on port 443

### Port 3000
- **Request:** `curl -s http://localhost:3000`
- **Response:** Connection refused — no service

### Port 5000
- **Request:** `curl -s http://localhost:5000`
- **Response:** Connection refused — no service

### Port 8080
- **Request:** `curl -s http://localhost:8080`
- **Response:** Connection refused — no service

### Port 8443
- **Request:** `curl -sk https://localhost:8443`
- **Response:** Connection refused — no service

### Port 9090
- **Request:** `curl -s http://localhost:9090`
- **Response:** Connection refused — no service

### Port 6001 (Discovered open)
- **Request:** `curl -s http://localhost:6001`
- **Response:** Empty reply from server — port is open but does not speak HTTP

### Port 18080 (Discovered open — Mono/.NET server)
- **Request:** `curl -s http://localhost:18080`
- **Response:** `Bad Request (Invalid host)` — Server: Mono-HTTPAPI/1.0
- **Request:** `curl -s http://localhost:18080/api`
- **Response:** `Bad Request (Invalid host)` — same response
- **Request:** `curl -s http://localhost:18080/health`
- **Response:** `Bad Request (Invalid host)` — same response
- **Notes:** The Mono HTTP API server rejects requests likely because it requires a specific `Host` header. The process running is `payment-terminal-mono-server.exe ./server.json`.

---

## Running Services (systemd)

| Service | Status |
|---------|--------|
| cron.service | running |
| dbus.service | running |
| dnsmasq.service | running |
| getty@tty1.service | running |
| ssh.service | running |
| systemd-journald.service | running |
| systemd-logind.service | running |
| systemd-networkd.service | running |
| systemd-timesyncd.service | running |
| systemd-udevd.service | running |
| user@1000.service | running |
| user@1001.service | running |

---

## Docker

Docker is **not installed** or the current user does not have access.

---

## Key Findings

1. **Debian server** running kernel 6.1.0-42 on x86_64 hardware with two active network interfaces (192.168.1.190 and 192.168.50.1).
2. **SSH (port 22)** is the primary accessible service (exposed externally on port 2222).
3. **Port 6001** is listening but does not respond to HTTP — likely a custom binary protocol (possibly related to the payment terminal).
4. **Port 18080** runs a **Mono/.NET application** (`payment-terminal-mono-server.exe`) with configuration loaded from `server.json`. It requires a specific `Host` header to accept connections, returning "Bad Request (Invalid host)" for localhost requests.
5. **No standard web servers** (nginx, Apache, etc.) are running — ports 80, 443, 3000, 5000, 8080, 8443, 9090 are all closed.
6. **dnsmasq** is running, suggesting this server may act as a DNS/DHCP server for the 192.168.50.0/24 subnet.
7. Two user sessions are active (UID 1000 and 1001).
