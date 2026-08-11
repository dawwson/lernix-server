package com.lxp.aplus.enrollment.application.service;

import com.lxp.aplus.common.error.BusinessException;
import com.lxp.aplus.common.error.code.EnrollmentErrorCode;
import com.lxp.aplus.enrollment.application.command.EnrollmentCommand;
import com.lxp.aplus.enrollment.application.port.out.CourseQueryPort;
import com.lxp.aplus.enrollment.application.port.out.CourseSummary;
import com.lxp.aplus.enrollment.domain.Enrollment;
import com.lxp.aplus.enrollment.application.port.out.EnrollmentRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EnrollmentCommandServiceTest {

    @InjectMocks
    private EnrollmentCommandService enrollmentCommandService;

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private CourseQueryPort courseQueryPort;

    @Captor
    private ArgumentCaptor<Enrollment> enrollmentCaptor;

    private static final Long STUDENT_ID = 1L;
    private static final Long COURSE_ID_1 = 100L;
    private static final Long ORDER_ITEM_ID_1 = 1000L;

    @Test
    @DisplayName("수강 신청이 정상적으로 완료되어야 한다.")
    void enroll_success() {
        // given
        EnrollmentCommand command = new EnrollmentCommand(STUDENT_ID, COURSE_ID_1, ORDER_ITEM_ID_1);
        Enrollment createdEnrollment = Enrollment.create(STUDENT_ID, COURSE_ID_1, ORDER_ITEM_ID_1);

        given(courseQueryPort.findCourseById(COURSE_ID_1)).willReturn(Optional.of(new CourseSummary(COURSE_ID_1, "Test Course")));
        given(enrollmentRepository.findByOrderItemId(ORDER_ITEM_ID_1)).willReturn(Optional.empty());
        given(enrollmentRepository.existsByStudentIdAndCourseId(STUDENT_ID, COURSE_ID_1)).willReturn(false);
        given(enrollmentRepository.save(any(Enrollment.class))).willReturn(createdEnrollment);

        // when
        Long resultId = enrollmentCommandService.enroll(command);

        // then
        verify(enrollmentRepository).save(enrollmentCaptor.capture());
        Enrollment capturedEnrollment = enrollmentCaptor.getValue();
        assertThat(capturedEnrollment.getStudentId()).isEqualTo(STUDENT_ID);
        assertThat(capturedEnrollment.getCourseId()).isEqualTo(COURSE_ID_1);

        assertThat(resultId).isEqualTo(createdEnrollment.getId());
    }

    @Test
    @DisplayName("같은 주문 항목과 학생, 강좌로 재처리하면 기존 수강 ID를 반환한다")
    void enroll_sameOrderItem_returnsExistingEnrollment() {
        EnrollmentCommand command = new EnrollmentCommand(STUDENT_ID, COURSE_ID_1, ORDER_ITEM_ID_1);
        Enrollment existingEnrollment = Enrollment.create(STUDENT_ID, COURSE_ID_1, ORDER_ITEM_ID_1);
        ReflectionTestUtils.setField(existingEnrollment, "id", 10L);
        given(enrollmentRepository.findByOrderItemId(ORDER_ITEM_ID_1))
                .willReturn(Optional.of(existingEnrollment));

        Long resultId = enrollmentCommandService.enroll(command);

        assertThat(resultId).isEqualTo(10L);
        verify(enrollmentRepository, never()).save(any(Enrollment.class));
    }

    @Test
    @DisplayName("같은 주문 항목에 다른 학생이나 강좌가 들어오면 정합성 오류가 발생한다")
    void enroll_mismatchedOrderItem_throwsConflict() {
        EnrollmentCommand command = new EnrollmentCommand(STUDENT_ID, COURSE_ID_1, ORDER_ITEM_ID_1);
        Enrollment existingEnrollment = Enrollment.create(2L, COURSE_ID_1, ORDER_ITEM_ID_1);
        given(enrollmentRepository.findByOrderItemId(ORDER_ITEM_ID_1))
                .willReturn(Optional.of(existingEnrollment));

        assertThatThrownBy(() -> enrollmentCommandService.enroll(command))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(EnrollmentErrorCode.ENROLLMENT_ORDER_ITEM_CONFLICT);
    }

    @Test
    @DisplayName("존재하지 않는 강의를 수강 신청하면 에러가 발생해야 한다.")
    void enroll_fail_course_not_found() {
        // given
        EnrollmentCommand command = new EnrollmentCommand(STUDENT_ID, COURSE_ID_1, ORDER_ITEM_ID_1);

        given(courseQueryPort.findCourseById(COURSE_ID_1)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> enrollmentCommandService.enroll(command))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(EnrollmentErrorCode.ENROLLMENT_COURSE_NOT_FOUND);
    }

    @Test
    @DisplayName("이미 수강 중인 강의라면 에러가 발생해야 한다.")
    void enroll_fail_duplicate() {
        // given
        EnrollmentCommand command = new EnrollmentCommand(STUDENT_ID, COURSE_ID_1, ORDER_ITEM_ID_1);

        given(courseQueryPort.findCourseById(COURSE_ID_1)).willReturn(Optional.of(new CourseSummary(COURSE_ID_1, "Test Course")));
        given(enrollmentRepository.existsByStudentIdAndCourseId(STUDENT_ID, COURSE_ID_1)).willReturn(true);

        // when & then
        assertThatThrownBy(() -> enrollmentCommandService.enroll(command))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(EnrollmentErrorCode.ENROLLMENT_ALREADY_ENROLLED);
    }
}
