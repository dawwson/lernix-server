package com.lxp.aplus.course.application.internal.dto;

import com.lxp.aplus.course.domain.Course;
import com.lxp.aplus.course.domain.CourseStatus;

public record CoursePurchaseInfo(
        Long courseId,
        String title,
        String status,
        String instructorName,
        String thumbnailUrl,
        int price,
        boolean purchasable
) {
    public static CoursePurchaseInfo from(Course course, String instructorName) {
        return new CoursePurchaseInfo(
                course.getId(),
                course.getTitle(),
                course.getCourseStatus().name(),
                instructorName,
                course.getThumbnailResourceKey(),
                course.getPrice(),
                course.getCourseStatus() == CourseStatus.PUBLISHED
        );
    }
}
