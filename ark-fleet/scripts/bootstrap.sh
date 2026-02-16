#!/usr/bin/env bash
set -euo pipefail

# Bootstrap a fresh Debian host for Ansible management.
#
# This script is intentionally conservative and contains NO real secrets.
# - Adjust ADMIN_USER to your desired admin account
# - Provide authorized_keys via a local file next to this script OR replace the placeholder string below
# - DO NOT commit real keys into git

ADMIN_USER="${ADMIN_USER:-alejandro}"

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
AUTH_KEYS_FILE="${SCRIPT_DIR}/authorized_keys"

# Placeholder key string (replace via env var or file). DO NOT COMMIT REAL KEYS.
EMBEDDED_AUTH_KEY="${EMBEDDED_AUTH_KEY:-ssh-ed25519 AAAAC3NzaC1lZDI1NTE5AAAAIFAKEPLACEHOLDERKEY ark-fleet-placeholder}"

echo "Installing prerequisites..."
export DEBIAN_FRONTEND=noninteractive
apt-get update -y
apt-get install -y --no-install-recommends python3 sudo openssh-server curl

echo "Ensuring SSH is enabled..."
systemctl enable --now ssh

if id -u "${ADMIN_USER}" >/dev/null 2>&1; then
  echo "User ${ADMIN_USER} already exists."
else
  echo "Creating admin user ${ADMIN_USER}..."
  useradd -m -s /bin/bash "${ADMIN_USER}"
fi

echo "Ensuring ${ADMIN_USER} is in sudo group..."
usermod -aG sudo "${ADMIN_USER}"

echo "Installing authorized_keys for ${ADMIN_USER}..."
HOME_DIR="$(getent passwd "${ADMIN_USER}" | cut -d: -f6)"
install -d -m 0700 -o "${ADMIN_USER}" -g "${ADMIN_USER}" "${HOME_DIR}/.ssh"

if [[ -f "${AUTH_KEYS_FILE}" ]]; then
  echo "Using ${AUTH_KEYS_FILE} (local file next to script)."
  install -m 0600 -o "${ADMIN_USER}" -g "${ADMIN_USER}" "${AUTH_KEYS_FILE}" "${HOME_DIR}/.ssh/authorized_keys"
else
  echo "No ${AUTH_KEYS_FILE} found; using embedded placeholder key string."
  echo "${EMBEDDED_AUTH_KEY}" > "${HOME_DIR}/.ssh/authorized_keys"
  chown "${ADMIN_USER}:${ADMIN_USER}" "${HOME_DIR}/.ssh/authorized_keys"
  chmod 0600 "${HOME_DIR}/.ssh/authorized_keys"
fi

echo "Bootstrap complete."

