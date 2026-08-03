package com.lxp.aplus.course.application.port.in.order;

import com.lxp.aplus.course.application.port.in.order.model.CoursePrice;

import java.util.List;

public interface CourseQueryToOrderUseCase {

    List<CoursePrice> getCoursePrices(List<Long> courseIds);
}
