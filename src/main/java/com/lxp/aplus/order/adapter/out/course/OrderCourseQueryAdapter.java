package com.lxp.aplus.order.adapter.out.course;

import com.lxp.aplus.course.application.port.in.order.CourseQueryToOrderUseCase;
import com.lxp.aplus.order.application.port.out.course.OrderCourseQueryPort;
import com.lxp.aplus.order.application.port.out.course.model.PurchasableCourse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class OrderCourseQueryAdapter implements OrderCourseQueryPort {

    private final CourseQueryToOrderUseCase courseQueryToOrderUseCase;

    @Override
    public List<PurchasableCourse> getPurchasableCourses(List<Long> courseIds) {
        // TODO: try-catch + Course Error -> Order Error로 변환
        //       경계 계약상 예상되는 비즈니스 오류만 선택적으로 변환
        return courseQueryToOrderUseCase.getCoursePrices(courseIds).stream()
                .map(coursePrice -> new PurchasableCourse(coursePrice.courseId(), coursePrice.price()))
                .toList();
    }
}
