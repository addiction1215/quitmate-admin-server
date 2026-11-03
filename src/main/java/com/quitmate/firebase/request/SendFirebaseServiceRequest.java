package com.quitmate.firebase.request;

import com.quitmate.user.push.entity.Push;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class SendFirebaseServiceRequest {
    private Push push;
    private String sound;
    private String title;
    private String body;
    private SendFirebaseDataDto sendFirebaseDataDto;

    @Builder
    private SendFirebaseServiceRequest(Push push, String sound, String title, String body, SendFirebaseDataDto sendFirebaseDataDto) {
        this.push = push;
        this.sound = sound;
        this.title = title;
        this.body = body;
        this.sendFirebaseDataDto = sendFirebaseDataDto;
    }
}
