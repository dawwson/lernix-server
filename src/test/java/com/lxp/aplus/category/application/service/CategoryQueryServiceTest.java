package com.lxp.aplus.category.application.service;

import com.lxp.aplus.category.application.dto.result.CategoryResult;
import com.lxp.aplus.category.application.internal.dto.CategoryInternalResult;
import com.lxp.aplus.category.application.port.out.CategoryRepository;
import com.lxp.aplus.category.domain.Category;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
@DisplayName("CategoryQueryService 단위 테스트")
class CategoryQueryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryQueryService service;

    @Test
    @DisplayName("루트 카테고리와 하위 카테고리를 조회 결과로 변환한다")
    void getAllCategories_RootWithChild_ReturnsHierarchy() {
        Category child = mock(Category.class);
        given(child.getId()).willReturn(2L);
        given(child.getName()).willReturn("Java");
        given(child.getChildren()).willReturn(List.of());
        Category root = mock(Category.class);
        given(root.getId()).willReturn(1L);
        given(root.getName()).willReturn("개발");
        given(root.getChildren()).willReturn(List.of(child));
        given(categoryRepository.findAllRootWithChildren()).willReturn(List.of(root));

        List<CategoryResult> result = service.getAllCategories();

        then(categoryRepository).should().findAllRootWithChildren();
        assertThat(result).containsExactly(
                new CategoryResult(
                        1L,
                        "개발",
                        List.of(new CategoryResult(2L, "Java", List.of()))
                )
        );
    }

    @Test
    @DisplayName("카테고리를 ID로 조회하고 부모와 자식 정보를 내부 결과로 변환한다")
    void findByIdWithParent_ExistingCategory_ReturnsInternalResult() {
        Category parent = mock(Category.class);
        given(parent.getName()).willReturn("개발");
        Category child = mock(Category.class);
        given(child.getId()).willReturn(3L);
        Category category = mock(Category.class);
        given(category.getId()).willReturn(2L);
        given(category.getName()).willReturn("Java");
        given(category.getParent()).willReturn(parent);
        given(category.getChildren()).willReturn(List.of(child));
        given(categoryRepository.findByIdWithParent(2L)).willReturn(Optional.of(category));

        Optional<CategoryInternalResult> result = service.findByIdWithParent(2L);

        then(categoryRepository).should().findByIdWithParent(2L);
        assertThat(result).contains(
                new CategoryInternalResult(2L, "Java", "개발", List.of(3L))
        );
    }

    @Test
    @DisplayName("존재하지 않는 카테고리를 ID로 조회하면 빈 결과를 반환한다")
    void findByIdWithParent_MissingCategory_ReturnsEmpty() {
        given(categoryRepository.findByIdWithParent(999L)).willReturn(Optional.empty());

        Optional<CategoryInternalResult> result = service.findByIdWithParent(999L);

        then(categoryRepository).should().findByIdWithParent(999L);
        assertThat(result).isEmpty();
    }

}
