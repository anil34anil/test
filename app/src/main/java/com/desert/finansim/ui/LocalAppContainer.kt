package com.desert.finansim.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.desert.finansim.di.AppContainer

val LocalAppContainer = staticCompositionLocalOf<AppContainer> {
    error("AppContainer sağlanmadı. FinansimApp içinde CompositionLocalProvider kullanın.")
}

/**
 * Ekran ViewModel'lerini bagimlilik kabindan uretir.
 *
 * Ekrana ozgu parametreler (borc kimligi gibi) dogrudan yapiciya gecer;
 * boylece SavedStateHandle ile ugrasmadan tip guvenli kalinir. [key]
 * verildiginde ayni ekranin farkli kayitlari icin ayri ViewModel olusur.
 */
@Composable
inline fun <reified VM : ViewModel> containerViewModel(
    key: String? = null,
    crossinline create: (AppContainer) -> VM,
): VM {
    val container = LocalAppContainer.current
    return viewModel(
        key = key,
        factory = viewModelFactory {
            initializer { create(container) }
        },
    )
}
