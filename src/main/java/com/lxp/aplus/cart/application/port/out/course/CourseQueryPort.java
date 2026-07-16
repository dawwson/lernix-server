package com.lxp.aplus.cart.application.port.out.course;

import com.lxp.aplus.cart.application.port.out.course.model.CourseSalesStatus;
import com.lxp.aplus.cart.application.port.out.course.model.CourseSnapshot;

import java.util.List;
import java.util.Map;

public interface CourseQueryPort {

    // TODO: model 네이밍 구체적으로 바꿀 필요성
    /*
     * course별 구매 정보(가격, 구매 가능 여부) 조회
     */
    Map<Long, CourseSalesStatus> getCourseSalesStatusByIds(List<Long> courseIds);

    /*
     * course의 구매 관련 상세 정보 조회
     */
    Map<Long, CourseSnapshot> getCourseSnapshotByIds(List<Long> courseIds);
}
