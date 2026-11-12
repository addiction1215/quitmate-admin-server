package com.quitmate.smokingPattern.repository.response;

import java.time.LocalDateTime;

/** 회수되지 않은 QuitMate 90 이용권 하나의 기간 [startsAt, endsAt) */
public record PassPeriod(LocalDateTime startsAt, LocalDateTime endsAt) {

    /** [from, to) 와 하루라도(한순간이라도) 겹치는지 */
    public boolean overlaps(LocalDateTime from, LocalDateTime to) {
        return startsAt.isBefore(to) && endsAt.isAfter(from);
    }
}
