-- =====================================================================================
-- KAN-109  야간 이벤트 공지 푸시 보류 → 08:00 자동 발송 (운영 MySQL 수동 실행)
--   브랜치: feat/KAN-109-deferred-event-push (addiction1215/quitmate-admin-server)
--   순서: 0) 구조 확인 → 1) 칸 추가 → 2) 인덱스(넣지 않음) → 3) 확인 → [관리자 서버 dev·운영 배포]
--   배포 순서
--   - 1)을 관리자 서버 dev·운영 배포보다 먼저 합니다. dev와 운영은 DB가 하나라 한 번만 하면 됩니다.
--     · dev 관리자 서버는 ddl-auto: none 이라, 1) 전에 dev를 배포하면 공지 목록·등록·수정·삭제가
--       Unknown column 'push_status' 로 실패하고, 1분마다 도는 스케줄러도 같은 오류를 로그에 남깁니다.
--     · 운영은 ddl-auto: update 라 1)보다 먼저 나가도 Hibernate가 같은 모양(varchar(20)·datetime(6), NULL 허용)으로
--       칸을 만들어 깨지지는 않습니다. 칸 정의를 이 파일로 맞추려면 1)을 먼저 해 주세요.
--   - 앱 서버(addiction)는 notice 를 읽기만 하고 새 칸을 모릅니다. 1)도 배포도 앱 서버에는 영향이 없습니다.
--   - 1) 뒤에는 이전 jar로 롤백해도 동작합니다(새 칸은 모두 NULL 허용, 옛 코드는 새 칸을 읽지도 쓰지도 않음).
--     롤백한 동안에는 08:00 발송을 하는 서버가 없습니다. 보류(DEFERRED)한 푸시는 새 jar를 다시 배포한 뒤
--     보낼 수 있는 시간(08:00~20:00)의 첫 실행에 나갑니다.
-- =====================================================================================


-- -------------------------------------------------------------------------------------
-- 0) 지금 구조 확인
--    기대: push_status·push_scheduled_at·push_sent_at 칸이 아직 없음.
--    created_date 가 datetime(6) 이면 그대로 진행합니다(Hibernate가 만든 테이블이면 datetime(6)).
--    datetime(소수 초 없음)이면 1)의 datetime(6)을 datetime 으로 바꿔도 됩니다. 코드는 둘 다 동작합니다.
-- -------------------------------------------------------------------------------------
SHOW CREATE TABLE notice;


-- -------------------------------------------------------------------------------------
-- 1) 칸 추가 (관리자 서버 dev·운영 배포 전)
--    모두 NULL 허용·기본값 없음 → 기존 행은 모두 NULL(= 이 기능 전에 등록한 공지)로 읽히고 바뀌지 않습니다.
--    MySQL 8.0.12+ 는 NULL 허용 칸 추가를 INSTANT 로 처리해 테이블을 다시 쓰지 않습니다.
--    잠금을 5초 안에 못 잡으면 이 ALTER만 실패합니다. 잠시 뒤 다시 실행하면 됩니다.
--    push_status 는 ENUM 이 아니라 VARCHAR 입니다(값을 더할 때 운영에서만 저장이 실패하는 일을 막으려고).
--      DEFERRED 발송 예정 / SENDING 발송 중 / SENT 발송함 / FAILED 발송 실패(다시 보내지 않음) / CANCELED 보내기 전 삭제
--    시각 두 칸은 한국 시각(서버 -Duser.timezone=Asia/Seoul, created_date 와 같은 기준)입니다.
-- -------------------------------------------------------------------------------------
SET SESSION lock_wait_timeout = 5;
ALTER TABLE notice
  ADD COLUMN push_status       varchar(20) DEFAULT NULL COMMENT '푸시 발송 상태(KAN-109) DEFERRED/SENDING/SENT/FAILED/CANCELED. 이 기능 전 공지는 NULL',
  ADD COLUMN push_scheduled_at datetime(6) DEFAULT NULL COMMENT '보류한 푸시를 보낼 시각(한국 시각). 야간 이벤트 공지만',
  ADD COLUMN push_sent_at      datetime(6) DEFAULT NULL COMMENT '푸시를 보내기 시작한 시각(한국 시각)';


-- -------------------------------------------------------------------------------------
-- 2) 인덱스: 넣지 않습니다.
--    스케줄러가 1분마다(dev·운영 각각) push_status = 'DEFERRED' AND push_scheduled_at <= ? AND use_yn = 'Y' 로 읽지만,
--    notice 는 행이 많지 않아 전체를 훑어도 부담이 없습니다. 행이 수만 개가 되면 아래를 추가하세요.
--      CREATE INDEX idx_notice_push_status_scheduled ON notice (push_status, push_scheduled_at);
-- -------------------------------------------------------------------------------------


-- -------------------------------------------------------------------------------------
-- 3) 확인
-- -------------------------------------------------------------------------------------
-- 3-1) 칸 정의. 기대: 3줄
--      push_status varchar(20) / push_scheduled_at datetime(6) / push_sent_at datetime(6), 모두 IS_NULLABLE = YES, COLUMN_DEFAULT = NULL
SELECT COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE, COLUMN_DEFAULT
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'notice'
  AND COLUMN_NAME IN ('push_status', 'push_scheduled_at', 'push_sent_at')
ORDER BY ORDINAL_POSITION;

-- 3-2) 기존 행에 영향 없음. 기대(배포 전): 0
SELECT COUNT(*) AS rows_with_push_columns
FROM notice
WHERE push_status IS NOT NULL OR push_scheduled_at IS NOT NULL OR push_sent_at IS NOT NULL;


-- =========================== [ 관리자 서버 dev·운영 배포 ] ===================================


-- (참고) 배포 뒤 상태 보기. 시각은 모두 한국 시각입니다. DB의 NOW() 와 비교하지 마세요(DB 시간대가 다를 수 있음).
--   SELECT id, type, title, use_yn, push_status, push_scheduled_at, push_sent_at
--   FROM notice
--   WHERE push_status IN ('DEFERRED', 'SENDING', 'FAILED')
--   ORDER BY id DESC;
--   - DEFERRED: push_scheduled_at(08:00)이 지나고 1~2분 안에 SENT 로 바뀌면 정상입니다.
--   - SENDING 이 30분 넘게 남으면 서버가 FAILED 로 바꾸고 다시 보내지 않습니다.
--   - FAILED 는 자동으로 다시 보내지 않습니다(일부에게 이미 갔을 수 있음). 다시 보내려면 공지를 새로 등록해 주세요.

-- (되돌리기, 필요할 때만) 이전 jar 는 새 칸을 쓰지 않으므로 칸을 지우지 않아도 됩니다. 지우려면 이전 jar 로 되돌린 뒤:
--   SET SESSION lock_wait_timeout = 5;
--   ALTER TABLE notice DROP COLUMN push_status, DROP COLUMN push_scheduled_at, DROP COLUMN push_sent_at;
