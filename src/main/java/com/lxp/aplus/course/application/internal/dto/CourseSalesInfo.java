package com.lxp.aplus.course.application.internal.dto;

import com.lxp.aplus.course.domain.Course;
import com.lxp.aplus.course.domain.CourseStatus;

public record CourseSalesInfo(
        Long courseId,
        int price,
        boolean purchasable
) {
    public static CourseSalesInfo from(Course course) {
        return new CourseSalesInfo(
                course.getId(),
                course.getPrice(),
                course.getCourseStatus() == CourseStatus.PUBLISHED
        );
    }
}
