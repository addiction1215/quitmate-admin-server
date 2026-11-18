package com.quitmate.locationAccessLog.enums;

/**
 * 관리자가 열람한 위치정보의 종류(KAN-208). admin_location_access_log.resource_type 에 이름 그대로 저장한다.
 * 칸이 MySQL ENUM 이 아닌 varchar(40)이라 값을 더해도 DDL 이 필요 없다. 이름은 40자를 넘기지 않는다.
 */
public enum LocationAccessResourceType {

    /** 미션 위치 인증 좌표(mission_history_address, 앱이 보낸 "위도,경도"). resource_id = mission_history.id */
    MISSION_HISTORY_ADDRESS
}
