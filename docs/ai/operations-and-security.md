# 운영·보안 (관리자 서버)

앱 서버(addiction1215/addiction)의 `docs/ai/operations-and-security.md`와 같은 자리의 관리자 서버 문서다.
지금은 위치정보 열람 이력(KAN-208)만 적는다. 문서와 코드가 다르면 코드를 사실로 보고 이 문서를 고친다.

## 위치정보 열람 이력 (KAN-208)

관리자 API 응답에 회원의 위치 값(주소·좌표)이 하나라도 나가면 `LocationAccessLogService.record(관리자 userId, 회원 id, 종류, 행 id)`로
`admin_location_access_log`에 한 줄을 남긴다(법무 검토 KAN-136). 지금 위치가 나가는 관리자 API는 미션 이력 상세
`GET /api/v1/admin/mission-history/{id}`의 `locationData.addresses`(GPS "위도,경도") 하나이고, `MissionHistoryReadService`가
`MissionHistoryDetailResponse.containsLocation()`일 때만 부른다. 관리자 userId는 `AdminAccessService.getCurrentAdminId()`(KAN-206)로 읽고,
API(메서드 + 경로 패턴)와 열람 시각(한국 시각)은 서비스가 채운다. **위치 값은 인자로 넘기지도, 표·로그에 남기지도 않는다.**
위치 칸이 비어 있는 응답은 남기지 않는다. 여러 회원의 위치를 한 번에 주는 API를 새로 만들면 회원마다 한 줄씩 남긴다.
저장은 `LocationAccessLogWriter`가 본 조회와 다른 트랜잭션(REQUIRES_NEW)에서 한다. 조회는 readOnly 트랜잭션이라 같은 트랜잭션에서는
MySQL이 INSERT를 거부하고, 실패가 조회까지 되돌리기 때문이다. 저장이 실패해도 예외를 던지지 않고 응답을 그대로 주며,
ERROR 로그 한 줄(관리자 id·회원 id·종류·행 id·예외 클래스 이름, 위치 값 없음)만 남긴다(관리자 서버에는 Slack 알림이 없다).
표는 `docs/db/KAN-208-admin-location-access-log.sql`로 만든다. 운영은 ddl-auto: update라 표가 먼저 없으면 Hibernate가 만들 수 있어서, 엔티티와 DDL을
`AdminLocationAccessLogDdlTest`로 맞춰 둔다. 위치가 나가는 API를 더하면 `MissionHistoryLocationAccessLogApiTest`처럼
이력 한 줄·위치 값 없음·관리자 아니면 403과 이력 없음·저장 실패해도 200을 확인한다.

### 보관 기간과 자동 삭제 (KAN-208)

기획 결정: **이력은 6개월 보관하고, 지난 이력은 자동 삭제하며, 열람 권한은 관리자만 갖는다.**

- `LocationAccessLogRetentionScheduler`가 매일 04:40(한국 시각, `zone = "Asia/Seoul"`)에 `LocationAccessLogRetentionService.purgeExpired()`를 부른다.
  `accessed_at < (지금 한국 시각 - 6개월)`인 행만 지우고(기준 시각과 같은 행은 남김), 한 트랜잭션에 1000건씩 나눠 지울 것이 없을 때까지 반복한다
  (기존 인덱스 `idx_admin_location_access_log_accessed` 사용).
- 보관 개월 수는 설정 `location-access-log.retention-months`(기본 6)다. application-*.yml 에는 적지 않았고 코드 기본값만 쓴다.
- 멱등이다. 무중단 배포 중 WAS 두 개가 겹쳐 같이 돌아도 이미 지워진 행은 0건일 뿐이다. 관리자 서버의 첫 `@Scheduled`라
  `SchedulingConfig`(`@EnableScheduling`)를 새로 더했고, ShedLock 같은 분산 잠금은 두지 않았다(운영은 서버 한 대, scripts/prd 기준).
- 지운 건수는 INFO 한 줄, 실패는 ERROR 한 줄(예외 클래스 이름만)이다. 회원 id·주소 등은 남기지 않는다. 예외는 스케줄러에서 잡아 전역 핸들러로 새지 않는다.
- 열람 권한: 이 이력을 조회하는 API는 없다(만들지 않음). 관리자 서버 `SecurityConfig`는 공개 경로 외 모든 요청을 `hasAuthority(ADMIN)`으로만 연다(KAN-206).
  이력을 보는 API를 더하면 같은 설정을 지나야 하고, 응답에 위치 값이 없어도 접근 자체를 이력 대상으로 볼지 다시 정한다.
- 표 구조는 바뀌지 않아 DDL 파일은 그대로다(주석에 보관 6개월만 적음). 테스트는 `LocationAccessLogRetentionTest`
  (6개월 경계·빈 표·묶음 반복·스케줄 식).
