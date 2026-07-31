package com.lxp.aplus.user.application.internal.usecase;

import com.lxp.aplus.user.application.internal.dto.UserInternalResult;
import com.lxp.aplus.user.application.port.out.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserInternalUseCase {
    private final UserRepository userRepository;

    public Optional<UserInternalResult> findById(Long id) {
        return userRepository.findById(id)
                .map(UserInternalResult::from);
    }

    public Map<Long, UserInternalResult> findByIds(List<Long> ids) {
        return userRepository.findByIdIn(ids).stream()
                .map(UserInternalResult::from)
                .collect(Collectors.toMap(
                        UserInternalResult::id,
                        user -> user
                ));
    }
}
