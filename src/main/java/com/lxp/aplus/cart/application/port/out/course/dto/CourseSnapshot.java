package com.lxp.aplus.cart.application.port.out.course.dto;

import lombok.Builder;

@Builder
public record CourseSnapshot(
        Long courseId,
        String courseTitle,
        String courseStatus,
        String instructorName,
        String thumbnailUrl,
        int price
) {
}
