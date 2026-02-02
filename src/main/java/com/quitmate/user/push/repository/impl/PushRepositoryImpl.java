package com.quitmate.user.push.repository.impl;

import com.quitmate.user.push.entity.Push;
import com.quitmate.user.push.repository.PushJpaRepository;
import com.quitmate.user.push.repository.PushRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class PushRepositoryImpl implements PushRepository {

    private final PushJpaRepository pushJpaRepository;

    @Override
    public void deleteAllInBatch() {
        pushJpaRepository.deleteAllInBatch();
    }

    @Override
    public Push save(Push push) {
        return pushJpaRepository.save(push);
    }

    @Override
    public void saveAll(List<Push> pushes) {
        pushJpaRepository.saveAll(pushes);
    }
}
