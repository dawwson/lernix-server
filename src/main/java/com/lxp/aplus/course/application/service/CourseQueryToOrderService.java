package com.lxp.aplus.course.application.service;

import com.lxp.aplus.common.error.BusinessException;
import com.lxp.aplus.common.error.code.CourseErrorCode;
import com.lxp.aplus.course.application.port.in.order.CourseQueryToOrderUseCase;
import com.lxp.aplus.course.application.port.in.order.model.CoursePrice;
import com.lxp.aplus.course.domain.Course;
import com.lxp.aplus.course.domain.CourseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseQueryToOrderService implements CourseQueryToOrderUseCase {

    private final CourseRepository courseRepository;

    @Override
    public List<CoursePrice> getCoursePrices(List<Long> courseIds) {
        List<Course> courses = courseRepository.findByIdIn(courseIds);

        if (courses.size() != courseIds.size()) {
            throw new BusinessException(CourseErrorCode.COURSE_NOT_FOUND);
        }

        return courses.stream()
                .map(CoursePrice::from)
                .toList();
    }
}
