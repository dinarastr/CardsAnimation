package ru.yandexpraktikum.cardsanimation.compose

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import ru.yandexpraktikum.cardsanimation.model.CardData

@Composable
fun CardStack(cards: List<CardData>) {
    val cardCount = cards.size
    var isRotated by remember { mutableStateOf(false) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    var cardOffset by remember { mutableIntStateOf(0) }

    Box(
        modifier = Modifier
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = {
                        // Обрабатываем жест после того, как пользователь убрал палец
                        handleDragEnd(
                            verticalDragDistance = dragOffset,
                            onFanStateChange = { newFanState -> isRotated = newFanState }
                        )
                        dragOffset = 0f
                    }
                ) { _, dragAmount ->
                    val horizontalMovement = dragAmount.x
                    val verticalMovement = dragAmount.y

                    val isHorizontalSwipe = kotlin.math.abs(horizontalMovement) > kotlin.math.abs(verticalMovement)
                    val isVerticalSwipe = kotlin.math.abs(verticalMovement) > kotlin.math.abs(horizontalMovement)

                    if (isVerticalSwipe) {
                        // При вертикальном свайпе раскрываем/складываем карты
                        dragOffset += verticalMovement
                    }

                    // При горизонтальном свайпе перетасовываем карты
                    if (isHorizontalSwipe) {
                        handleHorizontalSwipe(
                            horizontalMovement = horizontalMovement,
                            currentCardOffset = cardOffset,
                            cardCount = cardCount,
                            onCardCycle = { newCardOffset -> cardOffset = newCardOffset }
                        )
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        // Отрисовка колоды карт в исходной позиции
        for (i in 0 until cardCount) {
            val actualCardIndex = (i + cardOffset) % cardCount
            val cardData = cards[actualCardIndex]

            val baseRotation = if (cardCount > 1) {
                val angleStep = 45f / (cardCount - 1)
                22.5f - (i * angleStep)
            } else {
                0f
            }

            AnimatedStackCard(
                cardIndex = i,
                totalCards = cardCount,
                isRotated = isRotated,
                baseRotation = baseRotation,
                cardData = cardData
            )
        }
    }
}

/**
 * Обработка окончания свайпа вниз или вверх
 * (когда пользователь убирает палец с экрана)
 */
fun handleDragEnd(
    verticalDragDistance: Float,
    onFanStateChange: (Boolean) -> Unit
) {
    val threshold = 100f

    when {
        verticalDragDistance < -threshold -> onFanStateChange(true)
        verticalDragDistance > threshold -> onFanStateChange(false)
    }
}

/**
 * Обработка горизонтального свайпа (перетасовка карт)
 */
fun handleHorizontalSwipe(
    horizontalMovement: Float,
    currentCardOffset: Int,
    cardCount: Int,
    onCardCycle: (Int) -> Unit
) {
    val swipeThreshold = 50f

    if (kotlin.math.abs(horizontalMovement) > swipeThreshold) {
        val newOffset = if (currentCardOffset - 1 < 0) cardCount - 1 else currentCardOffset - 1
        onCardCycle(newOffset)
    }
}