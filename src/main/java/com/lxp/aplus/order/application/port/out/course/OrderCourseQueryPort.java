package com.lxp.aplus.order.application.port.out.course;

import com.lxp.aplus.order.application.port.out.course.model.PurchasableCourse;

import java.util.List;

public interface OrderCourseQueryPort {

    List<PurchasableCourse> getPurchasableCourses(List<Long> courseIds);
}
