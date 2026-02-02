package com.quitmate.user.push.entity;

import com.quitmate.global.BaseTimeEntity;
import com.quitmate.user.users.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
public class Push extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String deviceId;

    private String pushToken;

    @ManyToOne(fetch = FetchType.LAZY)
    private User user;

    @Builder
    private Push(String deviceId, User user, String pushToken) {
        this.deviceId = deviceId;
        this.user = user;
        this.pushToken = pushToken;
    }

    public static Push of(String deviceId, User user, String pushToken) {
        return Push.builder()
                .deviceId(deviceId)
                .user(user)
                .pushToken(pushToken)
                .build();
    }

    public void updatePushToken(String pushToken) {
        this.pushToken = pushToken;
    }

    public void updateUser(User user) {
        this.user = user;
        user.getPushes().add(this);
    }
}
