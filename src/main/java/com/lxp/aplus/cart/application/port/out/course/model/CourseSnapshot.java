package com.lxp.aplus.cart.application.port.out.course.model;

import lombok.Builder;

@Builder
public record CourseSnapshot(
        Long courseId,
        String courseTitle,
        String courseStatus,
        String instructorName,
        String thumbnailUrl,
        int price,
        boolean purchasable
) {
}
