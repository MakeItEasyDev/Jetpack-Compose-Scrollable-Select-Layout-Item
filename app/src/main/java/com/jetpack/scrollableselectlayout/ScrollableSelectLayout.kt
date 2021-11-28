package com.jetpack.scrollableselectlayout

import androidx.annotation.FloatRange
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.*
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.FractionalThreshold
import androidx.compose.material.rememberSwipeableState
import androidx.compose.material.swipeable
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.*
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset

open class SelectionLineStyle(
    val color: Color,
    @FloatRange(from = 0.0, to = 1.0)
    val lengthFraction: Float,
    val strokeWidth: Float
) {
    object Default: SelectionLineStyle(
        color = Color(0xff83cde6),
        lengthFraction = 1f,
        strokeWidth = 3f
    )
}

private class ScrollableSelectColumnItem<T>(var item: T, selected: Boolean = false) {
    var selected by mutableStateOf(selected)
}

@Composable
private fun ScrollableSelectColumn(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Layout(
        content = content,
        modifier = modifier,
        measurePolicy = object : MeasurePolicy {
            override fun MeasureScope.measure(
                measurables: List<Measurable>,
                constraints: Constraints
            ): MeasureResult {
                val placeable = measurables.map {
                    it.measure(constraints)
                }
                var needHeight = 0
                placeable.forEach { placeable ->
                    needHeight += placeable.height
                }
                var currentY = 0
                return layout(
                    constraints.maxWidth, needHeight
                ) {
                    placeable.forEach { placeable ->
                        placeable.placeRelative(x = 0, y = currentY)
                        currentY += placeable.height
                    }
                }
            }
        }
    )
}

@Composable
private fun <E> ScrollableSelectColumnItemLayout(
    itemHeight: Dp,
    scrollableSelectItem: ScrollableSelectColumnItem<E>,
    content: @Composable RowScope.(E, Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(itemHeight),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        content(scrollableSelectItem.item, scrollableSelectItem.selected)
    }
}

class ScrollableSelectState(currentSwipeItemIndex: Int) {
    var currentSwipeItemIndex by mutableStateOf(currentSwipeItemIndex)

    companion object {
        val Saver = object : Saver<ScrollableSelectState, Int> {
            override fun restore(value: Int): ScrollableSelectState {
                return ScrollableSelectState(value)
            }

            override fun SaverScope.save(value: ScrollableSelectState): Int {
                return value.currentSwipeItemIndex
            }
        }
    }
}

@ExperimentalMaterialApi
@Composable
fun <E> ScrollableSelectLayout(
    items: List<E>,
    scrollableSelectState: ScrollableSelectState = rememberScrollableSelectState(),
    itemHeight: Dp,
    modifier: Modifier = Modifier,
    visibleAmount: Int = 3,
    selectionLineStyle: SelectionLineStyle = SelectionLineStyle.Default,
    content: @Composable RowScope.(item: E, Boolean) -> Unit
) {
    val scrollableSelectColumnItems = remember(items) {
        items.map {
            ScrollableSelectColumnItem(it)
        }
    }

    var midItemIndexStart = remember(scrollableSelectColumnItems) {
        val midItemIndexStart = if (scrollableSelectState.currentSwipeItemIndex != -1) {
            scrollableSelectState.currentSwipeItemIndex - 1
        } else {
            (((scrollableSelectColumnItems.size - 1) / 2) - 1).coerceAtLeast(0).coerceAtMost(scrollableSelectColumnItems.size - 2)
        }
        scrollableSelectColumnItems[midItemIndexStart + 1].selected = true
        scrollableSelectState.currentSwipeItemIndex = midItemIndexStart + 1
        midItemIndexStart
    }

    val anchors = remember(scrollableSelectColumnItems) {
        val anchors = mutableMapOf<Float, Int>()
        for (index in scrollableSelectColumnItems.indices) {
            anchors[-index * itemHeight.toPx()] = index - 1
        }
        anchors
    }

    val swipeableState = rememberSwipeableState(initialValue = midItemIndexStart) {
        scrollableSelectColumnItems[midItemIndexStart + 1].selected = false
        scrollableSelectColumnItems[it + 1].selected = true
        midItemIndexStart = it
        scrollableSelectState.currentSwipeItemIndex = midItemIndexStart + 1
        true
    }

    val selectBoxOffset: Float = if (visibleAmount.mod(2) == 0) itemHeight.toPx() / 2f else 0f

    Box(
        modifier = Modifier
            .then(modifier)
            .fillMaxWidth()
            .height(itemHeight * visibleAmount)
            .swipeable(
                state = swipeableState,
                anchors = anchors,
                orientation = Orientation.Vertical,
                thresholds = { _, _ ->
                    FractionalThreshold(0.5f)
                }
            )
            .drawWithContent {
                val width = drawContext.size.width
                val startFraction = (1 - selectionLineStyle.lengthFraction) / 2f
                val endFraction = startFraction + selectionLineStyle.lengthFraction
                drawContent()
                drawLine(
                    color = selectionLineStyle.color,
                    start = Offset(
                        width * startFraction,
                        itemHeight.toPx() * ((visibleAmount - 1) / 2)
                    ),
                    end = Offset(
                        width * endFraction,
                        itemHeight.toPx() * ((visibleAmount - 1) / 2)
                    ),
                    strokeWidth = 3f
                )
                drawLine(
                    color = selectionLineStyle.color,
                    start = Offset(
                        width * startFraction,
                        itemHeight.toPx() * ((visibleAmount - 1) / 2 + 1)
                    ),
                    end = Offset(
                        width * endFraction,
                        itemHeight.toPx() * ((visibleAmount - 1) / 2 + 1)
                    ),
                    strokeWidth = selectionLineStyle.strokeWidth
                )
            }
            .graphicsLayer { clip = true }
    ) {
        ScrollableSelectColumn(
            modifier = Modifier
                .fillMaxWidth()
                .layout { measurable, constraints ->
                    val nonConstraints = Constraints(
                        minWidth = constraints.minWidth,
                        maxWidth = constraints.maxWidth
                    )
                    val placeable = measurable.measure(nonConstraints)
                    val currentY = placeable.height / 2 - (itemHeight.toPx() * 1.5).toInt()
                    layout(placeable.width, placeable.height) {
                        placeable.placeRelative(0, currentY - selectBoxOffset.toInt())
                    }
                }
                .offset {
                    IntOffset(0, swipeableState.offset.value.toInt())
                }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(itemHeight)
            )
            for (scrollableSelectItem in scrollableSelectColumnItems) {
                ScrollableSelectColumnItemLayout(
                    itemHeight = itemHeight,
                    scrollableSelectItem = scrollableSelectItem,
                    content = content
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(itemHeight)
            )
        }
    }
}

@Composable
fun rememberScrollableSelectState(
    initialItemIndex: Int = -1
) = rememberSaveable(saver = ScrollableSelectState.Saver) {
    ScrollableSelectState(initialItemIndex)
}






















