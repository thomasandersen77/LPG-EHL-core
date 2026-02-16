plugins {
    kotlin("jvm") version "1.9.22"
    application
}

group = "no.cloudberries.lpg"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.ktor:ktor-client-core:2.3.7")
    implementation("io.ktor:ktor-client-cio:2.3.7")
    implementation("io.ktor:ktor-client-websockets:2.3.7")
    implementation("io.ktor:ktor-client-content-negotiation:2.3.7")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.7")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("ch.qos.logback:logback-classic:1.4.14")
}

application {
    mainClass.set("TestLoginKt")
}

tasks.register<JavaExec>("testLogin") {
    group = "nets-cloud-testing"
    description = "STEP 1: Test HTTP Login til Nets Cloud Connect"
    mainClass.set("TestLoginKt")
    classpath = sourceSets["main"].runtimeClasspath
}

tasks.register<JavaExec>("testWebSocket") {
    group = "nets-cloud-testing"
    description = "STEP 2: Test WebSocket-tilkobling og Open-kommando"
    mainClass.set("TestWebSocketKt")
    classpath = sourceSets["main"].runtimeClasspath
}

tasks.register<JavaExec>("testOpenTerminal") {
    group = "nets-cloud-testing"
    description = "STEP 3: Test Open Terminal"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("TestOpenTerminalKt")
}

tasks.register<JavaExec>("testPurchase") {
    group = "nets-cloud-testing"
    description = "STEP 4: Test Purchase (1 krone)"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("TestPurchaseKt")
}

tasks.register<JavaExec>("testCompleteFlow") {
    group = "nets-cloud-testing"
    description = "COMPLETE: Open → Purchase (1 krone)"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("TestCompleteFlowKt")
}
