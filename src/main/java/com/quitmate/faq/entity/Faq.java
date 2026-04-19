package com.quitmate.faq.entity;

import com.quitmate.global.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
public class Faq extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String useYn;

    @Builder
    public Faq(Long id, String title, String description) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.useYn = "Y";
    }

    public void update(String title, String description) {
        this.title = title;
        this.description = description;
    }

    public void delete() {
        this.useYn = "N";
    }
}
