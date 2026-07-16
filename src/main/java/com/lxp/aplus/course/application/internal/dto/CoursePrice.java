package com.lxp.aplus.course.application.internal.dto;

import com.lxp.aplus.course.domain.Course;

public record CoursePrice(
        Long courseId,
        int price
) {
    public static CoursePrice from(Course course) {
        return new CoursePrice(course.getId(), course.getPrice());
    }
}
