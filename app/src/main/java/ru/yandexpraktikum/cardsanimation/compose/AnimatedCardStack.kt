package ru.yandexpraktikum.cardsanimation.compose

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import kotlinx.coroutines.delay

@Composable
fun AnimatedCardStack(cards: List<CardData>) {
    val cardCount = cards.size
    var isRotated by remember { mutableStateOf(false) }
    var currentCards by remember { mutableStateOf(cards) }
    var verticalDragOffset by remember { mutableFloatStateOf(0f) }
    var horizontalDragOffset by remember { mutableFloatStateOf(0f) }
    var isAnimatingSwap by remember { mutableStateOf(false) }
    var animatingCardIndex by remember { mutableStateOf(-1) }
    var triggerFinalStep by remember { mutableStateOf(false) }
    var justCompletedAnimationCardIndex by remember { mutableStateOf(-1) }

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
                                // Start the 3-step animation if not already animating
                                if (!isAnimatingSwap && !triggerFinalStep) {
                                    println("Starting card reorder animation")
                                    isAnimatingSwap = true
                                    animatingCardIndex = 0 // Bottom card
                                    triggerFinalStep = false
                                }
                            }
                        )
                        verticalDragOffset = 0f
                        horizontalDragOffset = 0f
                    }
                ) { _, dragAmount ->
                    // Only handle gestures if not currently animating
                    if (!isAnimatingSwap) {
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
                }
            },
        contentAlignment = Alignment.Center
    ) {
        // Отрисовка колоды карт в исходной позиции
        // Use snapshot of cards during animation to prevent recomposition issues
        val cardsToRender = if (triggerFinalStep) {
            // During final step, use the cards before reordering to prevent visual jumps
            remember { currentCards.toList() }
        } else {
            currentCards
        }
        
        cardsToRender.forEachIndexed { i, cardData ->
            // Use stable key to help Compose track card identity during animation
            key(cardData.imageResId) {

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

            // Calculate final rotation based on where this card WILL BE after reordering
            val finalRotation = if (triggerFinalStep) {
                // Calculate the card's position after bottom card moves to top
                val newIndex = if (i == justCompletedAnimationCardIndex) {
                    // The card that just completed animation becomes top card (last position)
                    cardCount - 1
                } else if (i == 0 && justCompletedAnimationCardIndex == 0) {
                    // This case shouldn't happen but handle it
                    cardCount - 1
                } else {
                    // All other cards shift down by 1 if they were above the animated card
                    if (i > justCompletedAnimationCardIndex) {
                        i - 1
                    } else {
                        i
                    }
                }
                
                // Calculate rotation for new position
                if (isRotated) {
                    val angleStep = if (cardCount > 1) 180f / (cardCount - 1) else 0f
                    90f - (newIndex * angleStep)
                } else {
                    val angleStep = if (cardCount > 1) 45f / (cardCount - 1) else 0f
                    22.5f - (newIndex * angleStep)
                }
            } else {
                targetRotation
            }

            AnimatedCard(
                cardIndex = i,
                targetRotation = targetRotation,
                cardData = cardData,
                isAnimating = isAnimatingSwap && i == animatingCardIndex,
                currentRotation = if (isAnimatingSwap && i == animatingCardIndex) baseRotation else targetRotation,
                triggerFinalAnimation = triggerFinalStep,
                finalRotation = finalRotation,
                keepAtTopPosition = i == justCompletedAnimationCardIndex && triggerFinalStep,
                onAnimationComplete = {
                    if (i == animatingCardIndex) {
                        println("Steps 1-2 completed for card $i")
                        // Steps 1-2 completed, DON'T reorder data yet to avoid recomposition
                        
                        // Mark this card as just completed animation (to maintain its top position)
                        justCompletedAnimationCardIndex = i
                        
                        // Reset animation state
                        isAnimatingSwap = false
                        animatingCardIndex = -1
                        
                        // Trigger final step for all cards (like XML)
                        println("Setting triggerFinalStep = true")
                        triggerFinalStep = true
                        
                        // Data will be reordered AFTER animation completes
                    }
                }
            )
            }
        }
    }
    
    // Handle final step completion and data reordering
    LaunchedEffect(triggerFinalStep) {
        println("LaunchedEffect triggered with triggerFinalStep = $triggerFinalStep")
        if (triggerFinalStep) {
            println("Starting final step delay...")
            delay(1000) // Wait for 800ms final animations + 200ms buffer to ensure visual completion
            
            println("Final step delay completed, reordering data...")
            // Reorder data
            println("Reordering cards: ${currentCards.map { it.imageResId }}")
            val reorderedCards = currentCards.drop(1) + currentCards.first()
            currentCards = reorderedCards
            println("After reordering: ${currentCards.map { it.imageResId }}")
            
            // Reset all animation state
            triggerFinalStep = false
            justCompletedAnimationCardIndex = -1
            println("triggerFinalStep reset to false")
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