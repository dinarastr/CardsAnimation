package ru.yandexpraktikum.cardsanimation.compose

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntOffsetAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import ru.yandexpraktikum.cardsanimation.model.CardData

@Composable
fun AnimatedStackCard(
    cardIndex: Int,
    totalCards: Int,
    cardData: CardData,
    isRotated: Boolean,
    baseRotation: Float
) {
    // Расчёт расположения карт в зависимости от состояния
    val targetRotation = if (isRotated) {
        // В развёрнутом состоянии карты занимают половину окружности
        val angleStep = if (totalCards > 1) 180f / (totalCards - 1) else 0f
        -90f + (cardIndex * angleStep) // Distribute from -90° to +90°
    } else {
        // В свёрнутом карты возвращаются в исходное положение
        baseRotation
    }

    val animatedRotationZ by animateFloatAsState(
        targetValue = targetRotation,
        animationSpec = tween(durationMillis = 300),
        label = "card_rotation_$cardIndex"
    )

    val animatedOffset by animateIntOffsetAsState(
        targetValue = IntOffset(0, 0),
        animationSpec = tween(durationMillis = 300),
        label = "card_position_$cardIndex"
    )

    Card(
        modifier = Modifier
            .size(width = 100.dp, height = 160.dp)
            .offset { animatedOffset }
            .graphicsLayer {
                rotationZ = animatedRotationZ
                transformOrigin = TransformOrigin(
                    0.5f,
                    1.0f
                )
            },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = (4 + cardIndex * 1).dp
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