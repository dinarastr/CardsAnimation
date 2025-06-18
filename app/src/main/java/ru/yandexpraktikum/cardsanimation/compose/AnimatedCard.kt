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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import ru.yandexpraktikum.cardsanimation.model.CardData
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun AnimatedCard(
    cardIndex: Int,
    cardData: CardData,
    targetRotation: Float,
    isAnimating: Boolean = false,
    animationStep: Int = 0, // 0=normal, 1=move right, 2=move center, 3=final rotation
    finalRotation: Float = targetRotation,
    onAnimationStepComplete: ((Int) -> Unit)? = null
) {
    val density = LocalDensity.current
    
    // Simple translation X animation
    val animatedTranslationX by animateFloatAsState(
        targetValue = if (isAnimating && animationStep == 1) {
            val moveDistance = with(density) { 50.dp.toPx() }
            val rotationRad = Math.toRadians(targetRotation.toDouble())
            moveDistance * cos(rotationRad).toFloat()
        } else 0f,
        animationSpec = tween(durationMillis = 800),
        finishedListener = { if (isAnimating && animationStep == 1) onAnimationStepComplete?.invoke(1) },
        label = "translationX"
    )
    
    // Simple translation Y animation
    val animatedTranslationY by animateFloatAsState(
        targetValue = if (isAnimating && animationStep == 1) {
            val moveDistance = with(density) { 50.dp.toPx() }
            val rotationRad = Math.toRadians(targetRotation.toDouble())
            moveDistance * sin(rotationRad).toFloat()
        } else 0f,
        animationSpec = tween(durationMillis = 800),
        finishedListener = { if (isAnimating && animationStep == 2) onAnimationStepComplete?.invoke(2) },
        label = "translationY"
    )
    
    // Simple rotation animation
    val animatedRotation by animateFloatAsState(
        targetValue = when (animationStep) {
            3 -> finalRotation // Step 3: final rotation
            else -> targetRotation // Normal target rotation
        },
        animationSpec = tween(durationMillis = if (animationStep == 3) 800 else 300),
        finishedListener = { if (animationStep == 3) onAnimationStepComplete?.invoke(3) },
        label = "rotation"
    )
    
    // Determine if card should be brought to front
    val shouldBringToFront = isAnimating && animationStep >= 2

    Card(
        modifier = Modifier
            .size(width = 100.dp, height = 160.dp)
            .graphicsLayer {
                translationX = if (isAnimating) animatedTranslationX else 0f
                translationY = if (isAnimating) animatedTranslationY else 0f
                rotationZ = animatedRotation
                transformOrigin = TransformOrigin(0.5f, 1.0f)
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