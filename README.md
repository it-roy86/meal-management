# 🍱 구내식당 식수 관리 웹앱

> 어머니가 운영하시는 구내식당의 종이 명단을 디지털로 자동화한 웹 애플리케이션이에요.

---

## 📌 프로젝트 개요

| 항목 | 내용 |
|------|------|
| 프로젝트명 | 구내식당 식수 관리 웹앱 |
| 목적 | 종이 명단 → 디지털 자동화 (매일 종이에 기록 → 엑셀 정리 작업 제거) |
| 대상 회사 | 대박유통, (주)복수 |
| 개발 기간 | 2026.04 ~ 2026.05 |

---

## 🔗 저장소

| 저장소 | 링크 | 설명 |
|--------|------|------|
| 백엔드 | [meal-management](https://github.com/it-roy86/meal-management) | Spring Boot API 서버 |
| 프론트엔드 | [meal-management-front](https://github.com/it-roy86/meal-management-front) | Vue.js 클라이언트 |
| 개발노트 | [dev-notes](https://github.com/it-roy86/dev-notes) | 개발 과정 기록 |

---

## 🛠 기술 스택

### 백엔드
| 기술 | 버전 |
|------|------|
| Java | 17 |
| Spring Boot | 3.5.13 |
| Spring Security | JWT 인증 |
| PostgreSQL | 16 |
| JPA / Hibernate | 6.x |

### 프론트엔드
| 기술 | 버전 |
|------|------|
| Vue.js | 3.x |
| Vite | 5.x |
| Axios | - |
| Vue Router | 4.x |

### 인프라
| 기술 | 설명 |
|------|------|
| AWS Lightsail | 클라우드 서버 (Seoul) |
| Docker | 컨테이너 배포 |
| Docker Compose | 멀티 컨테이너 관리 |
| Nginx | 웹 서버 / API 프록시 |

---

## 👥 사용자 권한

| 역할 | 대상 | 권한 |
|------|------|------|
| ADMIN | 개발자(아드님) | 전체 관리 (설정/입력/조회/정산) |
| OPERATOR | 어머니(식당 운영자) | 일일 식사 인원 입력 |
| VIEWER | 경리담당자 | 자기 회사 데이터만 읽기 전용 조회 |

---

## ✅ 개발 완료 기능

### 인증
- JWT 기반 로그인 (ADMIN/OPERATOR)
- 사업자번호 뒤 4자리로 VIEWER 로그인 (별도 계정 불필요)
- 역할별 화면 분기

### 설정 관리 (ADMIN)
- 회사 등록/수정 (사업자번호, 담당자 이메일 포함)
- 팀 등록/수정 (중식/석식 단가 설정)

### 식사 입력 (OPERATOR)
- 날짜/회사/팀 선택
- 중식/석식 인원 입력
- 금액 자동 계산 (단가 × 인원)

### 식사 현황 조회 (ADMIN/VIEWER)
- 날짜 범위 조회
- 회사별 필터링
- 합계 자동 계산

### 월별 정산 (ADMIN/VIEWER)
- 년월 선택으로 월간 집계
- 회사/팀별 정산 데이터
- 중식/석식 금액 분리 표시

### 기타
- 모바일 반응형 (768px 기준, 테이블 → 카드 전환)
- VIEWER 데이터 제한 (JWT의 companyId로 자기 회사만 조회)
- 소프트 딜리트 (is_active로 데이터 보존)

---

## 🏗 아키텍처
인터넷
↓
Nginx (80포트) → Vue.js 정적 파일 서빙
↓ /api/* 요청
Spring Boot (8080포트)
↓
PostgreSQL (5432포트)

### Docker 컨테이너 구성
meal-frontend   Nginx + Vue.js  80포트
meal-backend    Spring Boot     8080포트
meal-db         PostgreSQL      5432포트

---

## ☁️ 인프라 환경

| 항목 | 내용 |
|------|------|
| Cloud | AWS (Amazon Web Services) |
| Service | Amazon Lightsail |
| Region | Seoul (ap-northeast-2a) |
| OS | Ubuntu 24.04 LTS |
| 서버 사양 | 2GB RAM, 2 vCPUs, 60GB SSD |
| 고정 IP | 15.165.199.223 |

### 방화벽 설정

| 포트 | 용도 |
|------|------|
| 22 | SSH 접속 |
| 80 | HTTP (Vue.js 프론트엔드) |
| 443 | HTTPS (추후 SSL 적용) |
| 8080 | Spring Boot API |

---

## 🚀 배포 방법

### 서버 초기 세팅

```bash
chmod +x server-setup.sh
./server-setup.sh
```

### Docker 배포

```bash
git clone https://github.com/it-roy86/meal-management.git
git clone https://github.com/it-roy86/meal-management-front.git

cd meal-management

# .env 파일 생성 (직접 작성)
cat > .env << 'EOF'
DB_NAME=meal_management
DB_USER=postgres
DB_PASSWORD=비밀번호
ADMIN_PASSWORD=관리자비밀번호
OPERATOR_PASSWORD=운영자비밀번호
EOF

# 배포 실행
docker compose up -d --build
```

### 유용한 Docker 명령어

```bash
# 컨테이너 상태 확인
docker compose ps

# 로그 확인
docker compose logs backend
docker compose logs frontend

# 재시작
docker compose restart

# 중지
docker compose down

# 중지 + 데이터 삭제
docker compose down -v
```

---

## 📁 프로젝트 구조

### 백엔드 (Spring Boot)
src/main/java/meal_management/
├── config/          SecurityConfig.java
├── controller/      Auth, Company, CompanyTeam, MealRecord, Settlement
├── dto/             LoginRequestDto, LoginResponseDto
├── entity/          Company, CompanyTeam, User, MealRecord
├── repository/      4개
├── service/         Auth, Company, CompanyTeam, MealRecord
├── util/            JwtUtil, JwtAuthenticationFilter
└── DataInitializer.java

### 프론트엔드 (Vue.js)
src/
├── api/             axios.js (JWT 인터셉터)
├── views/
│   ├── auth/        LoginView.vue
│   ├── dashboard/   DashboardView.vue
│   ├── setting/     SettingView.vue
│   ├── meal/        MealInputView.vue, MealView.vue
│   └── settlement/  SettlementView.vue
└── router/          index.js
---

## 🌐 접속 주소
http://15.165.199.223
