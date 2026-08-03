package com.lxp.aplus.course.application.service;

import com.lxp.aplus.common.error.BusinessException;
import com.lxp.aplus.common.error.code.CourseErrorCode;
import com.lxp.aplus.course.application.port.in.order.model.CoursePrice;
import com.lxp.aplus.course.domain.Course;
import com.lxp.aplus.course.domain.CourseRepository;
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
    @DisplayName("요청한 강좌가 모두 존재하면 강좌별 가격을 반환한다")
    void getCoursePrices_Success() {
        List<Long> courseIds = List.of(10L, 20L);
        List<Course> courses = List.of(
                Course.builder().id(10L).price(10000).build(),
                Course.builder().id(20L).price(30000).build()
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
    void getCoursePrices_CourseNotFound() {
        List<Long> courseIds = List.of(10L, 20L);
        given(courseRepository.findByIdIn(courseIds))
                .willReturn(List.of(Course.builder().id(10L).price(10000).build()));

        assertThatThrownBy(() -> service.getCoursePrices(courseIds))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(CourseErrorCode.COURSE_NOT_FOUND);
    }
}
