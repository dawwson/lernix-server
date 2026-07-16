package com.lxp.aplus.cart.application.port.out.course;

import com.lxp.aplus.cart.application.port.out.course.model.CourseSalesStatus;
import com.lxp.aplus.cart.application.port.out.course.model.CourseSnapshot;

import java.util.List;
import java.util.Map;

public interface CourseQueryPort {

    // TODO: model 네이밍 구체적으로 바꿀 필요성
    /*
     * 판매 정보 조회
     */
    Map<Long, CourseSalesStatus> getCourseSalesStatusByIds(List<Long> courseIds);

    /*
     * 구매 상세 정보 조회
     */
    Map<Long, CourseSnapshot> getCourseSnapshotByIds(List<Long> courseIds);
}
