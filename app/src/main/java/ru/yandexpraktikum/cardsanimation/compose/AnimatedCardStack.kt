package ru.yandexpraktikum.cardsanimation.compose

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
reimport androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import ru.yandexpraktikum.cardsanimation.model.CardData
import kotlin.math.abs

/**
 * Data class to hold animation state
 */
data class CardSwapAnimationState(
    val isAnimating: Boolean = false,
    val animationStep: Int = 0 // 0=normal, 1=right, 2=center, 3=final
)

/**
 * Calculate the rotation angle for a card at given index
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
 * Calculate the final rotation after card reordering (bottom card moves to top)
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
 * Handle animation step progression
 */
fun handleAnimationStepComplete(
    step: Int,
    cardIndex: Int,
    onStepChange: (Int) -> Unit,
    onAnimationComplete: () -> Unit
) {
    if (cardIndex == 0) { // Only bottom card triggers progression
        when (step) {
            1 -> onStepChange(2) // Move to step 2
            2 -> onStepChange(3) // Move to step 3
            3 -> onAnimationComplete() // Animation complete
        }
    }
}

/**
 * Start card swap animation
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
 * Complete card swap animation and reorder data
 */
fun completeCardSwapAnimation(
    cards: List<CardData>,
    onStateChange: (CardSwapAnimationState) -> Unit,
    onCardsReorder: (List<CardData>) -> Unit
) {
    // Reset animation state
    onStateChange(CardSwapAnimationState())
    
    // Reorder cards: move first card to end
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
    
    // Animation state
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
                    isAnimating = animationState.isAnimating && i == 0, // Only animate bottom card
                    animationStep = if (animationState.isAnimating && i == 0) animationState.animationStep else 0,
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