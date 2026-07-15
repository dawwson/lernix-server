package com.lxp.aplus.order.application.port.out.course;

import com.lxp.aplus.order.application.port.out.course.model.CoursePrice;

import java.util.List;

public interface CourseQueryPort {

    List<CoursePrice> getCoursePriceByIds(List<Long> courseIds);
}
