package com.lxp.aplus.course.infrastructure.adapter.module;

import com.lxp.aplus.course.application.port.out.UserQueryPort;
import com.lxp.aplus.course.application.result.InstructorResult;
import com.lxp.aplus.user.application.internal.usecase.UserInternalUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class CourseUserModuleAdapter implements UserQueryPort {
    private final UserInternalUseCase userInternalUseCase;

    @Override
    public Optional<InstructorResult> findInstructorById(Long userId) {
        return userInternalUseCase.findById(userId)
                .map(dto -> InstructorResult.builder()
                        .id(dto.id())
                        .nickName(dto.nickName())
                        .build());
    }

    @Override
    public Map<Long, InstructorResult> findInstructorsByIds(List<Long> userIds) {
        return userInternalUseCase.findByIds(userIds).values().stream()
                .map(dto -> InstructorResult.builder()
                        .id(dto.id())
                        .nickName(dto.nickName())
                        .build())
                .collect(Collectors.toMap(
                        InstructorResult::id,
                        instructor -> instructor
                ));
    }
}
