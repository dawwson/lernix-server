package com.lxp.aplus.course.application.internal.usecase;

import com.lxp.aplus.common.error.BusinessException;
import com.lxp.aplus.common.error.code.CourseErrorCode;
import com.lxp.aplus.common.error.code.UserErrorCode;
import com.lxp.aplus.course.application.internal.dto.CoursePurchaseInfo;
import com.lxp.aplus.course.application.internal.dto.CourseSalesInfo;
import com.lxp.aplus.course.application.port.out.UserQueryPort;
import com.lxp.aplus.course.application.result.InstructorResult;
import com.lxp.aplus.course.domain.Course;
import com.lxp.aplus.course.domain.CourseRepository;
import com.lxp.aplus.course.domain.CourseStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
@DisplayName("CourseQueryToCartUseCase 단위 테스트")
class CourseQueryToCartUseCaseUnitTest {

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private UserQueryPort userQueryPort;

    @InjectMocks
    private CourseQueryToCartUseCase useCase;

    @Test
    @DisplayName("판매 정보는 강사 조회 없이 List로 반환한다")
    void getCourseSalesInfoByIds_Success() {
        List<Long> courseIds = List.of(10L, 20L);
        given(courseRepository.findByIdIn(courseIds)).willReturn(List.of(
                createCourse(10L, 1L, CourseStatus.PUBLISHED, 10000),
                createCourse(20L, 2L, CourseStatus.DELETED, 30000)
        ));

        List<CourseSalesInfo> result = useCase.getCourseSalesInfoByIds(courseIds);

        assertThat(result).containsExactly(
                new CourseSalesInfo(10L, 10000, true),
                new CourseSalesInfo(20L, 30000, false)
        );
        then(userQueryPort).should(never()).findInstructorsByIds(org.mockito.ArgumentMatchers.anyList());
    }

    @Test
    @DisplayName("강사 ID를 일괄 조회하고 구매 상세 정보를 조합한다")
    void getCoursePurchaseInfoByIds_Success() {
        List<Long> courseIds = List.of(10L, 20L);
        List<Long> instructorIds = List.of(1L, 1L);
        given(courseRepository.findByIdIn(courseIds)).willReturn(List.of(
                createCourse(10L, 1L, CourseStatus.PUBLISHED, 10000),
                createCourse(20L, 1L, CourseStatus.DRAFT, 30000)
        ));
        given(userQueryPort.findInstructorsByIds(instructorIds)).willReturn(Map.of(
                1L, new InstructorResult(1L, "강사1")
        ));

        Map<Long, CoursePurchaseInfo> result = useCase.getCoursePurchaseInfoByIds(courseIds);

        then(userQueryPort).should().findInstructorsByIds(instructorIds);
        assertThat(result.get(10L).instructorName()).isEqualTo("강사1");
        assertThat(result.get(10L).purchasable()).isTrue();
        assertThat(result.get(20L).purchasable()).isFalse();
    }

    @Test
    @DisplayName("구매 상세 조회에서 강좌가 누락되면 COURSE_NOT_FOUND 예외를 발생시킨다")
    void getCoursePurchaseInfoByIds_CourseNotFound() {
        List<Long> courseIds = List.of(10L, 20L);
        given(courseRepository.findByIdIn(courseIds))
                .willReturn(List.of(createCourse(10L, 1L, CourseStatus.PUBLISHED, 10000)));

        assertBusinessError(
                () -> useCase.getCoursePurchaseInfoByIds(courseIds),
                CourseErrorCode.COURSE_NOT_FOUND
        );
        then(userQueryPort).should(never()).findInstructorsByIds(org.mockito.ArgumentMatchers.anyList());
    }

    @Test
    @DisplayName("구매 상세 조회에서 강사가 누락되면 USER_NOT_FOUND 예외를 발생시킨다")
    void getCoursePurchaseInfoByIds_InstructorNotFound() {
        List<Long> courseIds = List.of(10L);
        given(courseRepository.findByIdIn(courseIds))
                .willReturn(List.of(createCourse(10L, 1L, CourseStatus.PUBLISHED, 10000)));
        given(userQueryPort.findInstructorsByIds(List.of(1L))).willReturn(Map.of());

        assertBusinessError(
                () -> useCase.getCoursePurchaseInfoByIds(courseIds),
                UserErrorCode.USER_NOT_FOUND
        );
    }

    private Course createCourse(
            Long courseId,
            Long instructorId,
            CourseStatus status,
            int price
    ) {
        return Course.builder()
                .id(courseId)
                .instructorId(instructorId)
                .title("강좌 " + courseId)
                .thumbnailResourceKey("thumbnail-" + courseId)
                .courseStatus(status)
                .price(price)
                .build();
    }

    private void assertBusinessError(Runnable action, Object errorCode) {
        assertThatThrownBy(action::run)
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(errorCode);
    }
}
