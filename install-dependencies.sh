#!/usr/bin/env bash
set -euo pipefail

# install-dependencies.sh
# Installs dependencies needed to build + run the FELTESTING.md workflow:
# - Java 21 (required by pom.xml: maven.compiler.release=21)
# - Node.js + npm (required by build_monolith.sh to build lpg-web and bundle static assets)
# - Common field/serial utilities (curl, lsusb, socat/minicom, etc.)
#
# Supported:
# - Debian 12 (Bookworm)
# - macOS (Homebrew)

log() { printf '%s\n' "$*"; }
warn() { printf 'WARN: %s\n' "$*" >&2; }
die() { printf 'ERROR: %s\n' "$*" >&2; exit 1; }

need_cmd() {
  command -v "$1" >/dev/null 2>&1 || die "Missing required command: $1"
}

major_version() {
  # Extract the first integer from a version string
  # e.g. "v20.11.1" -> "20", "21.0.2" -> "21"
  sed -E 's/[^0-9]*([0-9]+).*/\1/' <<<"${1:-}"
}

java_major() {
  if ! command -v java >/dev/null 2>&1; then
    echo ""
    return 0
  fi
  # java -version prints to stderr
  local v
  v="$(java -version 2>&1 | head -n1 | sed -E 's/.* version "([^"]+)".*/\1/')"
  major_version "$v"
}

node_major() {
  if ! command -v node >/dev/null 2>&1; then
    echo ""
    return 0
  fi
  major_version "$(node -v 2>/dev/null || true)"
}

require_sudo() {
  if [[ "${EUID:-$(id -u)}" -eq 0 ]]; then
    return 0
  fi
  need_cmd sudo
  sudo -v
}

install_java21_debian() {
  local jm
  jm="$(java_major)"
  if [[ "$jm" == "21" ]]; then
    log "Java 21 already present."
    return 0
  fi

log "Installing Java 21 (Debian)..."
  require_sudo

  # Try native packages first (including backports if configured)
  if apt-cache policy openjdk-21-jdk 2>/dev/null | grep -q "Candidate:" && \
     ! apt-cache policy openjdk-21-jdk 2>/dev/null | grep -q "Candidate: (none)"; then
    sudo apt-get install -y openjdk-21-jdk || true
  fi

  jm="$(java_major)"
  if [[ "$jm" == "21" ]]; then
    return 0
  fi

  # Enable bookworm-backports if needed, then retry
  local codename="bookworm"
  if [[ -r /etc/os-release ]]; then
    # shellcheck disable=SC1091
    . /etc/os-release
    codename="${VERSION_CODENAME:-bookworm}"
  fi

  if [[ ! -f "/etc/apt/sources.list.d/${codename}-backports.list" ]]; then
    log "Adding Debian backports source: ${codename}-backports"
    echo "deb http://deb.debian.org/debian ${codename}-backports main" | sudo tee "/etc/apt/sources.list.d/${codename}-backports.list" >/dev/null
  fi

  sudo apt-get update -y
  sudo apt-get install -y -t "${codename}-backports" openjdk-21-jdk || true

  jm="$(java_major)"
  if [[ "$jm" == "21" ]]; then
    return 0
  fi

  # Fallback: Adoptium (Temurin) repo
  log "openjdk-21-jdk not available; installing Temurin 21 from Adoptium repo..."
  sudo apt-get install -y ca-certificates curl gnupg
  sudo install -d -m 0755 /etc/apt/keyrings
  curl -fsSL https://packages.adoptium.net/artifactory/api/gpg/key/public | sudo gpg --dearmor -o /etc/apt/keyrings/adoptium.gpg
  echo "deb [signed-by=/etc/apt/keyrings/adoptium.gpg] https://packages.adoptium.net/artifactory/deb ${codename} main" | sudo tee /etc/apt/sources.list.d/adoptium.list >/dev/null
  sudo apt-get update -y
  sudo apt-get install -y temurin-21-jdk

  jm="$(java_major)"
  [[ "$jm" == "21" ]] || die "Java 21 install failed (java -version not reporting 21)."
}

install_node20_debian() {
  local nm
  nm="$(node_major)"
  if [[ -n "$nm" ]] && (( nm >= 20 )); then
    log "Node.js $nm already present."
    return 0
  fi

log "Installing Node.js 20.x (Debian, via NodeSource)..."
  require_sudo
  sudo apt-get install -y ca-certificates curl gnupg
  curl -fsSL https://deb.nodesource.com/setup_20.x | sudo -E bash -
  sudo apt-get install -y nodejs

  nm="$(node_major)"
  [[ -n "$nm" ]] && (( nm >= 20 )) || die "Node.js install failed (need >= 20)."
  command -v npm >/dev/null 2>&1 || die "npm not found after installing Node.js."
}

install_debian_packages() {
  require_sudo
  sudo apt-get update -y

  # Note:
  # - build-essential/python3 are here because some npm deps may require node-gyp.
  # - socat/minicom are not strictly required for FIELD mode, but are useful in the same workflow (SOCAT mode / troubleshooting).
  sudo apt-get install -y \
    bash \
    ca-certificates \
    curl \
    git \
    gnupg \
    lsof \
    procps \
    usbutils \
    unzip \
    zip \
    build-essential \
    python3 \
    socat \
    minicom \
    setserial

  # Common runtime dependency for some serial stacks (harmless if already installed)
  sudo apt-get install -y libudev1 || true
}

ensure_dialout_membership() {
  # Allows opening /dev/ttyS* and /dev/ttyUSB* without chmod 666.
  if ! getent group dialout >/dev/null 2>&1; then
    warn "Group 'dialout' not found; skipping serial permission setup."
    return 0
  fi

  local user="${SUDO_USER:-${USER:-}}"
  if [[ -z "$user" ]]; then
    warn "Could not determine user for dialout group; skipping."
    return 0
  fi

  if id -nG "$user" | tr ' ' '\n' | grep -qx dialout; then
    log "User '$user' is already in dialout."
    return 0
  fi

  require_sudo
  sudo usermod -a -G dialout "$user"
  warn "Added '$user' to dialout. You must logout/login (or reboot) for it to take effect."
}

install_macos_deps() {
  need_cmd uname
  if ! command -v brew >/dev/null 2>&1; then
    die "Homebrew is required on macOS. Install it from https://brew.sh/ then re-run this script."
  fi

  log "Installing dependencies via Homebrew (macOS)..."
  brew update

  # Build + runtime basics
  brew install git node@20
  brew install socat minicom || true

  # Java 21 (Temurin)
  brew install --cask temurin@21

  # Helpful hint for shells that don't auto-detect Java 21
  if /usr/libexec/java_home -v 21 >/dev/null 2>&1; then
    log "JAVA_HOME for Java 21 is: $(/usr/libexec/java_home -v 21)"
  else
    warn "macOS java_home did not find Java 21. You may need to reopen your terminal."
  fi

  local nm
  nm="$(node_major)"
  if [[ -z "$nm" ]] || (( nm < 20 )); then
    warn "Node is installed but 'node' on PATH is not >=20. You may need to add node@20 to PATH."
    warn "Common fix (zsh): echo 'export PATH=\"$(brew --prefix)/opt/node@20/bin:\$PATH\"' >> ~/.zshrc && source ~/.zshrc"
  fi
}

main() {
  local os
  os="$(uname -s)"

  case "$os" in
    Linux)
      if [[ -r /etc/os-release ]]; then
        # shellcheck disable=SC1091
        . /etc/os-release
        if [[ "${ID:-}" != "debian" || "${VERSION_CODENAME:-}" != "bookworm" ]]; then
          warn "Detected Linux: ID='${ID:-?}', CODENAME='${VERSION_CODENAME:-?}'. Script is intended for Debian Bookworm."
        fi
      else
        warn "Could not read /etc/os-release; assuming Debian-like system."
      fi

      install_debian_packages
      install_java21_debian
      install_node20_debian
      ensure_dialout_membership

      log ""
      log "Installed. Next steps (from FELTESTING.md):"
      log "  - Build: ./build_monolith.sh --skip-tests"
      log "    (If you insist on running tests, you will likely need Docker for Testcontainers.)"
      log "  - Then: cp application-h2.yaml release/ && cd release/"
      log "  - Run headless (example):"
      log "      java -jar lpg-ehl-headless.jar --spring.config.location=file:./application-h2.yaml --spring.profiles.active=h2,debug-api --ehl.serial.port=/dev/ttyS1"
      log "  - List ports: ls -l /dev/ttyS* /dev/ttyUSB* ; lsusb"
      ;;

    Darwin)
      install_macos_deps
      log ""
      log "Installed. Next steps (from FELTESTING.md):"
      log "  - Build: ./build_monolith.sh --skip-tests"
      log "  - Note: the RS-485 dongle will show up as /dev/cu.* or /dev/tty.* on macOS."
      log "    List ports: ls -l /dev/cu.* /dev/tty.*"
      ;;

    *)
      die "Unsupported OS: $os"
      ;;
  esac
}

main "$@"

