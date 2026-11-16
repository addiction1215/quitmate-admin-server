package com.quitmate.storage.enums;

import lombok.Getter;

@Getter
public enum BucketKind {

    USER,
    CHALLENGE,
    CHALLENGE_BADGE,
    INQUIRY,
    /** 공지 본문 이미지. 전용 버킷이 아직 없어 CHALLENGE_BADGE 버킷에 "notice/" 접두사로 둔다(S3StorageService). */
    NOTICE

}
