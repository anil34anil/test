package com.desert.finansim.ui.screens.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.desert.finansim.data.local.CategoryEntity
import com.desert.finansim.di.AppContainer
import com.desert.finansim.domain.model.CategoryKind
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CategoriesUiState(
    val incomeCategories: List<CategoryEntity> = emptyList(),
    val expenseCategories: List<CategoryEntity> = emptyList(),
)

class CategoriesViewModel(private val container: AppContainer) : ViewModel() {

    val uiState: StateFlow<CategoriesUiState> = container.categoryRepository.allCategories
        .map { categories ->
            CategoriesUiState(
                incomeCategories = categories.filter { it.kind == CategoryKind.INCOME },
                expenseCategories = categories.filter { it.kind == CategoryKind.EXPENSE },
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = CategoriesUiState(),
        )

    fun add(name: String, kind: CategoryKind, iconKey: String, colorArgb: Long) {
        viewModelScope.launch {
            container.categoryRepository.add(
                CategoryEntity(
                    name = name,
                    kind = kind,
                    iconKey = iconKey,
                    colorArgb = colorArgb,
                    sortOrder = 999,
                )
            )
        }
    }

    fun update(category: CategoryEntity) {
        viewModelScope.launch { container.categoryRepository.update(category) }
    }

    fun toggleArchived(category: CategoryEntity) {
        viewModelScope.launch {
            if (category.isArchived) {
                container.categoryRepository.restore(category)
            } else {
                container.categoryRepository.archive(category)
            }
        }
    }
}
