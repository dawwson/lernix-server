package com.lxp.aplus.course.application.internal.usecase;

import com.lxp.aplus.common.error.BusinessException;
import com.lxp.aplus.common.error.code.CourseErrorCode;
import com.lxp.aplus.course.application.internal.dto.CoursePrice;
import com.lxp.aplus.course.domain.Course;
import com.lxp.aplus.course.domain.CourseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// TODO: Port 필요
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseQueryToOrderUseCase {

    private final CourseRepository courseRepository;

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
