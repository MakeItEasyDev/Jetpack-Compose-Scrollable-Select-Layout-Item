package com.jetpack.scrollableselectlayout

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp

private lateinit var density: Density

@Composable
fun InitDensity() {
    density = LocalDensity.current
}

fun Dp.toPx(): Float {
    with(density) {
        return this@toPx.toPx()
    }
}