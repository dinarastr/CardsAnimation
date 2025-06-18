package ru.yandexpraktikum.cardsanimation.compose

import androidx.compose.animation.core.Animatable
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
import kotlinx.coroutines.launch
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
    onAnimationComplete: (() -> Unit)? = null
) {
    val density = LocalDensity.current
    
    // Animation state management
    var animationState by remember { mutableStateOf(CardAnimationState.NORMAL) }
    
    // Animatable values for smooth transitions
    val animatedTranslationX = remember { Animatable(0f) }
    val animatedTranslationY = remember { Animatable(0f) }
    val animatedRotation = remember { Animatable(currentRotation) }
    
    // Elevation for bringing card to front
    val cardElevation = when {
        isAnimating && animationState == CardAnimationState.MOVING_TO_TOP -> {
            (4 + cardIndex + 30).dp // Extra high elevation during move to top
        }
        isAnimating && animationState != CardAnimationState.NORMAL -> {
            (4 + cardIndex + 20).dp // High elevation during other animation states
        }
        else -> {
            (4 + cardIndex * 1).dp // Normal elevation
        }
    }
    
    // Normal rotation animation (when not doing complex animation)
    val normalRotation by animateFloatAsState(
        targetValue = if (isAnimating) animatedRotation.value else targetRotation,
        animationSpec = tween(durationMillis = 300),
        label = "normal_rotation"
    )
    
    // Handle the 3-step animation sequence
    LaunchedEffect(isAnimating) {
        if (isAnimating) {
            // Step 1: Move slightly to the right (800ms)
            animationState = CardAnimationState.MOVING_RIGHT
            
            // Calculate movement in screen coordinates considering current rotation
            val moveDistance = with(density) { 50.dp.toPx() }
            val currentRotationRad = Math.toRadians(animatedRotation.value.toDouble())
            val deltaX = moveDistance * cos(currentRotationRad).toFloat()
            val deltaY = moveDistance * sin(currentRotationRad).toFloat()
            
            // Move X and Y simultaneously using async
            val moveXJob = launch {
                animatedTranslationX.animateTo(
                    targetValue = deltaX,
                    animationSpec = tween(durationMillis = 800)
                )
            }
            val moveYJob = launch {
                animatedTranslationY.animateTo(
                    targetValue = deltaY,
                    animationSpec = tween(durationMillis = 800)
                )
            }
            
            // Wait for both movements to complete
            moveXJob.join()
            moveYJob.join()
            
            // Step 2: Move to center position (1200ms) - preserving rotation
            // Change state BEFORE starting the movement so elevation increases
            animationState = CardAnimationState.MOVING_TO_TOP
            
            // Add a small delay to make the elevation change visible
            delay(100)
            
            // Move back to center simultaneously
            val centerXJob = launch {
                animatedTranslationX.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(durationMillis = 1200)
                )
            }
            val centerYJob = launch {
                animatedTranslationY.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(durationMillis = 1200)
                )
            }
            
            // Wait for both movements to complete
            centerXJob.join()
            centerYJob.join()
            
            // Step 3: Rotate to final position (800ms)
            animationState = CardAnimationState.FINAL_ADJUST
            
            animatedRotation.animateTo(
                targetValue = targetRotation,
                animationSpec = tween(durationMillis = 800)
            )
            
            // Reset state and notify completion
            animationState = CardAnimationState.NORMAL
            onAnimationComplete?.invoke()
        } else {
            // Reset to normal state when not animating
            if (animationState != CardAnimationState.NORMAL) {
                animatedTranslationX.snapTo(0f)
                animatedTranslationY.snapTo(0f)
                animatedRotation.snapTo(targetRotation)
                animationState = CardAnimationState.NORMAL
            }
        }
    }
    
    // Update rotation when targetRotation changes (for normal animations)
    LaunchedEffect(targetRotation) {
        if (!isAnimating) {
            animatedRotation.animateTo(
                targetValue = targetRotation,
                animationSpec = tween(durationMillis = 300)
            )
        }
    }

    Card(
        modifier = Modifier
            .size(width = 100.dp, height = 160.dp)
            .graphicsLayer {
                translationX = if (isAnimating) animatedTranslationX.value else 0f
                translationY = if (isAnimating) animatedTranslationY.value else 0f
                rotationZ = if (isAnimating) animatedRotation.value else normalRotation
                transformOrigin = TransformOrigin(0.5f, 1.0f)
                
                // Bring the card to front only during step 2 and 3
                if (isAnimating && (animationState == CardAnimationState.MOVING_TO_TOP || animationState == CardAnimationState.FINAL_ADJUST)) {
                    scaleX = 1.001f // Tiny scale to force layer creation
                    scaleY = 1.001f
                }
            }
            // Use zIndex to bring card to front ONLY during step 2 and 3
            .let { modifier ->
                if (isAnimating && (animationState == CardAnimationState.MOVING_TO_TOP || animationState == CardAnimationState.FINAL_ADJUST)) {
                    modifier.zIndex(1000f) // High z-index only during step 2 and 3
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