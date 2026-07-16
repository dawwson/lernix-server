package com.lxp.aplus.course.infrastructure.adapter.module;

import com.lxp.aplus.course.application.result.InstructorResult;
import com.lxp.aplus.user.application.internal.dto.UserInternalResult;
import com.lxp.aplus.user.application.internal.usecase.UserInternalUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("CourseUserModuleAdapter 단위 테스트")
class CourseUserModuleAdapterUnitTest {

    @Mock
    private UserInternalUseCase userInternalUseCase;

    @InjectMocks
    private CourseUserModuleAdapter adapter;

    @Test
    @DisplayName("User 내부 결과를 Course의 강사 결과로 일괄 변환한다")
    void findInstructorsByIds_Success() {
        List<Long> instructorIds = List.of(1L, 2L);
        given(userInternalUseCase.findByIds(instructorIds)).willReturn(Map.of(
                1L, new UserInternalResult(1L, "강사1", "one@example.com"),
                2L, new UserInternalResult(2L, "강사2", "two@example.com")
        ));

        Map<Long, InstructorResult> result = adapter.findInstructorsByIds(instructorIds);

        then(userInternalUseCase).should().findByIds(instructorIds);
        assertThat(result.get(1L).nickName()).isEqualTo("강사1");
        assertThat(result.get(2L).nickName()).isEqualTo("강사2");
    }
}
