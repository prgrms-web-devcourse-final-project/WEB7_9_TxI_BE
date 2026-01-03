plugins {
    java
    id("org.springframework.boot") version "3.5.8"
    id("io.spring.dependency-management") version "1.1.7"
    checkstyle
    jacoco
}
val springCloudVersion by extra("2025.0.1")

group = "com"
version = "0.0.1-SNAPSHOT"
description = "backend"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-web")

    implementation("org.springframework.boot:spring-boot-starter-batch")
    testImplementation("org.springframework.batch:spring-batch-test")

    implementation("org.springframework.boot:spring-boot-starter-security")
    testImplementation("org.springframework.security:spring-security-test")

    compileOnly("org.projectlombok:lombok")
    developmentOnly("org.springframework.boot:spring-boot-devtools")
    runtimeOnly("com.h2database:h2")
    runtimeOnly("com.mysql:mysql-connector-j")
    annotationProcessor("org.projectlombok:lombok")

    // test
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("net.datafaker:datafaker:2.3.1")
    testImplementation("com.github.codemonstur:embedded-redis:1.4.3")


    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.13")
    implementation("io.jsonwebtoken:jjwt-api:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.6")

    implementation("org.postgresql:postgresql:42.7.8")

    //redis
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    testImplementation("com.github.codemonstur:embedded-redis:1.4.3")

    implementation("org.springframework.boot:spring-boot-starter-websocket")

    // actuator, micrometer
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("io.micrometer:micrometer-registry-prometheus")

    //coolsms
    implementation("net.nurigo:sdk:4.3.0")

    //querydsl
    implementation("com.querydsl:querydsl-jpa:5.1.0:jakarta")
    annotationProcessor("com.querydsl:querydsl-apt:5.1.0:jakarta")
    annotationProcessor("jakarta.annotation:jakarta.annotation-api")
    annotationProcessor("jakarta.persistence:jakarta.persistence-api")

    // logstash logback encoder
    implementation("net.logstash.logback:logstash-logback-encoder:7.4")

    //s3
    implementation("software.amazon.awssdk:s3:2.40.13")

    // ShedLock (스케줄러 중복 실행 방지)
    implementation("net.javacrumbs.shedlock:shedlock-spring:5.10.0")
    implementation("net.javacrumbs.shedlock:shedlock-provider-redis-spring:5.10.0")

    // flyway
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")

    // resilience4j
    implementation("org.springframework.cloud:spring-cloud-starter-circuitbreaker-resilience4j")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.compilerArgs.add("-parameters")
}

tasks.withType<org.gradle.api.plugins.quality.Checkstyle>().configureEach {
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}

checkstyle {
    toolVersion = "10.12.5"  // 최신 안정 버전으로 업그레이드
    configFile = rootProject.file("config/checkstyle/naver-checkstyle-rules.xml")
    configProperties = mapOf(
            "suppressionFile" to rootProject
                    .file("config/checkstyle/naver-checkstyle-suppressions.xml")
                    .absolutePath
    )
    isIgnoreFailures = false  // 명시적으로 설정
}

/** -----------------------------
 *  JaCoCo Configuration
 *  ----------------------------- */
jacoco {
    toolVersion = "0.8.12" // Java 21 호환
}


/** 공통 커버리지 제외 패턴 */
val coverageExcludes = listOf(
        "**/*Application*",
        "**/config/**",
        "**/dto/**",
        "**/exception/**",
        "**/response/**",
        "**/repository/**",
        "**/init/**",
        "**/error/**",
        "**/entity/**",
        "**/home/**",
        "**/vo/**",
        "**/Q*.*",
        "**/controller/**/*Api.class",
        "**/controller/**/*Api\$*"
)

/** -----------------------------
 *  Test 공통 설정 (로깅/요약/실패수집)
 *  ----------------------------- */
