#!/bin/bash

java -Xms512m -Xmx512m \
  -XX:+UseG1GC -XX:MaxGCPauseMillis=100 \
  -jar release/lpg-ehl-webapp.jar \
  --spring.profiles.active=field \
  --ehl.serial.port=/tmp/vserial1 \
  --ehl.serial.baud-rate=9600 \
  --ehl.serial.data-bits=8 \
  --ehl.serial.parity=NONE \
  --ehl.serial.stop-bits=1
