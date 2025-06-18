package ru.yandexpraktikum.cardsanimation.compose

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import ru.yandexpraktikum.cardsanimation.model.CardData
import kotlin.math.abs

@Composable
fun AnimatedCardStack(cards: List<CardData>) {
    val cardCount = cards.size
    var isRotated by remember { mutableStateOf(false) }
    var currentCards by remember { mutableStateOf(cards) }
    var verticalDragOffset by remember { mutableFloatStateOf(0f) }
    var horizontalDragOffset by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = Modifier
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = {
                        // Обрабатываем жест после того, как пользователь убрал палец
                        handleDragEnd(
                            verticalDragDistance = verticalDragOffset,
                            horizontalDragDistance = horizontalDragOffset,
                            onFanStateChange = { newFanState -> isRotated = newFanState },
                            onCardsReorder = {
                                // Берём нижнюю карту (первый элемент) и перемещаем в конец списка
                                val reorderedCards = currentCards.drop(1) + currentCards.first()
                                currentCards = reorderedCards
                            }
                        )
                        verticalDragOffset = 0f
                        horizontalDragOffset = 0f
                    }
                ) { _, dragAmount ->
                    val horizontalMovement = dragAmount.x
                    val verticalMovement = dragAmount.y

                    val isHorizontalSwipe =
                        abs(horizontalMovement) > abs(verticalMovement)
                    val isVerticalSwipe =
                        abs(verticalMovement) > abs(horizontalMovement)

                    if (isVerticalSwipe) {
                        // При вертикальном свайпе раскрываем/складываем карты
                        verticalDragOffset += verticalMovement
                    }

                    if (isHorizontalSwipe) {
                        // Накапливаем горизонтальное движение для обработки в onDragEnd
                        horizontalDragOffset += horizontalMovement
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        // Отрисовка колоды карт в исходной позиции
        for (i in 0 until cardCount) {
            val cardData = currentCards[i]

            // Расчёт расположения карт в исходной позиции
            val baseRotation = if (cardCount > 1) {
                val angleStep = 45f / (cardCount - 1)
                22.5f - (i * angleStep)
            } else {
                0f
            }

            // Расчёт расположения карт в зависимости от состояния
            val targetRotation = if (isRotated) {
                // В развёрнутом состоянии карты занимают половину окружности
                val angleStep = if (cardCount > 1) 180f / (cardCount - 1) else 0f
                90f - (i * angleStep)
            } else {
                // В свёрнутом карты возвращаются в исходное положение
                baseRotation
            }

            AnimatedCard(
                cardIndex = i,
                targetRotation = targetRotation,
                cardData = cardData
            )
        }
    }
}

/**
 * Обработка окончания свайпа (когда пользователь убирает палец с экрана)
 * Проверяет как вертикальное, так и горизонтальное движение
 */
fun handleDragEnd(
    verticalDragDistance: Float,
    horizontalDragDistance: Float,
    onFanStateChange: (Boolean) -> Unit,
    onCardsReorder: () -> Unit
) {
    val verticalThreshold = 100f
    val horizontalThreshold = 100f

    // Обрабатываем вертикальное движение
    when {
        verticalDragDistance < -verticalThreshold -> onFanStateChange(true)
        verticalDragDistance > verticalThreshold -> onFanStateChange(false)
    }

    // Обработка горизонтального движения
    if (abs(horizontalDragDistance) > horizontalThreshold) {
        onCardsReorder()
    }
}