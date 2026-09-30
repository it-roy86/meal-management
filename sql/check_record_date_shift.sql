-- =====================================================================
-- 식사 기록 날짜 밀림 점검 쿼리 (조회 전용 — 데이터를 바꾸지 않음)
-- 기획설계서 17장 "프론트엔드 날짜 기본값 UTC 버그"의 과거 데이터 영향 확인용 (2026-09-30 작성)
--
-- [버그 내용]
--   식사 입력 화면의 날짜 기본값이 toISOString()(UTC) 기준이라,
--   한국 시간 00:00~08:59에 화면을 열면 날짜 기본값이 "어제"로 잡혔음.
--   날짜를 확인하지 않고 저장했다면 record_date = (입력한 날 - 1일)로 저장됐을 수 있음.
--
-- [created_at 시간대 주의]
--   created_at은 서버의 LocalDateTime.now()로 저장됨.
--   - 2026-09-09 낮 이전: Docker(eclipse-temurin alpine, TZ 미설정) 서버 → UTC로 저장됨
--   - 2026-09-09 저녁 이후: 로컬 PC(IntelliJ) 서버 → KST로 저장됨
--   (실제 데이터의 생성 시각 분포로 확인: 9/9 00:11 = UTC(한국 09:11), 9/9 20:45 = KST)
--   그래서 아래 쿼리는 전환 시점(utc_until) 이전 created_at에 +9시간을 해서 한국 시간으로 맞춰 비교함.
--   전환 시점이 다르다고 판단되면 params의 utc_until 값만 바꾸면 됨.
--
-- [실행]
--   psql -h localhost -U postgres -d meal_management -f sql/check_record_date_shift.sql
-- =====================================================================

-- 실수로 데이터를 바꾸지 않도록 이 세션을 읽기 전용으로 설정
SET SESSION CHARACTERISTICS AS TRANSACTION READ ONLY;


-- ---------------------------------------------------------------------
-- 1) 버그 패턴에 해당하는 의심 건
--    조건: 한국 시간 기준 오전 9시 전에 입력 + 기록일이 입력한 날의 "전날"
--    ※ 점심 인원은 점심 이후에야 알 수 있어서, 오전에 입력한 건 원래 "전날" 기록일 가능성이 높음
--      → 여기 나온다고 무조건 잘못된 건 아님. 종이 명단과 날짜를 대조해서 확인할 것.
-- ---------------------------------------------------------------------
WITH params AS (
    SELECT TIMESTAMP '2026-09-09 12:00' AS utc_until
),
kst AS (
    SELECT r.*,
           CASE WHEN r.created_at < p.utc_until
                THEN r.created_at + INTERVAL '9 hours'
                ELSE r.created_at
           END AS created_kst
    FROM meal_record r CROSS JOIN params p
)
SELECT k.id,
       k.record_date                     AS 기록일,
       k.created_kst::timestamp(0)       AS 입력시각_KST,
       c.company_name                    AS 회사,
       t.team_name                       AS 팀,
       k.lunch_count                     AS 중식,
       k.dinner_count                    AS 석식
FROM kst k
JOIN company c      ON c.id = k.company_id
JOIN company_team t ON t.id = k.company_team_id
WHERE k.created_kst::time < TIME '09:00'
  AND k.record_date = k.created_kst::date - 1
ORDER BY k.created_kst, c.company_name, t.team_name;


-- ---------------------------------------------------------------------
-- 2) 같은 날짜 + 같은 팀으로 2건 이상 저장된 경우
--    입력 화면은 항상 새로 저장(POST)해서, 날짜가 밀려 저장되면
--    "이미 기록이 있는 전날"에 한 건이 더 생겨 중복으로 드러날 수 있음.
-- ---------------------------------------------------------------------
SELECT r.record_date                          AS 기록일,
       c.company_name                         AS 회사,
       t.team_name                            AS 팀,
       count(*)                               AS 건수,
       string_agg(r.id::text, ', ' ORDER BY r.id) AS 기록_id목록
FROM meal_record r
JOIN company c      ON c.id = r.company_id
JOIN company_team t ON t.id = r.company_team_id
GROUP BY r.record_date, c.company_name, t.team_name
HAVING count(*) > 1
ORDER BY r.record_date, c.company_name, t.team_name;


-- ---------------------------------------------------------------------
-- 3) 기록이 하나도 없는 평일 (첫 기록일 ~ 마지막 기록일 사이)
--    날짜가 밀려 저장됐다면 "원래 날짜"가 비어 있을 수 있음.
--    공휴일/휴무일도 같이 나오므로 달력과 비교해서 볼 것.
-- ---------------------------------------------------------------------
SELECT d::date                                    AS 기록없는_평일,
       to_char(d, 'Dy')                           AS 요일
FROM generate_series(
         (SELECT min(record_date) FROM meal_record),
         (SELECT max(record_date) FROM meal_record),
         INTERVAL '1 day') AS d
WHERE extract(isodow FROM d) BETWEEN 1 AND 5
  AND NOT EXISTS (SELECT 1 FROM meal_record r WHERE r.record_date = d::date)
ORDER BY d;
