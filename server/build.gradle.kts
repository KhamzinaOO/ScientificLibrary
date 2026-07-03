import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("java-library")
    id("org.springframework.boot") version "3.1.0"
    id("io.spring.dependency-management") version "1.1.0"
    kotlin("plugin.spring") version "2.1.10"
    kotlin("jvm") version "2.1.10"
    kotlin("plugin.jpa") version "2.1.10"
}



group = "org.olgakhamzina.scientificlibrarythesis"
version = "0.0.1"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

repositories {
    mavenCentral()
}

tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    // Общий код из shared-модуля
    implementation(project(":shared"))

    // Spring Boot Web и JDBC
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-jdbc")
    implementation("com.mysql:mysql-connector-j")

    // Jackson для Kotlin
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

    // Драйвер MySQL на runtime
    runtimeOnly("mysql:mysql-connector-java:8.0.32")

    // Kotlin reflection (нужно Spring-у)
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation(kotlin("stdlib-jdk8"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.4.0")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-web")

    implementation("org.apache.lucene:lucene-analyzers-common:8.11.2")

    testImplementation("org.springframework.boot:spring-boot-starter-test")

    // Selenium — подставьте последнюю стабильную
    testImplementation("org.seleniumhq.selenium:selenium-java:4.9.0")

    // Gatling
    testImplementation("io.gatling:gatling-test-framework:3.9.4")
    testImplementation("io.gatling.highcharts:gatling-charts-highcharts:3.9.4")

    testImplementation("com.h2database:h2:2.1.214")
    testImplementation("org.mockito.kotlin:mockito-kotlin:4.1.0")
}

tasks.test {
    useJUnitPlatform()
    systemProperty("spring.profiles.active", "test")
}

springBoot {
    mainClass.set("org.olgakhamzina.scientificlibrarythesis.server.ServerApplicationKt")
}
