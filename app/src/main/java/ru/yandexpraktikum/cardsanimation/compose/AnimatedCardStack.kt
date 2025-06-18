package ru.yandexpraktikum.cardsanimation.compose

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
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
    
    // Simple animation state
    var isAnimating by remember { mutableStateOf(false) }
    var animationStep by remember { mutableIntStateOf(0) } // 0=normal, 1=right, 2=center, 3=final

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
                                if (!isAnimating) {
                                    isAnimating = true
                                    animationStep = 1 // Start with step 1
                                }
                            }
                        )
                        verticalDragOffset = 0f
                        horizontalDragOffset = 0f
                    }
                ) { _, dragAmount ->
                    if (!isAnimating) {
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
                // Calculate rotations
                val baseRotation = if (cardCount > 1) {
                    val angleStep = 45f / (cardCount - 1)
                    22.5f - (i * angleStep)
                } else 0f

                val targetRotation = if (isRotated) {
                    val angleStep = if (cardCount > 1) 180f / (cardCount - 1) else 0f
                    90f - (i * angleStep)
                } else baseRotation

                val finalRotation = if (animationStep == 3) {
                    // Calculate position after bottom card moves to top
                    val newIndex = if (i == 0) cardCount - 1 else i - 1
                    if (isRotated) {
                        val angleStep = if (cardCount > 1) 180f / (cardCount - 1) else 0f
                        90f - (newIndex * angleStep)
                    } else {
                        val angleStep = if (cardCount > 1) 45f / (cardCount - 1) else 0f
                        22.5f - (newIndex * angleStep)
                    }
                } else targetRotation

                AnimatedCard(
                    cardIndex = i,
                    targetRotation = targetRotation,
                    cardData = cardData,
                    isAnimating = isAnimating && i == 0, // Only animate bottom card
                    animationStep = if (isAnimating && i == 0) animationStep else 0,
                    finalRotation = finalRotation,
                    onAnimationStepComplete = { step ->
                        if (i == 0) { // Only bottom card triggers progression
                            when (step) {
                                1 -> animationStep = 2 // Move to step 2
                                2 -> animationStep = 3 // Move to step 3
                                3 -> {
                                    // Animation complete, reorder data
                                    isAnimating = false
                                    animationStep = 0
                                    currentCards = currentCards.drop(1) + currentCards.first()
                                }
                            }
                        }
                    }
                )
            }
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