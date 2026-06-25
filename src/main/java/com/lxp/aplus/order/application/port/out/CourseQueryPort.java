package com.lxp.aplus.order.application.port.out;

import java.util.List;

public interface CourseQueryPort {

    List<CoursePrice> getCoursePriceByIds(List<Long> courseIds);
}
