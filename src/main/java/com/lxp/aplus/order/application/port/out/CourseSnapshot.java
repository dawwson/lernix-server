package com.lxp.aplus.order.application.port.out;

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
