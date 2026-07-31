package com.lxp.aplus.cart.adapter.out.course;

import com.lxp.aplus.cart.application.port.out.course.CartCourseQueryPort;
import com.lxp.aplus.cart.application.port.out.course.model.CourseSalesStatus;
import com.lxp.aplus.cart.application.port.out.course.model.CourseSnapshot;
import com.lxp.aplus.course.application.internal.dto.CoursePurchaseInfo;
import com.lxp.aplus.course.application.internal.dto.CourseSalesInfo;
import com.lxp.aplus.course.application.internal.usecase.CourseQueryToCartUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Component
@RequiredArgsConstructor
public class CartCourseQueryAdapter implements CartCourseQueryPort {

    private final CourseQueryToCartUseCase courseQueryToCartUseCase;

    public Map<Long, CourseSalesStatus> getCourseSalesStatusByIds(List<Long> courseIds) {
        return courseQueryToCartUseCase.getCourseSalesInfoByIds(courseIds).stream()
                .collect(Collectors.toMap(
                        CourseSalesInfo::courseId,
                        course -> new CourseSalesStatus(
                                course.price(),
                                course.purchasable()
                        )
                ));
    }

    @Override
    public Map<Long, CourseSnapshot> getCourseSnapshotByIds(List<Long> courseIds) {
        return courseQueryToCartUseCase.getCoursePurchaseInfoByIds(courseIds).values().stream()
                .collect(Collectors.toMap(
                        CoursePurchaseInfo::courseId,
                        course -> CourseSnapshot.builder()
                                .courseId(course.courseId())
                                .courseTitle(course.title())
                                .courseStatus(course.status())
                                .instructorName(course.instructorName())
                                .thumbnailUrl(course.thumbnailUrl())
                                .price(course.price())
                                .purchasable(course.purchasable())
                                .build()
                ));
    }
}
