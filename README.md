# WaitFair

> 사전 등록과 랜덤 대기열 시스템으로 모든 사용자에게 공평한 티켓 구매 기회를 제공하는 예매 플랫폼


## 💡서비스 소개

**WaitFair**는 기존 선착순 티켓팅 구조에서 발생하는 **불공정성, 서버 트래픽 폭주, 암표 거래 문제**를 해결하기 위해 설계된 **사전 등록 + 랜덤 대기열 기반의 공정한 예매 플랫폼**입니다.

기존 티켓팅은 접속 속도, 네트워크 상태, 디바이스 성능, 자동화 도구 여부 등 환경적 요소에 의해 예매 성공 여부가 결정되었습니다. 

이러한 요소들은 사용자가 통제할 수 없으며, 진심으로 예매를 원하는 사용자에게 공평한 기회를 제공하지 못했습니다.

WaitFair는 이러한 환경적 편차를 제거하고, **누구나 동일한 조건에서 참여할 수 있는 예매 경험**을 제공하는 것을 목표로 합니다.

---

## 👨‍💻 개발 기간 & 팀원 소개

### **개발 기간**

> 2025.12.03 ~ 2026.01.07
> 

### **팀원**

| <a href="https://github.com/LeeMinwoo115"><img src="https://github.com/LeeMinwoo115.png" width="100"/></a> | <a href="https://github.com/gksdud1109"><img src="https://github.com/gksdud1109.png" width="100"/></a> | <a href="https://github.com/kimeunkyoungg"><img src="https://github.com/kimeunkyoungg.png" width="100"/></a> | <a href="https://github.com/No-366"><img src="https://github.com/No-366.png" width="100"/></a> | <a href="https://github.com/thatgirls00"><img src="https://github.com/thatgirls00.png" width="100"/></a> |
| :---: | :---: | :---: | :---: | :---: |
| **이민우** | **정한영** | **김은경** | **나웅철** | **전현수** |
| PO | BE 팀장 | 팀원 | 팀원 | 팀원 |

---

## ✨ WaitFair의 차별점

### BEFORE (기존 선착순 방식의 문제점)

- 네트워크 속도, 디바이스 성능 등 사용자가 통제할 수 없는 요인이 예매 성공 여부를 결정
- 클릭 전쟁으로 인한 서버 과부하 : 오픈 시간 트래픽 폭주로 서버 다운 및 지연 빈번
- 봇/매크로의 남용 : 자동화 도구가 상위 순번을 독점하며 일반 사용자 피해 증가
- 암표(리셀) 문제

### AFTER (WaitFair의 해결책)

- 사전 등록 + 대기열 랜덤 셔플로 공정한 티켓팅 환경 제공
- 배치 단위 입장으로 트래픽 분산
- reCAPTCHA & ****IP Rate Limiting & IDC IP 차단 & Device Fingerprinting 기반 봇/매크로 차단
- Dynamic QR, 디바이스 중복 차단, Merkle Tree 기반 양도 시스템으로 암표 문제 해결

---

## ✅ 주요 기능

1. JWT 기반 인증 / 인가 및 소셜 로그인
2. CoolSMS를 비롯한 봇/매크로 차단 사전 등록
3. Fisher-Yates Shuffle 기반 공정한 대기열 배정 & WebSocket 기반 실시간 순서 확인
4. WebSocket 기반 실시간 좌석 선택
5. Toss Payments 기반 결제
6. Dynamic QR 티켓
7. WebSocket 기반 실시간 알림
8. Merkle Tree 기반 양도 시스템
9. 관리자 대시보드
10. Grafana Alloy 기반 서버 관측 파이프라인

---

## ⚙️ 환경 변수 설정

**로컬 환경 .env.properties**

