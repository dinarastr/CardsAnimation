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
                        handleDragEnd(
                            verticalDragDistance = verticalDragOffset,
                            horizontalDragDistance = horizontalDragOffset,
                            onFanStateChange = { newFanState -> isRotated = newFanState },
                            onCardsReorder = {
                                // Start animation if not already animating
                                if (!isAnimatingSwap && !triggerFinalStep) {
                                    isAnimatingSwap = true
                                    animatingCardIndex = 0 // Always animate bottom card
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
        // Use snapshot of cards during final step to prevent recomposition issues
        val cardsToRender = if (triggerFinalStep) {
            remember { currentCards.toList() }
        } else {
            currentCards
        }
        
        cardsToRender.forEachIndexed { i, cardData ->
            key(cardData.imageResId) {
                // Calculate base rotation (collapsed state)
                val baseRotation = if (cardCount > 1) {
                    val angleStep = 45f / (cardCount - 1)
                    22.5f - (i * angleStep)
                } else {
                    0f
                }

                // Calculate target rotation (current fan state)
                val targetRotation = if (isRotated) {
                    val angleStep = if (cardCount > 1) 180f / (cardCount - 1) else 0f
                    90f - (i * angleStep)
                } else {
                    baseRotation
                }

                // Calculate final rotation for step 3 (after reordering)
                val finalRotation = if (triggerFinalStep) {
                    val newIndex = if (i == justCompletedAnimationCardIndex) {
                        cardCount - 1 // Animated card becomes top card
                    } else if (i > justCompletedAnimationCardIndex) {
                        i - 1 // Cards above shift down
                    } else {
                        i // Cards below stay in place
                    }
                    
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
                            // Steps 1-2 completed, prepare for final step
                            justCompletedAnimationCardIndex = i
                            isAnimatingSwap = false
                            animatingCardIndex = -1
                            triggerFinalStep = true
                        }
                    }
                )
            }
        }
    }
    
    // Handle final step completion and data reordering
    LaunchedEffect(triggerFinalStep) {
        if (triggerFinalStep) {
            delay(1000) // Wait for final animations to complete
            
            // Reorder data: move first card to end
            val reorderedCards = currentCards.drop(1) + currentCards.first()
            currentCards = reorderedCards
            
            // Reset animation state
            triggerFinalStep = false
            justCompletedAnimationCardIndex = -1
        }
    }
}

/**
 * Handle drag end gesture - determines fan state and card reordering
 */
fun handleDragEnd(
    verticalDragDistance: Float,
    horizontalDragDistance: Float,
    onFanStateChange: (Boolean) -> Unit,
    onCardsReorder: () -> Unit
) {
    val verticalThreshold = 100f
    val horizontalThreshold = 100f

    // Handle vertical movement (fan/unfan)
    when {
        verticalDragDistance < -verticalThreshold -> onFanStateChange(true)
        verticalDragDistance > verticalThreshold -> onFanStateChange(false)
    }

    // Handle horizontal movement (card reordering)
    if (abs(horizontalDragDistance) > horizontalThreshold) {
        onCardsReorder()
    }
}