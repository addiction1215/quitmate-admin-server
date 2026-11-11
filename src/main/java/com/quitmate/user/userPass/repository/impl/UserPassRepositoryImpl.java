package com.quitmate.user.userPass.repository.impl;

import com.quitmate.user.userPass.entity.UserPass;
import com.quitmate.user.userPass.enums.UserPassProduct;
import com.quitmate.user.userPass.enums.UserPassStatus;
import com.quitmate.user.userPass.repository.UserPassJpaRepository;
import com.quitmate.user.userPass.repository.UserPassQueryRepository;
import com.quitmate.user.userPass.repository.UserPassRepository;
import com.quitmate.user.userPass.repository.response.UserPassDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class UserPassRepositoryImpl implements UserPassRepository {

    private final UserPassJpaRepository userPassJpaRepository;
    private final UserPassQueryRepository userPassQueryRepository;

    @Override
    public UserPass save(UserPass userPass) {
        return userPassJpaRepository.save(userPass);
    }

    @Override
    public Optional<UserPass> findById(Long id) {
        return userPassJpaRepository.findById(id);
    }

    @Override
    public Optional<UserPass> findByIdForUpdate(Long id) {
        return userPassJpaRepository.findByIdForUpdate(id);
    }

    @Override
    public List<UserPass> findOverlapping(Long userId, UserPassProduct product, LocalDateTime startsAt,
                                          LocalDateTime endsAt) {
        return userPassJpaRepository.findOverlapping(userId, product, startsAt, endsAt);
    }

    @Override
    public Page<UserPassDto> findUserPassList(Long userId, UserPassStatus status, LocalDateTime now,
                                              Pageable pageable) {
        return userPassQueryRepository.findUserPassList(userId, status, now, pageable);
    }
}