```java
# PostgreSQL
DB_USER=your_db_user
DB_PASSWORD=your_db_password

# JWT 
JWT_SECRET=your_secret_pattern
JWT_QR_SECRET=your_qr_secret_pattern
JWT_ACCESS_TOKEN_DURATION=3600
JWT_REFRESH_TOKEN_DURATION=1209600

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=your_redis_password

# URLs
API_BASE_URL=waitfair.shop
FRONTEND_URL=https://www.waitfair.shop

# CoolSMS
SMS_API_KEY=your_sms_api_key
SMS_SECRET_KEY=your_sms_secret_key
SMS_SENDER_NUMBER=your_sms_sender_number

# S3
S3_BUCKET_NAME=your_s3_bucket_name

# reCAPTCHA
RECAPTCHA_SITE_KEY=your_recaptcha_site_key
RECAPTCHA_SECRET_KEY=your_recaptcha_secret_key

# TossPayments
TOSS_SECRET_KEY=your_payment_secret_key

# KAKAO
KAKAO_CLIENT_ID=your_kakao_client_id
KAKAO_CLIENT_SECRET=your_kakao_client_secret

```

**배포 환경 Doppler**

```java
# PostgreSQL
DB_USER=your_db_user
DB_PASSWORD=your_db_password

# Supabase Database (prod)
SUPABASE_URL=your_supabase_url
SUPABASE_USERNAME=your_supabase_username
SUPABASE_PASSWORD=your_supabase_password

# JWT 
JWT_SECRET=your_secret_pattern
JWT_QR_SECRET=your_qr_secret_pattern
JWT_ACCESS_TOKEN_DURATION=3600
JWT_REFRESH_TOKEN_DURATION=1209600

# Redis
REDIS_HOST=your_docker_host
REDIS_PORT=6379
REDIS_PASSWORD=your_redis_password

# URLs
API_BASE_URL=waitfair.shop
FRONTEND_URL=https://www.waitfair.shop
BACKEND_URL=https://api.waitfair.shop

# CoolSMS
SMS_API_KEY=your_sms_api_key
SMS_SECRET_KEY=your_sms_secret_key
SMS_SENDER_NUMBER=your_sms_sender_number

# S3
S3_BUCKET_NAME=your_s3_bucket_name

# reCAPTCHA
RECAPTCHA_SITE_KEY=your_recaptcha_site_key
RECAPTCHA_SECRET_KEY=your_recaptcha_secret_key

# TossPayments
TOSS_SECRET_KEY=your_payment_secret_key

# Grafana
GRAFANA_API_TOKEN=your_grafana_token
GRAFANA_USER_ID=your_grafana_name
LOKI_PUSH_URL=your_loki_url

# KAKAO
KAKAO_CLIENT_ID=your_kakao_client_id
KAKAO_CLIENT_SECRET=your_kakao_client_secret
```

---

## 🌐 배포 URL

**[BE]** https://api.waitfair.shop/

**[FE]** https://www.waitfair.shop/

---

## 🔗 ERD

**추가 예정**

---

## 🖼️ 시스템 아키텍처

<img width="3208" height="2301" alt="Image" src="https://github.com/user-attachments/assets/75630aff-5a0f-4f22-8a81-66c8ac3b77f2" />

---

## 🛠️ 기술 스택

### Backend

