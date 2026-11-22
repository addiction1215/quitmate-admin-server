-- =====================================================================================
-- KAN-208  관리자 위치정보 열람 이력 표 만들기 (운영 MySQL 수동 실행)
--   근거: 법무 검토 KAN-136. 관리자가 회원의 위치정보를 볼 때 누가·언제·어느 회원의·무엇을 봤는지 남깁니다.
--         이 표에는 주소·좌표 값을 남기지 않습니다(칸 자체가 없음).
--   브랜치: feat/KAN-208-location-access-log (addiction1215/quitmate-admin-server)
--   순서: 0) 구조 확인 → 1) 표 만들기 → 2) 확인 → [관리자 서버 운영 배포] → 3) 조회 예시(필요할 때)
--   배포 순서
--   - 1)을 관리자 서버 운영 배포보다 먼저 합니다. PR은 1) 적용 뒤에 머지합니다(지금은 dev 서버·dev DB 없이 운영 DB 하나라 한 번만 하면 됩니다).
--     · 운영은 ddl-auto: update 라 1)보다 먼저 나가면 Hibernate가 표를 만듭니다(COMMENT 없음, 표 옵션은 DB 기본값).
--       칸 정의·인덱스 이름은 엔티티와 같지만, COMMENT·문자셋을 이 파일로 맞추려면 1)을 먼저 해 주세요.
--     · 표가 없거나 저장이 실패해도 미션 이력 상세 응답은 그대로 200으로 나가고(저장 실패는 응답을 막지 않음), 로그에
--       "위치정보 열람 이력 저장 실패" ERROR 한 줄이 남습니다. 그동안의 열람은 이력이 없습니다.
--   - 새 표만 만듭니다. 기존 표는 바꾸지 않습니다. 1) 뒤에 이전 jar로 롤백해도 동작합니다(옛 코드는 이 표를 모름).
--   - 앱 서버(addiction)는 이 표를 모릅니다.
--   칸 이름은 엔티티 AdminLocationAccessLog 의 @Column 이름입니다. AdminLocationAccessLogDdlTest 가 Hibernate(MySQL 방언)가
--   만들 DDL과 아래 칸·타입·NULL 여부·인덱스 이름이 같은지 확인합니다.
--     adminUserId → admin_user_id, targetUserId → target_user_id, resourceType → resource_type,
--     resourceId → resource_id, api → api, accessedAt → accessed_at(한국 시각)
--   보관: 기획 결정으로 6개월. 지난 행은 관리자 서버가 매일 04:40(KST)에 accessed_at 기준으로 자동 삭제합니다(표 구조 변경 없음).
--   created_date·updated_date 는 두지 않습니다(고치지 않는 표, accessed_at 이 곧 만든 시각).
--   회원 id 두 칸에는 외래 키를 걸지 않습니다. 회원 행이 지워져도 열람 근거는 남아야 하고, 이 표 때문에 회원 삭제가 막히면 안 됩니다.
--   resource_type 은 MySQL ENUM 이 아니라 VARCHAR 입니다(종류를 더해도 DDL 불필요).
-- =====================================================================================


-- -------------------------------------------------------------------------------------
-- 0) 지금 구조 확인
-- -------------------------------------------------------------------------------------

-- 0-1) 표가 아직 없는지 봅니다(빈 결과여야 함).
--      이미 있으면(운영 ddl-auto가 먼저 만듦) 1)을 건너뛰고 2)에서 칸·인덱스만 비교합니다.
SHOW TABLES LIKE 'admin_location_access_log';

-- 0-2) 다른 표의 표 옵션을 봅니다. 1)의 DEFAULT CHARSET·COLLATE 를 이것과 같게 맞춰 주세요
--      (1)은 utf8mb4 / utf8mb4_unicode_ci 로 적어 둠. 앱 서버 KAN-177 적용 때 운영 DB가 utf8mb4_unicode_ci 였음).
--      시각 칸은 datetime(6)이어야 합니다(Hibernate가 LocalDateTime 을 MySQL에서 만드는 타입).
SHOW CREATE TABLE user_pass;