tasks.withType<Test>().configureEach {
    useJUnitPlatform()

    testLogging {
        events("PASSED", "FAILED", "SKIPPED")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        showExceptions = true
        showCauses = true
        showStackTraces = true
        showStandardStreams = true
    }

    // 각 Test task의 JaCoCo 실행파일 경로를 명시
    extensions.configure(org.gradle.testing.jacoco.plugins.JacocoTaskExtension::class) {
        val execName = if (name == "test") "test.exec" else "${name}.exec"
        setDestinationFile(layout.buildDirectory.file("jacoco/$execName").get().asFile)
    }


    val failed = mutableListOf<Triple<String, String, String?>>() // class, method, msg
    addTestListener(object : org.gradle.api.tasks.testing.TestListener {
        override fun beforeSuite(suite: TestDescriptor) {}
        override fun beforeTest(testDescriptor: TestDescriptor) {}
        override fun afterTest(desc: TestDescriptor, result: TestResult) {
            if (result.resultType == TestResult.ResultType.FAILURE) {
                val clazz = desc.className ?: "(unknown-class)"
                val method = desc.name
                val msg = result.exception?.message?.lineSequence()?.firstOrNull()
                failed += Triple(clazz, method, msg)
            }
        }

        override fun afterSuite(suite: TestDescriptor, result: TestResult) {
            if (suite.parent == null) {
                println(
                        """
                    ------------------------
                    ✅ TEST RESULT SUMMARY
                    Total tests : ${result.testCount}
                    Passed      : ${result.successfulTestCount}
                    Failed      : ${result.failedTestCount}
                    Skipped     : ${result.skippedTestCount}
                    ------------------------
                    """.trimIndent()
                )
                val out = layout.buildDirectory.file("reports/tests/failed-tests.txt").get().asFile
                out.parentFile.mkdirs()
                if (failed.isNotEmpty()) {
                    val RED = "\u001B[31m"
                    val RESET = "\u001B[0m"
                    println("❌ FAILED TESTS (${failed.size})")
                    failed.forEachIndexed { i, (c, m, msg) ->
                        println("${RED}${i + 1}. $c#$m${if (msg != null) "  —  $msg" else ""}${RESET}")
                    }
                    out.printWriter().use { pw ->
                        pw.println("FAILED TESTS (${failed.size})")
                        failed.forEach { (c, m, msg) ->
                            pw.println("$c#$m${if (msg != null) " — $msg" else ""}")
                        }
                        pw.println()
                        pw.println("Patterns for --tests:")
                        failed.forEach { (c, m, _) -> pw.println("--tests \"$c.$m\"") }
                    }
                    println("📄 Saved failed list -> ${out.absolutePath}")
                } else {
                    out.writeText("No failures 🎉")
                }
            }
        }
    })
}

/** -----------------------------
 *  기본 test 태스크
 *  ----------------------------- */
tasks.named<Test>("test") {
    if (project.findProperty("includeIntegration") == "true") {
        systemProperty("junit.platform.tags.includes", "integration,unit")
    } else {
        systemProperty("junit.platform.tags.excludes", "integration")
    }
    finalizedBy(tasks.jacocoTestReport)
}

/** -----------------------------
 *  JaCoCo 리포트 (fullTest)
 *  ----------------------------- */
tasks.register<JacocoReport>("jacocoFullTestReport") {
    dependsOn(tasks.named("fullTest"))

    executionData(fileTree(layout.buildDirectory.dir("jacoco")) { include("*.exec") })

    reports {
        xml.required.set(true)
        xml.outputLocation.set(layout.buildDirectory.file("reports/jacocoFull/xml/jacocoFullTestReport.xml"))
        html.required.set(true)
        csv.required.set(true)
        html.outputLocation.set(layout.buildDirectory.dir("reports/jacocoFull/html"))
    }

    val main = sourceSets.named("main").get()
    sourceDirectories.setFrom(main.allSource.srcDirs)
    classDirectories.setFrom(
            files(
                    main.output.classesDirs.files.map {
                        fileTree(it) { exclude(coverageExcludes) }
                    }
            )
    )
}

tasks.register<Test>("fullTest") {
    description = "Run unit + integration tests"
    group = "verification"

    val testSourceSet = sourceSets.named("test").get()
    testClassesDirs = testSourceSet.output.classesDirs
    classpath = testSourceSet.runtimeClasspath

    useJUnitPlatform()
    shouldRunAfter(tasks.named("test"))

    extensions.configure(org.gradle.testing.jacoco.plugins.JacocoTaskExtension::class) {
        setDestinationFile(layout.buildDirectory.file("jacoco/fullTest.exec").get().asFile)
    }
    finalizedBy(tasks.named("jacocoFullTestReport"))
}

/** -----------------------------
 *  JaCoCo 리포트 (test)
 *  ----------------------------- */
tasks.jacocoTestReport {
    dependsOn(tasks.named("test"))

    // test.exec 을 명시적으로 사용
    executionData(layout.buildDirectory.file("jacoco/test.exec"))

    reports {
        xml.required.set(true)
        xml.outputLocation.set(layout.buildDirectory.file("reports/jacoco/xml/jacocoTestReport.xml"))
        html.required.set(true)
        csv.required.set(false)
        html.outputLocation.set(layout.buildDirectory.dir("reports/jacoco/html"))
    }
    classDirectories.setFrom(
            files(
                    classDirectories.files.map {
                        fileTree(it) { exclude(coverageExcludes) }
                    }
            )
    )
}

// ============== querydsl ==============
val querydslDir = "src/main/generated"

sourceSets {
    main {
        java {
            srcDirs(querydslDir)
        }
    }
}

tasks.withType<JavaCompile> {
    options.generatedSourceOutputDirectory.set(file(querydslDir))
}

tasks.named("clean") {
    doLast {
        file(querydslDir).deleteRecursively()
    }
}

//checkstyleMain은 compileTestJava 이후에 실행
tasks.named<org.gradle.api.plugins.quality.Checkstyle>("checkstyleMain") {
    dependsOn(tasks.named("compileTestJava"))
    // QueryDSL generated 폴더 제외 (src/main/generated 기준 상대 경로)
    exclude("com/back/**/entity/Q*.java")
}
dependencyManagement {
    imports {
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:$springCloudVersion")
    }
}
