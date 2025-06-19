package ru.yandexpraktikum.cardsanimation.compose

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import ru.yandexpraktikum.cardsanimation.model.CardData
import kotlin.math.abs

/**
 * Класс для хранения состояния перетасовки карт
 */
data class CardSwapAnimationState(
    val isAnimating: Boolean = false,
    val animationStep: Int = 0
)

/**
 * Метод для вычисления поворота карты в конкретной позиции
 */
fun calculateCardRotation(
    cardIndex: Int,
    cardCount: Int,
    isRotated: Boolean
): Float {
    if (cardCount <= 1) return 0f
    
    return if (isRotated) {
        val angleStep = 180f / (cardCount - 1)
        90f - (cardIndex * angleStep)
    } else {
        val angleStep = 45f / (cardCount - 1)
        22.5f - (cardIndex * angleStep)
    }
}

/**
 * Расчет финальной позиции после перетасовки карт
 */
fun calculateFinalRotation(
    cardIndex: Int,
    cardCount: Int,
    isRotated: Boolean
): Float {
    val newIndex = if (cardIndex == 0) cardCount - 1 else cardIndex - 1
    return calculateCardRotation(newIndex, cardCount, isRotated)
}

/**
 * Делит анимацию на шаги:
 * 1) поворот нижней карты вправо,
 * 2) перенос карты поверх стопки,
 * 3) анимация стопки карт, занимающих финальное положение
 */
fun handleAnimationStepComplete(
    step: Int,
    cardIndex: Int,
    onStepChange: (Int) -> Unit,
    onAnimationComplete: () -> Unit
) {
    if (cardIndex == 0) {
        when (step) {
            1 -> onStepChange(2)
            2 -> onStepChange(3)
            3 -> onAnimationComplete()
        }
    }
}

/**
 * Начало анимации перетасовки карт
 */
fun startCardSwapAnimation(
    currentState: CardSwapAnimationState,
    onStateChange: (CardSwapAnimationState) -> Unit
) {
    if (!currentState.isAnimating) {
        onStateChange(
            CardSwapAnimationState(
                isAnimating = true,
                animationStep = 1
            )
        )
    }
}

/**
 * Окончание анимации перетасовки карт
 */
fun completeCardSwapAnimation(
    cards: List<CardData>,
    onStateChange: (CardSwapAnimationState) -> Unit,
    onCardsReorder: (List<CardData>) -> Unit
) {
    onStateChange(CardSwapAnimationState())

    val reorderedCards = cards.drop(1) + cards.first()
    onCardsReorder(reorderedCards)
}

@Composable
fun AnimatedCardStack(cards: List<CardData>) {
    val cardCount = cards.size
    var isRotated by remember { mutableStateOf(false) }
    var currentCards by remember { mutableStateOf(cards) }
    var verticalDragOffset by remember { mutableFloatStateOf(0f) }
    var horizontalDragOffset by remember { mutableFloatStateOf(0f) }
    
    var animationState by remember { mutableStateOf(CardSwapAnimationState()) }

    Box(
        modifier = Modifier
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = {
                        handleDragEnd(
                            verticalDragDistance = verticalDragOffset,
                            horizontalDragDistance = horizontalDragOffset,
                            onFanStateChange = { newFanState -> isRotated = newFanState },
                            onCardsReorder = {
                                startCardSwapAnimation(animationState) { newState ->
                                    animationState = newState
                                }
                            }
                        )
                        verticalDragOffset = 0f
                        horizontalDragOffset = 0f
                    }
                ) { _, dragAmount ->
                    if (!animationState.isAnimating) {
                        val horizontalMovement = dragAmount.x
                        val verticalMovement = dragAmount.y

                        val isHorizontalSwipe = abs(horizontalMovement) > abs(verticalMovement)
                        val isVerticalSwipe = abs(verticalMovement) > abs(horizontalMovement)

                        if (isVerticalSwipe) {
                            verticalDragOffset += verticalMovement
                        }
                        if (isHorizontalSwipe) {
                            horizontalDragOffset += horizontalMovement
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        currentCards.forEachIndexed { i, cardData ->
            key(cardData.imageResId) {
                val targetRotation = calculateCardRotation(i, cardCount, isRotated)
                val finalRotation = if (animationState.animationStep == 3) {
                    calculateFinalRotation(i, cardCount, isRotated)
                } else {
                    targetRotation
                }

                AnimatedCard(
                    cardIndex = i,
                    targetRotation = targetRotation,
                    cardData = cardData,
                    isAnimating = animationState.isAnimating && i == 0,
                    animationStep = if (animationState.animationStep == 3) {
                        3
                    } else if (animationState.isAnimating && i == 0) {
                        animationState.animationStep
                    } else {
                        0
                    },
                    finalRotation = finalRotation,
                    onAnimationStepComplete = { step ->
                        handleAnimationStepComplete(
                            step = step,
                            cardIndex = i,
                            onStepChange = { newStep ->
                                animationState = animationState.copy(animationStep = newStep)
                            },
                            onAnimationComplete = {
                                completeCardSwapAnimation(
                                    cards = currentCards,
                                    onStateChange = { newState -> animationState = newState },
                                    onCardsReorder = { newCards -> currentCards = newCards }
                                )
                            }
                        )
                    }
                )
            }
        }
    }
}

/**
 * Общий метод для обработки свайпов
 */
fun handleDragEnd(
    verticalDragDistance: Float,
    horizontalDragDistance: Float,
    onFanStateChange: (Boolean) -> Unit,
    onCardsReorder: () -> Unit
) {
    val verticalThreshold = 100f
    val horizontalThreshold = 100f

    // Раскрытие/закрытие карт в зависимости от направления вертикального свайпа
    when {
        verticalDragDistance < -verticalThreshold -> onFanStateChange(true)
        verticalDragDistance > verticalThreshold -> onFanStateChange(false)
    }

    // Если горизонтальный свайп был достаточно большим, перетасовываем карты
    if (abs(horizontalDragDistance) > horizontalThreshold) {
        onCardsReorder()
    }
}