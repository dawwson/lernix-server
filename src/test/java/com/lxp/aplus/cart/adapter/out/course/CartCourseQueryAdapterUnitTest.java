package com.lxp.aplus.cart.adapter.out.course;

import com.lxp.aplus.cart.application.port.out.course.model.CourseSalesStatus;
import com.lxp.aplus.cart.application.port.out.course.model.CourseSnapshot;
import com.lxp.aplus.course.application.port.in.cart.CourseQueryToCartUseCase;
import com.lxp.aplus.course.application.port.in.cart.model.CoursePurchaseInfo;
import com.lxp.aplus.course.application.port.in.cart.model.CourseSalesInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("CartCourseQueryAdapter 단위 테스트")
class CartCourseQueryAdapterUnitTest {

    @Mock
    private CourseQueryToCartUseCase courseQueryToCartUseCase;

    @InjectMocks
    private CartCourseQueryAdapter adapter;

    @Test
    @DisplayName("Course 판매 정보 List를 Cart의 ID 기준 Map으로 변환한다")
    void getCourseSalesStatusByIds_Success() {
        List<Long> courseIds = List.of(10L, 20L);
        given(courseQueryToCartUseCase.getCourseSalesInfoByIds(courseIds)).willReturn(List.of(
                new CourseSalesInfo(10L, 10000, true),
                new CourseSalesInfo(20L, 30000, false)
        ));

        Map<Long, CourseSalesStatus> result = adapter.getCourseSalesStatusByIds(courseIds);

        then(courseQueryToCartUseCase).should().getCourseSalesInfoByIds(courseIds);
        assertThat(result).containsEntry(10L, new CourseSalesStatus(10000, true));
        assertThat(result).containsEntry(20L, new CourseSalesStatus(30000, false));
    }

    @Test
    @DisplayName("Course 구매 상세 정보를 Cart의 강좌 스냅샷으로 변환한다")
    void getCourseSnapshotByIds_Success() {
        List<Long> courseIds = List.of(10L);
        given(courseQueryToCartUseCase.getCoursePurchaseInfoByIds(courseIds)).willReturn(Map.of(
                10L,
                new CoursePurchaseInfo(
                        10L,
                        "Java Basic",
                        "PUBLISHED",
                        "강사1",
                        "thumbnail-10",
                        10000,
                        true
                )
        ));

        Map<Long, CourseSnapshot> result = adapter.getCourseSnapshotByIds(courseIds);

        then(courseQueryToCartUseCase).should().getCoursePurchaseInfoByIds(courseIds);
        assertThat(result).containsEntry(
                10L,
                new CourseSnapshot(
                        10L,
                        "Java Basic",
                        "PUBLISHED",
                        "강사1",
                        "thumbnail-10",
                        10000,
                        true
                )
        );
    }
}
