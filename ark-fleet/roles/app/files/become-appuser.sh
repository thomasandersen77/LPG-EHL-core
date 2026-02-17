#!/bin/bash
# Helper script to switch to appuser for testing/debugging
# Place this in /usr/local/bin/become-appuser for easy access

if [ "$EUID" -eq 0 ]; then
    echo "Running as root, switching to appuser..."
    exec su - appuser
else
    echo "Switching to appuser (requires sudo)..."
    exec sudo su - appuser
fi
