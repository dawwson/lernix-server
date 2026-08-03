package com.lxp.aplus.course.application.service;

import com.lxp.aplus.common.error.BusinessException;
import com.lxp.aplus.common.error.code.CourseErrorCode;
import com.lxp.aplus.common.error.code.UserErrorCode;
import com.lxp.aplus.course.application.port.in.cart.CourseQueryToCartUseCase;
import com.lxp.aplus.course.application.port.in.cart.model.CoursePurchaseInfo;
import com.lxp.aplus.course.application.port.in.cart.model.CourseSalesInfo;
import com.lxp.aplus.course.application.port.out.UserQueryPort;
import com.lxp.aplus.course.application.result.InstructorResult;
import com.lxp.aplus.course.domain.Course;
import com.lxp.aplus.course.domain.CourseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseQueryToCartService implements CourseQueryToCartUseCase {

    private final CourseRepository courseRepository;
    private final UserQueryPort userQueryPort;

    @Override
    public List<CourseSalesInfo> getCourseSalesInfoByIds(List<Long> courseIds) {
        if (courseIds.isEmpty()) {
            return List.of();
        }

        return courseRepository.findByIdIn(courseIds).stream()
                .map(CourseSalesInfo::from)
                .toList();
    }

    @Override
    public Map<Long, CoursePurchaseInfo> getCoursePurchaseInfoByIds(List<Long> courseIds) {
        if (courseIds.isEmpty()) {
            return Map.of();
        }

        List<Course> courses = courseRepository.findByIdIn(courseIds);

        if (courses.size() != courseIds.size()) {
            throw new BusinessException(CourseErrorCode.COURSE_NOT_FOUND);
        }

        List<Long> instructorIds = courses.stream()
                .map(Course::getInstructorId)
                .toList();
        Map<Long, InstructorResult> instructors = userQueryPort.findInstructorsByIds(instructorIds);

        return courses.stream()
                .collect(Collectors.toMap(
                        Course::getId,
                        course -> {
                            InstructorResult instructor = instructors.get(course.getInstructorId());

                            // TODO: 강사 탈퇴 시 해당 강사의 PUBLISHED 강좌를 판매 중단 상태로 전환하고,
                            //       Cart/Order에서 구매할 수 없도록 처리하는 정책을 적용한다.
                            if (instructor == null) {
                                throw new BusinessException(UserErrorCode.USER_NOT_FOUND);
                            }

                            return CoursePurchaseInfo.from(course, instructor.nickName());
                        }
                ));
    }
}
