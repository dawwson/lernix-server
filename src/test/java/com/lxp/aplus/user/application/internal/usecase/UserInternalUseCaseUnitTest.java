package com.lxp.aplus.user.application.internal.usecase;

import com.lxp.aplus.user.application.internal.dto.UserInternalResult;
import com.lxp.aplus.user.application.port.out.UserRepository;
import com.lxp.aplus.user.domain.User;
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
@DisplayName("UserInternalUseCase 단위 테스트")
class UserInternalUseCaseUnitTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserInternalUseCase useCase;

    @Test
    @DisplayName("사용자를 일괄 조회하고 ID 기준 결과를 반환한다")
    void findByIds_Success() {
        List<Long> userIds = List.of(1L, 2L);
        given(userRepository.findByIdIn(userIds)).willReturn(List.of(
                User.builder().id(1L).nickName("강사1").build(),
                User.builder().id(2L).nickName("강사2").build()
        ));

        Map<Long, UserInternalResult> result = useCase.findByIds(userIds);

        then(userRepository).should().findByIdIn(userIds);
        assertThat(result).containsEntry(1L, new UserInternalResult(1L, "강사1", null));
        assertThat(result).containsEntry(2L, new UserInternalResult(2L, "강사2", null));
    }
}
