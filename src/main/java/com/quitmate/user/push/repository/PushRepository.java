package com.quitmate.user.push.repository;


import com.quitmate.user.push.entity.Push;

import java.util.List;

public interface PushRepository {
    void deleteAllInBatch();

    Push save(Push push);

    void saveAll(List<Push> pushes);
}
