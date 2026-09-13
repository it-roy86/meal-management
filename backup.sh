#!/bin/sh
# DB 자동 백업 스크립트
#
# postgres:16-alpine 이미지 안에서 실행돼요 (pg_dump가 이미 들어있음).
# docker-compose.yml의 backup 서비스가 이 스크립트를 계속 돌려요.
#
# 동작 방식:
#   1. 컨테이너가 뜨자마자 즉시 한 번 백업 (배포 직후에도 최신 백업이 있도록)
#   2. BACKUP_INTERVAL_SECONDS 만큼 잠들었다가 다시 백업 (기본 7일 = 주 1회)
#   3. 오래된 백업 파일은 RETENTION_DAYS 지나면 자동 삭제
#
# 왜 볼륨이 아니라 폴더(bind mount)에 저장하냐면:
#   Docker 볼륨끼리 꼬여서 데이터가 분리됐던 사고(기획설계서 13장)가 있었어서,
#   백업만큼은 그 문제와 완전히 무관하게 눈에 보이는 실제 폴더에 남겨두는 거예요.

set -eu

BACKUP_DIR="/backups"
RETENTION_DAYS="${RETENTION_DAYS:-56}"              # 기본 8주(56일)치 보관
BACKUP_INTERVAL_SECONDS="${BACKUP_INTERVAL_SECONDS:-604800}"  # 기본 7일(주 1회)

echo "[backup] 시작 — DB_HOST=$DB_HOST DB_NAME=$DB_NAME, 보관기간=${RETENTION_DAYS}일, 주기=${BACKUP_INTERVAL_SECONDS}초"

while true; do
    timestamp=$(date +%Y%m%d_%H%M%S)
    filename="$BACKUP_DIR/meal_management_${timestamp}.sql.gz"

    echo "[backup] $timestamp 백업 시작..."
    if pg_dump -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" "$DB_NAME" | gzip > "$filename"; then
        size=$(du -h "$filename" | cut -f1)
        echo "[backup] 완료: $filename ($size)"
    else
        echo "[backup] 실패! (다음 주기에 재시도)" >&2
        rm -f "$filename"
    fi

    # 보관기간이 지난 오래된 백업 삭제
    find "$BACKUP_DIR" -name "meal_management_*.sql.gz" -mtime "+$RETENTION_DAYS" -delete

    echo "[backup] 다음 백업까지 대기 (${BACKUP_INTERVAL_SECONDS}초)"
    sleep "$BACKUP_INTERVAL_SECONDS"
done
