package com.lxp.aplus.order.adapter.out.course;

import com.lxp.aplus.common.error.BusinessException;
import com.lxp.aplus.common.error.code.CourseErrorCode;
import com.lxp.aplus.course.application.port.in.order.CourseQueryToOrderUseCase;
import com.lxp.aplus.course.application.port.in.order.model.CoursePrice;
import com.lxp.aplus.order.application.port.out.course.model.PurchasableCourse;
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
@DisplayName("OrderCourseQueryAdapter 단위 테스트")
class OrderCourseQueryAdapterUnitTest {

    @Mock
    private CourseQueryToOrderUseCase courseQueryToOrderUseCase;

    @InjectMocks
    private OrderCourseQueryAdapter adapter;

    @Test
    @DisplayName("Course 가격 결과를 Order의 구매 가능 강좌 모델로 변환한다")
    void getPurchasableCourses_Success() {
        List<Long> courseIds = List.of(10L, 20L);
        given(courseQueryToOrderUseCase.getCoursePrices(courseIds)).willReturn(List.of(
                new CoursePrice(10L, 10000),
                new CoursePrice(20L, 30000)
        ));

        List<PurchasableCourse> result = adapter.getPurchasableCourses(courseIds);

        then(courseQueryToOrderUseCase).should().getCoursePrices(courseIds);
        assertThat(result).containsExactly(
                new PurchasableCourse(10L, 10000),
                new PurchasableCourse(20L, 30000)
        );
    }

    @Test
    @DisplayName("Course가 판매 불가 예외를 반환하면 변경하지 않고 전달한다")
    void getPurchasableCourses_unpurchasableCourse_propagatesException() {
        List<Long> courseIds = List.of(10L, 20L);
        BusinessException exception = new BusinessException(CourseErrorCode.COURSE_NOT_PURCHASABLE);
        given(courseQueryToOrderUseCase.getCoursePrices(courseIds)).willThrow(exception);

        assertThatThrownBy(() -> adapter.getPurchasableCourses(courseIds))
                .isSameAs(exception);
    }
}
