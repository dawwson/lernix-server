package com.lxp.aplus.course.application.port.in.order.model;

import com.lxp.aplus.course.domain.Course;

public record CoursePrice(
        Long courseId,
        int price
) {
    public static CoursePrice from(Course course) {
        return new CoursePrice(course.getId(), course.getPrice());
    }
}
