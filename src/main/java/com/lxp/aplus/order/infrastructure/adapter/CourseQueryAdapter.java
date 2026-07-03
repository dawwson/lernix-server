package com.lxp.aplus.order.infrastructure.adapter;

import com.lxp.aplus.common.error.BusinessException;
import com.lxp.aplus.common.error.code.CourseErrorCode;
import com.lxp.aplus.course.domain.Course;
import com.lxp.aplus.course.domain.CourseRepository;
import com.lxp.aplus.order.application.port.out.CoursePrice;
import com.lxp.aplus.order.application.port.out.CourseQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseQueryAdapter implements CourseQueryPort {

    private final CourseRepository courseRepository;

    @Override
    public List<CoursePrice> getCoursePriceByIds(List<Long> courseIds) {
        List<Course> courses = courseRepository.findByIdIn(courseIds);

        if (courses.size() != courseIds.size()) {
            throw new BusinessException(CourseErrorCode.COURSE_NOT_FOUND);
        }

        return courses.stream()
                .map(course -> new CoursePrice(course.getId(), course.getPrice()))
                .toList();
    }
}
