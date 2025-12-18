# 1단계: Gradle로 Spring Boot JAR 빌드
FROM gradle:8.10.0-jdk21 AS builder
WORKDIR /app

# Gradle 설정 파일 먼저 복사 (캐시 최적화)
COPY build.gradle.kts settings.gradle.kts ./
COPY gradle gradle
COPY gradlew .
RUN chmod +x gradlew

# 의존성 캐시
RUN ./gradlew dependencies --no-daemon

# 소스 코드 복사
COPY src src

# Spring Boot 실행용 JAR 생성
RUN ./gradlew bootJar --no-daemon -x test

# 두 번째 스테이지: 실행만 하는 가벼운 JRE 이미지
FROM eclipse-temurin:21-jre
WORKDIR /app

# Doppler CLI 설치
RUN apt-get update && apt-get install -y apt-transport-https ca-certificates curl gnupg && \
    curl -sLf --retry 3 --tlsv1.2 --proto "=https" 'https://packages.doppler.com/public/cli/gpg.DE2A7741A397C129.key' | gpg --dearmor -o /usr/share/keyrings/doppler-archive-keyring.gpg && \
    echo "deb [signed-by=/usr/share/keyrings/doppler-archive-keyring.gpg] https://packages.doppler.com/public/cli/deb/debian any-version main" | tee /etc/apt/sources.list.d/doppler-cli.list && \
    apt-get update && \
    apt-get -y install doppler

# Build argument로 Doppler 토큰 받기
ARG DOPPLER_TOKEN
ENV DOPPLER_TOKEN=$DOPPLER_TOKEN
ENV DOPPLER_PROJECT=waitfair
ENV DOPPLER_CONFIG=prd

# 첫 번째 스테이지에서 빌드된 JAR 파일 복사
COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["/bin/sh", "-c", "doppler secrets download --project waitfair --config prd --format env --no-file > /tmp/.env && set -a && . /tmp/.env && exec java -jar -Dspring.profiles.active=prod app.jar"]