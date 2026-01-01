#!/bin/bash

echo "Kompilerer..."
mvn -q compile

echo ""
echo "Kjører Bax test mot 192.168.0.4:8009..."
echo ""

java -cp target/classes:$(mvn -q dependency:build-classpath -Dmdep.outputFile=/dev/stdout) \
  no.cloudberries.lpg.payment.DebugBaxTest
