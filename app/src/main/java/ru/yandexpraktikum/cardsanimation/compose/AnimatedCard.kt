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
    animationStep: Int = 0,
    finalRotation: Float = targetRotation,
    onAnimationStepComplete: ((Int) -> Unit)? = null
) {
    val density = LocalDensity.current

    val animatedTranslationX by animateFloatAsState(
        targetValue = if (isAnimating && animationStep == 1) {
            val moveDistance = with(density) { 50.dp.toPx() }
            val rotationRad = Math.toRadians(targetRotation.toDouble())
            moveDistance * cos(rotationRad).toFloat()
        } else 0f,
        animationSpec = tween(durationMillis = 300),
        finishedListener = { if (isAnimating && animationStep == 1) onAnimationStepComplete?.invoke(1) },
        label = "translationX"
    )

    val animatedTranslationY by animateFloatAsState(
        targetValue = if (isAnimating && animationStep == 1) {
            val moveDistance = with(density) { 50.dp.toPx() }
            val rotationRad = Math.toRadians(targetRotation.toDouble())
            moveDistance * sin(rotationRad).toFloat()
        } else 0f,
        animationSpec = tween(durationMillis = 300),
        finishedListener = { if (isAnimating && animationStep == 2) onAnimationStepComplete?.invoke(2) },
        label = "translationY"
    )

    val animatedRotation by animateFloatAsState(
        targetValue = when {
            animationStep == 3 -> finalRotation
            isAnimating -> targetRotation
            else -> targetRotation
        },
        animationSpec = tween(durationMillis = if (animationStep == 3) 300 else 800),
        finishedListener = {
            if (animationStep == 3 && isAnimating) onAnimationStepComplete?.invoke(3)
        },
        label = "rotation"
    )

    // Поднимаем карту наверх только после второго шага
    // (после того, как нижняя карта выдвинулась вправо)
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