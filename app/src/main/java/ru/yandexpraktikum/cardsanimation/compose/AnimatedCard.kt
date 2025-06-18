package ru.yandexpraktikum.cardsanimation.compose

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import ru.yandexpraktikum.cardsanimation.model.CardData
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

/**
 * Animation states for the 3-step card movement
 */
enum class CardAnimationState {
    NORMAL,        // Normal position
    MOVING_RIGHT,  // Step 1: Moving right
    MOVING_TO_TOP, // Step 2: Moving to top position
    FINAL_ADJUST   // Step 3: Final rotation adjustment
}

@Composable
fun AnimatedCard(
    cardIndex: Int,
    cardData: CardData,
    targetRotation: Float,
    isAnimating: Boolean = false,
    currentRotation: Float = targetRotation,
    triggerFinalAnimation: Boolean = false,
    finalRotation: Float = targetRotation,
    keepAtTopPosition: Boolean = false, // New parameter to maintain top position after animation
    onAnimationComplete: (() -> Unit)? = null
) {
    val density = LocalDensity.current
    
    // Animation state management
    var animationState by remember { mutableStateOf(CardAnimationState.NORMAL) }
    
    // Simple animated values using animateFloatAsState
    val animatedTranslationX by animateFloatAsState(
        targetValue = when {
            isAnimating && animationState == CardAnimationState.MOVING_RIGHT -> {
                // Step 1: Move right
                val moveDistance = with(density) { 50.dp.toPx() }
                val rotationRad = Math.toRadians(currentRotation.toDouble())
                moveDistance * cos(rotationRad).toFloat()
            }
            else -> 0f // Step 2, Step 3, and normal state: stay at center
        },
        animationSpec = tween(durationMillis = if (animationState == CardAnimationState.MOVING_RIGHT) 800 else 1200),
        label = "translationX"
    )
    
    val animatedTranslationY by animateFloatAsState(
        targetValue = when {
            isAnimating && animationState == CardAnimationState.MOVING_RIGHT -> {
                // Step 1: Move right
                val moveDistance = with(density) { 50.dp.toPx() }
                val rotationRad = Math.toRadians(currentRotation.toDouble())
                moveDistance * sin(rotationRad).toFloat()
            }
            else -> 0f // Step 2, Step 3, and normal state: stay at center
        },
        animationSpec = tween(durationMillis = if (animationState == CardAnimationState.MOVING_RIGHT) 800 else 1200),
        label = "translationY"
    )
    
    val animatedRotation by animateFloatAsState(
        targetValue = when {
            triggerFinalAnimation -> finalRotation
            isAnimating -> currentRotation
            else -> targetRotation
        },
        animationSpec = tween(durationMillis = if (triggerFinalAnimation) 800 else 300),
        label = "rotation"
    )
    
    // Elevation for bringing card to front
    val cardElevation = when {
        isAnimating && animationState == CardAnimationState.MOVING_TO_TOP -> {
            (4 + cardIndex + 30).dp
        }
        isAnimating && animationState != CardAnimationState.NORMAL -> {
            (4 + cardIndex + 20).dp
        }
        else -> {
            (4 + cardIndex * 1).dp
        }
    }
    
    // Handle the 3-step animation sequence (only for the animating card)
    LaunchedEffect(isAnimating) {
        if (isAnimating) {
            println("Card $cardIndex: Starting step 1 - moving right")
            // Step 1: Move slightly to the right (800ms)
            animationState = CardAnimationState.MOVING_RIGHT
            delay(800)
            
            println("Card $cardIndex: Starting step 2 - moving to center")
            // Step 2: Move to center position (1200ms) - preserving rotation
            animationState = CardAnimationState.MOVING_TO_TOP
            delay(1200)
            
            println("Card $cardIndex: Steps 1-2 completed")
            // Steps 1-2 completed, notify to trigger step 3 for ALL cards
            animationState = CardAnimationState.NORMAL
            onAnimationComplete?.invoke()
        } else {
            // Reset to normal state when not animating
            animationState = CardAnimationState.NORMAL
        }
    }

    Card(
                    modifier = Modifier
            .size(width = 100.dp, height = 160.dp)
            .graphicsLayer {
                translationX = when {
                    isAnimating -> animatedTranslationX
                    keepAtTopPosition -> 0f // Stay at center when keeping top position
                    else -> 0f
                }
                translationY = when {
                    isAnimating -> animatedTranslationY
                    keepAtTopPosition -> 0f // Stay at center when keeping top position  
                    else -> 0f
                }
                rotationZ = animatedRotation
                transformOrigin = TransformOrigin(0.5f, 1.0f)
                
                // Bring the card to front only during step 2 and 3, or when keeping at top
                if ((isAnimating && (animationState == CardAnimationState.MOVING_TO_TOP || animationState == CardAnimationState.FINAL_ADJUST)) || keepAtTopPosition) {
                    scaleX = 1.001f // Tiny scale to force layer creation
                    scaleY = 1.001f
                }
            }
            // Use zIndex to bring card to front ONLY during step 2 and 3, or when keeping at top
            .let { modifier ->
                if ((isAnimating && (animationState == CardAnimationState.MOVING_TO_TOP || animationState == CardAnimationState.FINAL_ADJUST)) || keepAtTopPosition) {
                    modifier.zIndex(1000f) // High z-index during step 2 and 3, or when keeping at top
                } else {
                    modifier
                }
            },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = cardElevation
        )
    ) {
        Image(
            modifier = Modifier.fillMaxSize(),
            painter = painterResource(cardData.imageResId),
            contentDescription = null,
            contentScale = ContentScale.Crop
        )
    }
}