package ru.yandexpraktikum.cardsanimation.views

import android.animation.ObjectAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.cardview.widget.CardView
import ru.yandexpraktikum.cardsanimation.R
import ru.yandexpraktikum.cardsanimation.model.CardData

class AnimatedCardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    val cardView: CardView
    private val cardImageView: ImageView

    private var currentRotation = 0f
    private var rotationAnimator: ObjectAnimator? = null

    init {
        LayoutInflater.from(context).inflate(R.layout.card_view, this, true)

        cardView = this.getChildAt(0) as CardView
        cardImageView = findViewById(R.id.cardImage)

        pivotX = width / 2f
        pivotY = height.toFloat()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        pivotX = w / 2f
        pivotY = h.toFloat()
    }

    fun setCardData(cardData: CardData) {
        cardImageView.setImageResource(cardData.imageResId)
    }

    fun animateToRotation(targetRotation: Float, duration: Long = 300) {
        rotationAnimator?.cancel()

        rotationAnimator = ObjectAnimator.ofFloat(this, "rotation", rotation, targetRotation).apply {
            this.duration = duration
            addUpdateListener {
                currentRotation = rotation
            }
            start()
        }
    }

    fun setStackPosition(index: Int) {
        cardView.cardElevation = (4 + index * 1).toFloat() * resources.displayMetrics.density
    }

    /**
     * Шаг 1: Выдвигаем нижнюю карту вправо
     */
    fun moveCardRight(onComplete: (() -> Unit)? = null) {
        val moveDistance = 50f * resources.displayMetrics.density
        val currentRotationRad = Math.toRadians(rotation.toDouble())

        val deltaX = moveDistance * Math.cos(currentRotationRad).toFloat()
        val deltaY = moveDistance * Math.sin(currentRotationRad).toFloat()

        val currentX = x
        val currentY = y

        val animatorX = ObjectAnimator.ofFloat(this, "x", currentX, currentX + deltaX)
        val animatorY = ObjectAnimator.ofFloat(this, "y", currentY, currentY + deltaY)

        val animatorSet = android.animation.AnimatorSet().apply {
            playTogether(animatorX, animatorY)
            duration = 300
            addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    onComplete?.invoke()
                }
            })
        }

        animatorSet.start()
    }

    /**
     * Шаг 2: Двигаем карту наверх колоды
     */
    fun moveCardToTop(onComplete: (() -> Unit)? = null) {
        val parent = parent as? FrameLayout ?: return
        val cardWidth = 100f * resources.displayMetrics.density
        val cardHeight = 160f * resources.displayMetrics.density
        val centerX = parent.width / 2f - cardWidth / 2f
        val centerY = parent.height / 2f - cardHeight / 2f

        val animatorX = ObjectAnimator.ofFloat(this, "x", x, centerX)
        val animatorY = ObjectAnimator.ofFloat(this, "y", y, centerY)

        val animatorSet = android.animation.AnimatorSet().apply {
            playTogether(animatorX, animatorY)
            duration = 300
            addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    onComplete?.invoke()
                }
            })
        }

        animatorSet.start()
    }

    /**
     * Шаг 3: Складываем все карты в финальную позицию
     */
    fun adjustToFinalPosition(finalRotation: Float, finalZOrder: Int, onComplete: (() -> Unit)? = null) {
        ObjectAnimator.ofFloat(this, "rotation", rotation, finalRotation).apply {
            duration = 300
            addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    setStackPosition(finalZOrder)
                    onComplete?.invoke()
                }
            })
            start()
        }
    }
} 