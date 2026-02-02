package com.quitmate.user.users.entity;

import com.quitmate.global.BaseTimeEntity;
import com.quitmate.user.push.entity.Push;
import com.quitmate.user.users.entity.enums.Role;
import com.quitmate.user.users.entity.enums.SettingStatus;
import com.quitmate.user.users.entity.enums.Sex;
import com.quitmate.user.users.entity.enums.SnsType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "users")
public class User extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String email;

    private String password;

    private String nickName;

    private String phoneNumber;

    private String birthDay;

    private String purpose;

    private Integer totalScore;

    private String introduction;

    private Integer cigarettePrice;

    private LocalDateTime startDate;

    private String profileUrl;

    @Enumerated(EnumType.STRING)
    private SnsType snsType;

    @Enumerated(EnumType.STRING)
    private Sex sex;

    @Enumerated(EnumType.STRING)
    private Role role;

    @Enumerated(EnumType.STRING)
    private SettingStatus settingStatus;

    private String useYn;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Push> pushes = new ArrayList<>();

    @Builder
    public User(String profileUrl, String email, String password, String nickName, String phoneNumber, String birthDay, String purpose,
                Integer totalScore, String introduction, Integer cigarettePrice, SnsType snsType, Sex sex, Role role,
                SettingStatus settingStatus) {
        this.profileUrl = profileUrl;
        this.email = email;
        this.password = password;
        this.nickName = nickName;
        this.phoneNumber = phoneNumber;
        this.birthDay = birthDay;
        this.purpose = purpose;
        this.totalScore = totalScore;
        this.introduction = introduction;
        this.cigarettePrice = cigarettePrice;
        this.snsType = snsType;
        this.sex = sex;
        this.role = role;
        this.settingStatus = settingStatus;
        this.useYn = "Y";
    }

}
