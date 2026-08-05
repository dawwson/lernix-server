package com.lxp.aplus.course.application.service;

import com.lxp.aplus.common.error.BusinessException;
import com.lxp.aplus.common.error.code.CourseErrorCode;
import com.lxp.aplus.course.application.port.in.order.model.CoursePrice;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("CourseQueryToOrderService 단위 테스트")
class CourseQueryToOrderServiceUnitTest {

    @Mock
    private CourseRepository courseRepository;

    @InjectMocks
    private CourseQueryToOrderService service;

    @Test
    @DisplayName("요청한 강좌가 모두 판매 중이면 강좌별 가격을 반환한다")
    void getCoursePrices_publishedCourses_returnsPrices() {
        List<Long> courseIds = List.of(10L, 20L);
        List<Course> courses = List.of(
                createCourse(10L, 10000, CourseStatus.PUBLISHED),
                createCourse(20L, 30000, CourseStatus.PUBLISHED)
        );
        given(courseRepository.findByIdIn(courseIds)).willReturn(courses);

        List<CoursePrice> result = service.getCoursePrices(courseIds);

        then(courseRepository).should().findByIdIn(courseIds);
        assertThat(result).containsExactly(
                new CoursePrice(10L, 10000),
                new CoursePrice(20L, 30000)
        );
    }

    @Test
    @DisplayName("요청한 강좌 중 일부가 없으면 COURSE_NOT_FOUND 예외를 발생시킨다")
    void getCoursePrices_missingCourse_throwsCourseNotFound() {
        List<Long> courseIds = List.of(10L, 20L);
        given(courseRepository.findByIdIn(courseIds))
                .willReturn(List.of(createCourse(10L, 10000, CourseStatus.PUBLISHED)));

        assertCourseError(courseIds, CourseErrorCode.COURSE_NOT_FOUND);
    }

    @Test
    @DisplayName("작성 중인 강좌가 포함되면 COURSE_NOT_PURCHASABLE 예외를 발생시킨다")
    void getCoursePrices_draftCourse_throwsCourseNotPurchasable() {
        List<Long> courseIds = List.of(10L);
        given(courseRepository.findByIdIn(courseIds))
                .willReturn(List.of(createCourse(10L, 10000, CourseStatus.DRAFT)));

        assertCourseError(courseIds, CourseErrorCode.COURSE_NOT_PURCHASABLE);
    }

    @Test
    @DisplayName("삭제된 강좌가 포함되면 COURSE_NOT_PURCHASABLE 예외를 발생시킨다")
    void getCoursePrices_deletedCourse_throwsCourseNotPurchasable() {
        List<Long> courseIds = List.of(10L);
        given(courseRepository.findByIdIn(courseIds))
                .willReturn(List.of(createCourse(10L, 10000, CourseStatus.DELETED)));

        assertCourseError(courseIds, CourseErrorCode.COURSE_NOT_PURCHASABLE);
    }

    @Test
    @DisplayName("여러 강좌 중 하나라도 판매 중이 아니면 전체 가격 조회를 거부한다")
    void getCoursePrices_mixedCourseStatuses_throwsCourseNotPurchasable() {
        List<Long> courseIds = List.of(10L, 20L);
        given(courseRepository.findByIdIn(courseIds)).willReturn(List.of(
                createCourse(10L, 10000, CourseStatus.PUBLISHED),
                createCourse(20L, 30000, CourseStatus.DRAFT)
        ));

        assertCourseError(courseIds, CourseErrorCode.COURSE_NOT_PURCHASABLE);
    }

    private Course createCourse(Long courseId, int price, CourseStatus status) {
        return Course.builder()
                .id(courseId)
                .price(price)
                .courseStatus(status)
                .build();
    }

    private void assertCourseError(List<Long> courseIds, CourseErrorCode errorCode) {
        assertThatThrownBy(() -> service.getCoursePrices(courseIds))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(errorCode);
    }
}
