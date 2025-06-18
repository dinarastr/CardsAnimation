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
 * Animation states for the 2-step card movement
 */
enum class CardAnimationState {
    NORMAL,        // Normal position
    MOVING_RIGHT,  // Step 1: Moving right
    MOVING_TO_TOP  // Step 2: Moving to top position
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
    keepAtTopPosition: Boolean = false,
    onAnimationComplete: (() -> Unit)? = null
) {
    val density = LocalDensity.current
    var animationState by remember { mutableStateOf(CardAnimationState.NORMAL) }
    
    // Translation animations (only active during isAnimating)
    val animatedTranslationX by animateFloatAsState(
        targetValue = if (isAnimating && animationState == CardAnimationState.MOVING_RIGHT) {
            val moveDistance = with(density) { 50.dp.toPx() }
            val rotationRad = Math.toRadians(currentRotation.toDouble())
            moveDistance * cos(rotationRad).toFloat()
        } else 0f,
        animationSpec = tween(durationMillis = if (animationState == CardAnimationState.MOVING_RIGHT) 800 else 1200),
        label = "translationX"
    )
    
    val animatedTranslationY by animateFloatAsState(
        targetValue = if (isAnimating && animationState == CardAnimationState.MOVING_RIGHT) {
            val moveDistance = with(density) { 50.dp.toPx() }
            val rotationRad = Math.toRadians(currentRotation.toDouble())
            moveDistance * sin(rotationRad).toFloat()
        } else 0f,
        animationSpec = tween(durationMillis = if (animationState == CardAnimationState.MOVING_RIGHT) 800 else 1200),
        label = "translationY"
    )
    
    // Rotation animation
    val animatedRotation by animateFloatAsState(
        targetValue = when {
            triggerFinalAnimation -> finalRotation
            isAnimating -> currentRotation
            else -> targetRotation
        },
        animationSpec = tween(durationMillis = if (triggerFinalAnimation) 800 else 300),
        label = "rotation"
    )
    
    // Determine if this card should be brought to front
    val shouldBringToFront = (isAnimating && animationState == CardAnimationState.MOVING_TO_TOP) || keepAtTopPosition
    
    // Handle the 2-step animation sequence
    LaunchedEffect(isAnimating) {
        if (isAnimating) {
            // Step 1: Move right (800ms)
            animationState = CardAnimationState.MOVING_RIGHT
            delay(800)
            
            // Step 2: Move to center (1200ms)
            animationState = CardAnimationState.MOVING_TO_TOP
            delay(1200)
            
            // Steps completed
            animationState = CardAnimationState.NORMAL
            onAnimationComplete?.invoke()
        } else {
            animationState = CardAnimationState.NORMAL
        }
    }

    Card(
        modifier = Modifier
            .size(width = 100.dp, height = 160.dp)
            .graphicsLayer {
                translationX = if (isAnimating) animatedTranslationX else 0f
                translationY = if (isAnimating) animatedTranslationY else 0f
                rotationZ = animatedRotation
                transformOrigin = TransformOrigin(0.5f, 1.0f)
                
                // Force layer creation when bringing to front
                if (shouldBringToFront) {
                    scaleX = 1.001f
                    scaleY = 1.001f
                }
            }
            .let { modifier ->
                if (shouldBringToFront) {
                    modifier.zIndex(1000f)
                } else {
                    modifier
                }
            },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = (4 + cardIndex).dp
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