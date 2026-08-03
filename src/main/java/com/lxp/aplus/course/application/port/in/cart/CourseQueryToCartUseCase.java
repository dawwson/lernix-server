package com.lxp.aplus.course.application.port.in.cart;

import com.lxp.aplus.course.application.port.in.cart.model.CoursePurchaseInfo;
import com.lxp.aplus.course.application.port.in.cart.model.CourseSalesInfo;

import java.util.List;
import java.util.Map;

public interface CourseQueryToCartUseCase {

    List<CourseSalesInfo> getCourseSalesInfoByIds(List<Long> courseIds);

    Map<Long, CoursePurchaseInfo> getCoursePurchaseInfoByIds(List<Long> courseIds);
}
