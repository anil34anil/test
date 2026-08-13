package com.desert.finansim.ui.screens.categories

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.item
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.desert.finansim.data.local.CategoryEntity
import com.desert.finansim.domain.model.CategoryKind
import com.desert.finansim.ui.components.CategoryIcons
import com.desert.finansim.ui.components.CircleIcon
import com.desert.finansim.ui.components.SectionCard
import com.desert.finansim.ui.containerViewModel
import com.desert.finansim.ui.theme.ChartPaletteArgb

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesScreen(onBack: () -> Unit) {
    val viewModel = containerViewModel { CategoriesViewModel(it) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<CategoryEntity?>(null) }
    var creatingKind by remember { mutableStateOf<CategoryKind?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Kategoriler") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { creatingKind = CategoryKind.EXPENSE }) {
                Icon(Icons.Default.Add, contentDescription = "Kategori ekle")
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Text("Gider kategorileri", style = MaterialTheme.typography.titleMedium)
            }
            items(state.expenseCategories, key = { "cat-${it.id}" }) { category ->
                CategoryRow(
                    category = category,
                    onClick = { editing = category },
                    onToggleArchive = { viewModel.toggleArchived(category) },
                )
            }

            item {
                Spacer(Modifier.height(8.dp))
                Text("Gelir kategorileri", style = MaterialTheme.typography.titleMedium)
            }
            items(state.incomeCategories, key = { "cat-inc-${it.id}" }) { category ->
                CategoryRow(
                    category = category,
                    onClick = { editing = category },
                    onToggleArchive = { viewModel.toggleArchived(category) },
                )
            }

            item {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Kategoriler silinmez, arşivlenir: geçmiş işlemlerin kategorisi " +
                        "kaybolmasın diye. Arşivlenen kategori yeni işlemlerde görünmez.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    val target = editing
    if (target != null) {
        CategoryEditDialog(
            initial = target,
            onDismiss = { editing = null },
            onSave = { name, iconKey, color ->
                viewModel.update(target.copy(name = name, iconKey = iconKey, colorArgb = color))
                editing = null
            },
        )
    }

    val newKind = creatingKind
    if (newKind != null) {
        CategoryEditDialog(
            initial = CategoryEntity(
                name = "",
                kind = newKind,
                iconKey = "other",
                colorArgb = ChartPaletteArgb.first(),
            ),
            allowKindChange = true,
            onDismiss = { creatingKind = null },
            onSave = { name, iconKey, color ->
                viewModel.add(name, newKind, iconKey, color)
                creatingKind = null
            },
            onKindChange = { creatingKind = it },
        )
    }
}

@Composable
private fun CategoryRow(
    category: CategoryEntity,
    onClick: () -> Unit,
    onToggleArchive: () -> Unit,
) {
    SectionCard(modifier = Modifier.clickable(onClick = onClick)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CircleIcon(
                icon = CategoryIcons.forKey(category.iconKey),
                tint = Color(category.colorArgb),
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = category.name,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (category.isArchived) {
                    Text(
                        text = "Arşivlenmiş",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            TextButton(onClick = onToggleArchive) {
                Text(if (category.isArchived) "Geri al" else "Arşivle")
            }
        }
    }
}

@Composable
private fun CategoryEditDialog(
    initial: CategoryEntity,
    onDismiss: () -> Unit,
    onSave: (String, String, Long) -> Unit,
    allowKindChange: Boolean = false,
    onKindChange: (CategoryKind) -> Unit = {},
) {
    var name by remember { mutableStateOf(initial.name) }
    var iconKey by remember { mutableStateOf(initial.iconKey) }
    var color by remember { mutableStateOf(initial.colorArgb) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial.id == 0L) "Kategori ekle" else "Kategoriyi düzenle") },
        text = {
            Column {
                if (allowKindChange) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = initial.kind == CategoryKind.EXPENSE,
                            onClick = { onKindChange(CategoryKind.EXPENSE) },
                            label = { Text("Gider") },
                        )
                        FilterChip(
                            selected = initial.kind == CategoryKind.INCOME,
                            onClick = { onKindChange(CategoryKind.INCOME) },
                            label = { Text("Gelir") },
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; error = null },
                    label = { Text("Kategori adı") },
                    singleLine = true,
                    isError = error != null,
                    supportingText = error?.let {
                        { Text(it, color = MaterialTheme.colorScheme.error) }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(Modifier.height(12.dp))
                Text("İkon", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    CategoryIcons.selectableKeys.forEach { key ->
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .border(
                                    width = if (iconKey == key) 2.dp else 0.dp,
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = CircleShape,
                                )
                                .clickable { iconKey = key },
                            contentAlignment = Alignment.Center,
                        ) {
                            CircleIcon(
                                icon = CategoryIcons.forKey(key),
                                tint = Color(color),
                                size = 36,
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                Text("Renk", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ChartPaletteArgb.forEach { value ->
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(value))
                                .border(
                                    width = if (color == value) 3.dp else 0.dp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    shape = CircleShape,
                                )
                                .clickable { color = value },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isBlank()) {
                        error = "Kategori adı boş olamaz"
                    } else {
                        onSave(name.trim(), iconKey, color)
                    }
                }
            ) { Text("Kaydet") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Vazgeç") } },
    )
}