-- -------------------------------------------------------------------------------------
-- 1) 표 만들기 (관리자 서버 운영 배포 전)
--    외래 키가 없어 다른 표를 잠그지 않습니다. 메타데이터 잠금을 5초 안에 못 잡으면 실패하니 다시 실행하면 됩니다.
--    인덱스
--      idx_admin_location_access_log_target_accessed (target_user_id, accessed_at): 회원 한 명의 위치를 누가 언제 봤는지
--      idx_admin_location_access_log_accessed (accessed_at): 기간별 전체 열람, 보관 기간이 지난 행 지우기
-- -------------------------------------------------------------------------------------
SET SESSION lock_wait_timeout = 5;
CREATE TABLE admin_location_access_log (
  id             bigint       NOT NULL AUTO_INCREMENT,
  admin_user_id  bigint       NOT NULL COMMENT '열람한 관리자 users.id',
  target_user_id bigint       NOT NULL COMMENT '위치정보 주인 회원 users.id',
  resource_type  varchar(40)  NOT NULL COMMENT '본 위치정보 종류. MISSION_HISTORY_ADDRESS=미션 위치 인증 좌표(mission_history_address)',
  resource_id    bigint       NOT NULL COMMENT 'resource_type 의 행 id. MISSION_HISTORY_ADDRESS 면 mission_history.id',
  api            varchar(100) NOT NULL COMMENT '메서드 + 경로 패턴. 예: GET /api/v1/admin/mission-history/{id}',
  accessed_at    datetime(6)  NOT NULL COMMENT '열람 시각(한국 시각)',
  PRIMARY KEY (id),
  KEY idx_admin_location_access_log_target_accessed (target_user_id, accessed_at),
  KEY idx_admin_location_access_log_accessed (accessed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='관리자 위치정보 열람 이력(KAN-208). 주소·좌표 값은 남기지 않음. 고치지 않고 쌓기만 함';


-- -------------------------------------------------------------------------------------
-- 2) 확인
-- -------------------------------------------------------------------------------------

-- 2-1) 표 정의. 기대: 위 칸 7개·인덱스 2개 그대로, 외래 키 없음.
SHOW CREATE TABLE admin_location_access_log;

-- 2-2) 칸 정의. 기대: 7줄, 모두 IS_NULLABLE = NO
--      id bigint / admin_user_id bigint / target_user_id bigint / resource_type varchar(40) /
--      resource_id bigint / api varchar(100) / accessed_at datetime(6)
SELECT COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE, COLUMN_DEFAULT
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'admin_location_access_log'
ORDER BY ORDINAL_POSITION;

-- 2-3) 인덱스. 기대: PRIMARY(id), idx_admin_location_access_log_target_accessed(target_user_id, accessed_at),
--      idx_admin_location_access_log_accessed(accessed_at)
SELECT INDEX_NAME, SEQ_IN_INDEX, COLUMN_NAME
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'admin_location_access_log'
ORDER BY INDEX_NAME, SEQ_IN_INDEX;

-- 2-4) 행. 기대(배포 전): 0
SELECT COUNT(*) AS rows_before_deploy FROM admin_location_access_log;


-- ============================== [ 관리자 서버 운영 배포 ] =====================================
-- 배포 뒤 이 표에 행이 쌓이는 때
--   · 관리자가 미션 이력 상세(GET /api/v1/admin/mission-history/{id})를 열었고, 응답에 좌표(locationData.addresses)가
--     하나라도 담겼을 때 한 줄. LOCATION 이 아닌 유형, 좌표가 비어 있는 LOCATION, 없는 이력(400), 관리자가 아닌 계정(403)은 남기지 않습니다.
--   · 미션 이력 목록에는 좌표가 없어 남기지 않습니다.
-- 저장이 실패하면 응답은 그대로 나가고 관리자 서버 로그에 아래 ERROR 한 줄이 남습니다(좌표 없음).
--   위치정보 열람 이력 저장 실패 - 관리자 <id> 회원 <id> MISSION_HISTORY_ADDRESS <mission_history.id> (<예외 클래스>)


-- -------------------------------------------------------------------------------------
-- 3) 조회 예시 (필요할 때. 읽기만)
--    시각은 모두 한국 시각입니다. DB의 NOW() 와 비교하지 마세요(DB 시간대가 다를 수 있음).
-- -------------------------------------------------------------------------------------

-- 3-1) 회원 한 명의 위치를 누가 언제 봤는지(최근 순)
-- SELECT accessed_at, admin_user_id, resource_type, resource_id, api
--   FROM admin_location_access_log
--  WHERE target_user_id = <회원 id>
--  ORDER BY accessed_at DESC, id DESC;

-- 3-2) 기간 안의 전체 열람(관리자별 건수)
-- SELECT admin_user_id, COUNT(*) AS views
--   FROM admin_location_access_log
--  WHERE accessed_at >= '2026-11-01 00:00:00' AND accessed_at < '2026-12-01 00:00:00'
--  GROUP BY admin_user_id;

-- 3-3) (보관 기간을 정한 뒤, 예: 6개월) 지난 행 지우기. 지금은 자동으로 지우는 배치가 없습니다.
-- SET SESSION lock_wait_timeout = 5;
-- DELETE FROM admin_location_access_log WHERE accessed_at < '<한국 시각 기준 6개월 전 yyyy-MM-dd 00:00:00>' LIMIT 10000;


-- -------------------------------------------------------------------------------------
-- 롤백
--   코드만 이전 jar로 돌리면 됩니다(옛 코드는 이 표를 모름). 표는 열람 근거라 지우지 않습니다.
--   이 기능을 아예 접고 행을 남길 필요가 없다고 정했을 때만 아래를 실행해 주세요.
--   (운영 ddl-auto: update 라, 새 jar가 떠 있는 동안 지우면 다음 기동 때 Hibernate가 다시 만듭니다. 이전 jar로 돌린 뒤 지우세요.)
-- -------------------------------------------------------------------------------------
-- SET SESSION lock_wait_timeout = 5;
-- DROP TABLE admin_location_access_log;
