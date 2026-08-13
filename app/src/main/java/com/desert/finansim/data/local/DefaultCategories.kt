package com.desert.finansim.data.local

import com.desert.finansim.domain.model.CategoryKind

/**
 * Ilk acilista yuklenen varsayilan kategoriler. Kullanici bunlari
 * duzenleyebilir, arsivleyebilir veya yenilerini ekleyebilir.
 *
 * [iconKey] degerleri UI tarafinda ikona eslenir (CategoryIcons.kt).
 */
object DefaultCategories {

    val INCOME = listOf(
        Triple("Maaş", "salary", 0xFF2E7D5B),
        Triple("Prim", "bonus", 0xFF3E8E7E),
        Triple("Ek Gelir", "extra", 0xFF4CA98F),
        Triple("Kira Geliri", "rent_income", 0xFF5FA8D3),
        Triple("Freelance", "freelance", 0xFF7B6CF6),
        Triple("Diğer", "other", 0xFF7E8A97),
    )

    val EXPENSE = listOf(
        Triple("Market", "grocery", 0xFFE2703A),
        Triple("Fatura", "bill", 0xFF5B8FF9),
        Triple("Kira", "home", 0xFF9B5DE5),
        Triple("Ulaşım", "transport", 0xFF00A9A5),
        Triple("Yemek", "food", 0xFFF15BB5),
        Triple("Alışveriş", "shopping", 0xFFEE6C4D),
        Triple("Sağlık", "health", 0xFF2EC4B6),
        Triple("Eğlence", "fun", 0xFFFF9F1C),
        Triple("Abonelik", "subscription", 0xFF6A7FDB),
        Triple("Eğitim", "education", 0xFF3D5A80),
        Triple("Diğer", "other", 0xFF7E8A97),
    )

    fun all(): List<CategoryEntity> {
        val income = INCOME.mapIndexed { index, (name, icon, color) ->
            CategoryEntity(
                name = name,
                kind = CategoryKind.INCOME,
                iconKey = icon,
                colorArgb = color,
                sortOrder = index,
            )
        }
        val expense = EXPENSE.mapIndexed { index, (name, icon, color) ->
            CategoryEntity(
                name = name,
                kind = CategoryKind.EXPENSE,
                iconKey = icon,
                colorArgb = color,
                sortOrder = index,
            )
        }
        return income + expense
    }
}
