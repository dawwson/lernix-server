package com.lxp.aplus.enrollment.adapter.out.persistence;

import com.lxp.aplus.enrollment.domain.Enrollment;
import com.lxp.aplus.enrollment.infrastructure.persistence.EnrollmentJpaRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles("test")
@ContextConfiguration(classes = EnrollmentJpaRepositoryTest.Configuration.class)
@DisplayName("EnrollmentJpaRepository 테스트")
class EnrollmentJpaRepositoryTest {

    @Autowired
    private EnrollmentJpaRepository repository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("주문 항목 ID로 수강 정보를 조회한다")
    void findByOrderItemId_existingEnrollment_returnsEnrollment() {
        Enrollment enrollment = repository.saveAndFlush(Enrollment.create(1L, 10L, 100L));
        entityManager.clear();

        Enrollment result = repository.findByOrderItemId(100L).orElseThrow();

        assertThat(result.getId()).isEqualTo(enrollment.getId());
    }

    @Test
    @DisplayName("같은 주문 항목 ID로 수강 정보를 중복 저장할 수 없다")
    void save_duplicateOrderItemId_throwsDataIntegrityViolation() {
        repository.saveAndFlush(Enrollment.create(1L, 10L, 100L));
        entityManager.clear();

        assertThatThrownBy(() -> repository.saveAndFlush(Enrollment.create(2L, 20L, 100L)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @TestConfiguration(proxyBeanMethods = false)
    @EntityScan(basePackageClasses = Enrollment.class)
    @EnableJpaRepositories(basePackageClasses = EnrollmentJpaRepository.class)
    static class Configuration {
    }
}
