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
| ~~AWS Lightsail~~ | 클라우드 서버 (Seoul) — **사용 중지됨** (2026-09) |
| 홈서버 (예정) | 이사 후 자택에 직접 구축 예정. 자세한 내용은 `구내식당_웹앱_기획설계서.md` 11-2 참고 |
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
- 파일 로그 저장 (Logback, 일자별 로테이션 + 환경별 로그 레벨 분리)

---

## 📝 로그 (Logging)

콘솔뿐 아니라 파일로도 로그가 남도록 `logback-spring.xml`을 구성했어요.

| 파일 | 내용 |
|------|------|
| `logs/meal-management.log` | 전체 로그 |
| `logs/meal-management-error.log` | ERROR 레벨만 모은 로그 (장애 원인 파악용) |

- 날짜가 바뀌거나 10MB를 넘으면 자동으로 파일이 나뉘어요 (`meal-management.2026-09-09.0.log`)
- 최근 30일치만 보관, 전체 용량 1GB 초과 시 오래된 파일부터 자동 삭제
- `logs/` 폴더는 `.gitignore`에 포함되어 있어 git에 올라가지 않아요

### 로그 레벨

기본(운영)은 `INFO`, 로컬 개발 시에는 `local` 프로파일을 켜면 `DEBUG`까지 전부 확인할 수 있어요.

```bash
# 로컬에서 DEBUG 레벨로 실행
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

IntelliJ에서는 실행 설정(Run Configuration)의 `Active profiles`에 `local`을 입력하면 돼요.

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

## ☁️ 인프라 환경 (사용 중지됨)

> ⚠️ 아래는 예전에 쓰던 AWS Lightsail 서버 정보예요. **2026-09부터 사용 중지**했고, 현재 실제로 서비스되고 있지 않아요. 이사 후 자택 홈서버로 직접 배포/운영할 계획이라 여기 적힌 IP·서버 사양은 더 이상 유효하지 않아요.

| 항목 | 내용 |
|------|------|
| Cloud | AWS (Amazon Web Services) |
| Service | Amazon Lightsail |
| Region | Seoul (ap-northeast-2a) |
| OS | Ubuntu 24.04 LTS |
| 서버 사양 | 2GB RAM, 2 vCPUs, 60GB SSD |
| 고정 IP | ~~15.165.199.223~~ (반납됨) |

### 방화벽 설정 (참고용, Lightsail 기준)

| 포트 | 용도 |
|------|------|
| 22 | SSH 접속 |
| 80 | HTTP (Vue.js 프론트엔드) |
| 443 | HTTPS (추후 SSL 적용) |
| 8080 | Spring Boot API |

## 🏠 인프라 이전 계획

이사 예정에 맞춰 홈서버로 배포 환경을 옮길 계획이에요 (2026-09-12 기준 아직 미착수). 전환 시 체크리스트:

- 포트포워딩 최소화 — DB(5432) 등 내부 포트는 외부에 노출하지 않기
- 리버스 프록시 + HTTPS (Let's Encrypt)
- 고정 IP가 아니면 DDNS, 또는 Cloudflare Tunnel / Tailscale Funnel 검토
- 정전·재부팅 후 자동 기동 (`docker-compose`의 `restart: always`는 이미 적용됨)
- DB 백업 (원격지 포함)

홈서버 구축이 완료되면 이 섹션과 위 인프라 환경 정보를 최신 상태로 교체할 예정이에요.

---

## 💻 로컬 개발 환경

| 항목 | 내용 |
|------|------|
| 백엔드 | `http://localhost:8080` |
| 프론트엔드 (Vite) | 기본 `5173`, 이미 사용 중이면 `5174`, `5175` 순으로 자동 변경 |
| CORS 허용 origin | `SecurityConfig.java`에 `5173`~`5175` 등록됨 (다른 포트로 뜨면 403 발생 → 목록에 추가 필요) |
| DB 기본값 | `localhost:5432/meal_management`, `postgres`/`1265` (`application.properties` 기본값, 환경변수로 덮어쓰기 가능) |

> ⚠️ **`.env` 파일은 `docker-compose`에서만 읽어요.** IntelliJ나 `mvnw spring-boot:run`으로 로컬에서 직접 실행할 땐 `.env`가 자동 적용되지 않으므로, `ADMIN_PASSWORD` 등을 바꾸고 싶으면 OS 환경변수나 IntelliJ 실행 설정의 Environment variables에 직접 넣어야 해요.

---

## 🚀 배포 방법

> ⚠️ 아래 절차는 예전 AWS Lightsail 서버 기준이에요. 홈서버 이전 후에는 절차가 달라질 수 있어요 (`🏠 인프라 이전 계획` 참고).

### 서버 초기 세팅

```bash
chmod +x server-setup.sh
./server-setup.sh
```

> `server-setup.sh`는 이 저장소에는 포함되어 있지 않아요 (서버에 별도로 있던 스크립트). 홈서버 세팅 시 필요하면 새로 작성해야 해요.

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
JWT_SECRET=충분히_긴_랜덤_문자열로_직접_생성
EOF

# 배포 실행
docker compose up -d --build
```

> ⚠️ `JWT_SECRET`은 운영 배포 시 반드시 설정해야 해요. 값이 없으면 코드에 있는 로컬 개발용 기본값이 쓰여서 보안상 위험해요. 값을 새로 설정/변경하면 기존에 발급된 모든 JWT 토큰이 무효화(전체 강제 로그아웃)되니 배포 타이밍에 유의하세요.

### 🗄 DB 자동 백업

`docker compose up`을 하면 `backup` 서비스가 같이 떠서 DB를 자동으로 백업해줘요 (2026-09-13 추가, `backup.sh` 참고).

- **주기**: 컨테이너 시작 시 즉시 1회 + 이후 7일(주 1회)마다
- **보관 기간**: 최근 56일(8주)치, 이전 건 자동 삭제
- **저장 위치**: `./backups/meal_management_YYYYMMDD_HHMMSS.sql.gz` — 일부러 Docker 볼륨이 아니라 실제 호스트 폴더에 저장해요 (볼륨끼리 꼬였던 사고와 무관하게 안전하도록). `.gitignore`에 포함되어 있어 git에는 안 올라가요.
- **주기/보관기간 조정**: `.env`에 `BACKUP_RETENTION_DAYS`(일), `BACKUP_INTERVAL_SECONDS`(초) 추가하면 기본값을 덮어쓸 수 있어요.
- **복원 방법**: `gunzip -c backups/파일명.sql.gz | docker exec -i meal-db psql -U postgres -d meal_management`
- ⚠️ 아직 로컬(또는 홈서버) 디스크 안에만 저장돼요 — 홈서버 자체가 고장나면 백업도 같이 사라져요. 홈서버 이전 완료 후 클라우드(구글 드라이브 등) 업로드 기능 추가 예정 (`구내식당_웹앱_기획설계서.md` 11-2 참고).

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

현재 서비스 중지 상태예요 (AWS Lightsail 사용 중지, 홈서버 이전 예정). ~~http://15.165.199.223~~

홈서버 배포 완료 후 새 접속 주소로 갱신할 예정이에요.
