package com.quitmate.user.userPass.repository;

import com.quitmate.user.userPass.entity.UserPass;
import com.quitmate.user.userPass.enums.UserPassProduct;
import com.quitmate.user.userPass.enums.UserPassStatus;
import com.quitmate.user.userPass.repository.response.UserPassDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserPassRepository {

    UserPass save(UserPass userPass);

    Optional<UserPass> findById(Long id);

    Optional<UserPass> findByIdForUpdate(Long id);

    List<UserPass> findOverlapping(Long userId, UserPassProduct product, LocalDateTime startsAt,
                                   LocalDateTime endsAt);

    Page<UserPassDto> findUserPassList(Long userId, UserPassStatus status, LocalDateTime now, Pageable pageable);
}
