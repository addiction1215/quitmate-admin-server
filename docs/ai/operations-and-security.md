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
표는 `docs/db/KAN-208-admin-location-access-log.sql`로 만든다. 운영은 ddl-auto: update, dev는 none이라 엔티티와 DDL을
`AdminLocationAccessLogDdlTest`로 맞춰 둔다. 위치가 나가는 API를 더하면 `MissionHistoryLocationAccessLogApiTest`처럼
이력 한 줄·위치 값 없음·관리자 아니면 403과 이력 없음·저장 실패해도 200을 확인한다.
