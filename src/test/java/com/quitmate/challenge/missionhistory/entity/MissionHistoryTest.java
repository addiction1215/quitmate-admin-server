package com.quitmate.challenge.missionhistory.entity;

import com.quitmate.global.exception.QuitmateException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MissionHistoryTest {

    @DisplayName("승인하면 완료 처리하고 완료 시각을 남긴다.")
    @Test
    void judgeApprove() {
        // given
        MissionHistory missionHistory = createSubmittedMission(MissionStatus.READY);

        // when
        missionHistory.judge(true);

        // then
        assertThat(missionHistory.getStatus()).isEqualTo(MissionStatus.COMPLETED);
        assertThat(missionHistory.getCompleteAt()).isNotNull();
        assertThat(missionHistory.getPhotoUrl1()).isEqualTo("photo-1");
    }

    @DisplayName("반려하면 실패로 바꾸고 제출 내용을 비워 처음부터 다시 제출할 수 있게 한다.")
    @Test
    void judgeReject() {
        // given
        MissionHistory missionHistory = createSubmittedMission(MissionStatus.READY);

        // when
        missionHistory.judge(false);

        // then
        assertThat(missionHistory.getStatus()).isEqualTo(MissionStatus.FAILED);
        assertThat(missionHistory.getCompleteAt()).isNull();
        assertThat(missionHistory.getAddresses()).isEmpty();
        assertThat(missionHistory.getGpsVerifyCount()).isZero();
        assertThat(missionHistory.getPhotoUrl1()).isNull();
        assertThat(missionHistory.getPhotoUrl2()).isNull();
        assertThat(missionHistory.getPhotoUrl3()).isNull();
        assertThat(missionHistory.getAbstinenceTime()).isZero();
    }

    @DisplayName("승인 대기 중이 아닌 미션은 심사할 수 없다.")
    @Test
    void judgeNotReady() {
        // given
        MissionHistory completed = createSubmittedMission(MissionStatus.COMPLETED);
        MissionHistory rejected = createSubmittedMission(MissionStatus.FAILED);
        MissionHistory progressing = createSubmittedMission(MissionStatus.PROGRESSING);

        // when // then
        assertThatThrownBy(() -> completed.judge(false))
                .isInstanceOf(QuitmateException.class)
                .hasMessage("승인 대기 중인 미션만 심사할 수 있습니다.");
        assertThatThrownBy(() -> rejected.judge(true))
                .isInstanceOf(QuitmateException.class);
        assertThatThrownBy(() -> progressing.judge(true))
                .isInstanceOf(QuitmateException.class);
        assertThat(completed.getPhotoUrl1()).isEqualTo("photo-1");
    }

    private MissionHistory createSubmittedMission(MissionStatus status) {
        return MissionHistory.builder()
                .status(status)
                .addresses(List.of("서울시 1", "서울시 2", "서울시 3"))
                .gpsVerifyCount(3)
                .photoUrl1("photo-1")
                .photoUrl2("photo-2")
                .photoUrl3("photo-3")
                .abstinenceTime(300)
                .build();
    }
}