![Java](https://img.shields.io/badge/Java-007396?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)
![Spring Security](https://img.shields.io/badge/Spring%20Security-6DB33F?style=for-the-badge&logo=springsecurity&logoColor=white)
![Spring Data JPA](https://img.shields.io/badge/Spring%20Data%20JPA-007396?style=for-the-badge&logo=hibernate&logoColor=white)
![QueryDSL](https://img.shields.io/badge/QueryDSL-4479A1?style=for-the-badge&logo=databricks&logoColor=white)
![Gradle](https://img.shields.io/badge/Gradle-02303A?style=for-the-badge&logo=gradle&logoColor=white)
![Swagger](https://img.shields.io/badge/Swagger-85EA2D?style=for-the-badge&logo=swagger&logoColor=black)
![WebSocket](https://img.shields.io/badge/WebSocket%20(STOMP)-010101?style=for-the-badge&logo=socket.io&logoColor=white)
![reCAPTCHA](https://img.shields.io/badge/reCAPTCHA-4285F4?style=for-the-badge&logo=google&logoColor=white)

### Database

![PostgreSQL](https://img.shields.io/badge/PostgreSQL-4169E1?style=for-the-badge&logo=postgresql&logoColor=white)
![H2 Database](https://img.shields.io/badge/H2%20Database-003B57?style=for-the-badge&logo=h2&logoColor=white)

### Infra

![Docker](https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=docker&logoColor=white)
![AWS](https://img.shields.io/badge/AWS-232F3E?style=for-the-badge&logo=amazonaws&logoColor=white)
![EC2](https://img.shields.io/badge/Amazon%20EC2-FF9900?style=for-the-badge&logo=amazonec2&logoColor=white)
![S3](https://img.shields.io/badge/Amazon%20S3-569A31?style=for-the-badge&logo=amazons3&logoColor=white)
![Supabase](https://img.shields.io/badge/Supabase-3FCF8E?style=for-the-badge&logo=supabase&logoColor=white)

### Observability & Performance

![Grafana](https://img.shields.io/badge/Grafana-F46800?style=for-the-badge&logo=grafana&logoColor=white)
![Prometheus](https://img.shields.io/badge/Prometheus-E6522C?style=for-the-badge&logo=prometheus&logoColor=white)
![k6](https://img.shields.io/badge/k6-7D64FF?style=for-the-badge&logo=k6&logoColor=white)
![Doppler](https://img.shields.io/badge/Doppler-000000?style=for-the-badge&logo=doppler&logoColor=white)

### External Services

![CoolSMS](https://img.shields.io/badge/CoolSMS-00C300?style=for-the-badge&logo=messagebird&logoColor=white)
![Toss Payments](https://img.shields.io/badge/Toss%20Payments-0064FF?style=for-the-badge&logo=toss&logoColor=white)

---

## 💬 개발 컨벤션

### 1️⃣ GitHub Flow

- **main**
    - 실제 서비스에 배포되는 안정화 브랜치
    - 직접 커밋 금지 (feature, fix, refactor 브랜치를 통해 반영)
    - 브랜치 보호 규칙 적용 : PR을 통해 최소 1명의 리뷰 승인 후 머지 가능
- **feature/ & fix/ & refactor/**
    - 개별 기능 개발, 버그 수정, 코드 리팩토링용 브랜치
    - 이슈 단위로 생성하여 작업
    - 작업 완료 후 PR을 통해 main에 머지

---

### **2️⃣ 작업 순서**

1. **이슈 생성** → 작업 단위 정의
2. **브랜치 생성** → main 브랜치에서 이슈별 작업 브랜치 생성
3. **Commit & Push**
4. **PR 생성 & 코드 리뷰** → 최소 1명 승인 필요
5. **Merge & 브랜치 정리**
    - 리뷰 완료 후 main 브랜치로 Merge
    - Merge 후 이슈별 작업 브랜치 삭제

---

### 3️⃣ 네이밍 & 작성 규칙

1. **이슈**
    - 제목 규칙 : `[타입] 작업내용`
    - 예시 : `[feat] 로그인 기능 추가`
    - 본문은 템플릿에 맞춰서 작성
2. **PR**
    - 제목 규칙 : `[타입] 작업내용`
    - 예시 : `[feat] 로그인 기능 추가`
    - 본문은 템플릿에 맞춰서 작성 + close #이슈넘버
3. **브랜치**
    - 생성 기준 : `main` 브랜치에서 생성
    - 명명 규칙 : `타입/작업 설명`
    - 예시: `feat/login`
4. **Commit Message 규칙**
    
    
    | 타입 | 의미 |
    | --- | --- |
    | **feat** | 새로운 기능 추가 |
    | **fix** | 버그 수정 |
    | **docs** | 문서 수정 (README, 주석 등) |
    | **style** | 코드 스타일 변경 (포맷팅, 세미콜론 등. 기능 변화 없음) |
    | **refactor** | 코드 리팩토링 (동작 변화 없음) |
    | **test** | 테스트 코드 추가/수정 |
    | **chore** | 빌드, 패키지 매니저, 설정 파일 등 유지보수 작업(환경 설정) |
    | **remove** | 파일, 폴더 삭제 |
    | **rename** | 파일, 폴더명 수정 |
    - `타입 : 작업내용`
    - 예시: `feat : 로그인 기능 추가`

---

### 4️⃣ 코드 컨벤션

**네이버 Java 스타일 가이드 기반**
- 기본 원칙
    - 가독성 최우선
    - 특별한 이유가 없는 경우 IntelliJ IDEA 자동 서식 준수
