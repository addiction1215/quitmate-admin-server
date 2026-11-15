package com.quitmate.challenge.challange.service;

import com.quitmate.IntegrationTestSupport;
import com.quitmate.challenge.challange.controller.request.SortType;
import com.quitmate.challenge.challange.entity.Challenge;
import com.quitmate.challenge.challange.repository.ChallengeJpaRepository;
import com.quitmate.challenge.challange.service.request.ChallengeSearchServiceRequest;
import com.quitmate.challenge.challange.service.response.ChallengeListResponse;
import com.quitmate.challenge.challengehistory.entity.ChallengeHistory;
import com.quitmate.challenge.challengehistory.entity.ChallengeStatus;
import com.quitmate.challenge.mission.entity.Mission;
import com.quitmate.challenge.mission.entity.MissionCategoryStatus;
import com.quitmate.challenge.mission.repository.MissionJpaRepository;
import com.quitmate.global.page.response.PageCustom;
import com.quitmate.user.users.entity.User;
import com.quitmate.user.users.entity.enums.SettingStatus;
import com.quitmate.user.users.entity.enums.SnsType;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

class ChallengeReadServiceTest extends IntegrationTestSupport {

    @Autowired
    private ChallengeReadService challengeReadService;
    @Autowired
    private ChallengeJpaRepository challengeJpaRepository;
    @Autowired
    private MissionJpaRepository missionJpaRepository;
    @Autowired
    private EntityManager entityManager;
    @Autowired
    private TransactionTemplate transactionTemplate;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private int userSequence = 0;

    @AfterEach
    void deleteChallenges() {
        jdbcTemplate.update("delete from challenge_history");
        missionJpaRepository.deleteAllInBatch();
        challengeJpaRepository.deleteAllInBatch();
    }

    @DisplayName("미션 여러 개·참여 여러 건인 챌린지도 미션 수와 참여 수를 서로 곱하지 않고 센다")
    @Test
    void countsMissionsAndParticipationsSeparately() {
        // given: 사용 중 미션 3개(+삭제된 미션 1개) × 참여 4건이면, 곱해질 때 둘 다 12가 된다
        Challenge busy = saveChallenge("미션 많은 챌린지");
        saveMissions(busy, 3);
        missionJpaRepository.save(deletedMission(busy));
        saveParticipations(busy, 4);
        Challenge empty = saveChallenge("빈 챌린지");

        // when
        PageCustom<ChallengeListResponse> page = challengeReadService.getChallengeList(request(SortType.ID));

        // then
        assertThat(page.getContent())
                .extracting(ChallengeListResponse::getChallengeId, ChallengeListResponse::getMissionCount,
                        ChallengeListResponse::getAccumulatedCount)
                .containsExactly(
                        tuple(busy.getId(), 3L, 4L),
                        tuple(empty.getId(), 0L, 0L)
                );
        assertThat(page.getPageInfo().getTotalElement()).isEqualTo(2);
    }

    @DisplayName("참여자 순 정렬은 미션 수가 곱해지지 않은 참여 수로 정렬한다")
    @Test
    void sortsByParticipationCountWithoutMissionMultiplication() {
        // given: 곱해지면 미션 3 × 참여 2 = 6 이 미션 1 × 참여 4 = 4 보다 앞선다
        Challenge manyMissions = saveChallenge("미션 3개·참여 2건");
        saveMissions(manyMissions, 3);
        saveParticipations(manyMissions, 2);
        Challenge manyParticipants = saveChallenge("미션 1개·참여 4건");
        saveMissions(manyParticipants, 1);
        saveParticipations(manyParticipants, 4);

        // when
        PageCustom<ChallengeListResponse> page = challengeReadService.getChallengeList(request(SortType.USER));

        // then
        assertThat(page.getContent())
                .extracting(ChallengeListResponse::getChallengeId, ChallengeListResponse::getMissionCount,
                        ChallengeListResponse::getAccumulatedCount)
                .containsExactly(
                        tuple(manyParticipants.getId(), 1L, 4L),
                        tuple(manyMissions.getId(), 3L, 2L)
                );
    }

    private ChallengeSearchServiceRequest request(SortType sortType) {
        return ChallengeSearchServiceRequest.builder()
                .sortType(sortType)
                .page(1)
                .size(10)
                .build();
    }

    private Challenge saveChallenge(String title) {
        return challengeJpaRepository.save(Challenge.builder()
                .title(title)
                .content(title + " 설명")
                .badge("badge/" + title)
                .reward(100)
                .build());
    }

    private void saveMissions(Challenge challenge, int count) {
        for (int i = 1; i <= count; i++) {
            missionJpaRepository.save(mission(challenge, "미션 " + i));
        }
    }

    private Mission mission(Challenge challenge, String title) {
        return Mission.builder()
                .challenge(challenge)
                .category(MissionCategoryStatus.REPLACE_ACTION)
                .title(title)
                .reward(10)
                .content(title + " 설명")
                .build();
    }

    private Mission deletedMission(Challenge challenge) {
        Mission mission = mission(challenge, "삭제된 미션");
        mission.delete();
        return mission;
    }

    private void saveParticipations(Challenge challenge, int count) {
        for (int i = 0; i < count; i++) {
            userSequence++;
            User user = userRepository.save(
                    createUser("member" + userSequence + "@test.com", "1234", SnsType.NORMAL, SettingStatus.COMPLETE));
            transactionTemplate.executeWithoutResult(status -> entityManager.persist(ChallengeHistory.builder()
                    .challenge(challenge)
                    .user(user)
                    .status(ChallengeStatus.PROGRESSING)
                    .build()));
        }
    }
}
