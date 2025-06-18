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
     * Step 1: Move card slightly to the right
     */
    fun moveCardRight(onComplete: (() -> Unit)? = null) {
        // Calculate movement in screen coordinates, not view coordinates
        val moveDistance = 50f * resources.displayMetrics.density
        val currentRotationRad = Math.toRadians(rotation.toDouble())
        
        // For moving right in screen coordinates when view is rotated:
        // - X component: distance * cos(rotation) 
        // - Y component: distance * sin(rotation)
        val deltaX = moveDistance * Math.cos(currentRotationRad).toFloat()
        val deltaY = moveDistance * Math.sin(currentRotationRad).toFloat()
        
        val currentX = x
        val currentY = y
        
        // Animate both X and Y to achieve right movement in screen space
        val animatorX = ObjectAnimator.ofFloat(this, "x", currentX, currentX + deltaX)
        val animatorY = ObjectAnimator.ofFloat(this, "y", currentY, currentY + deltaY)
        
        val animatorSet = android.animation.AnimatorSet().apply {
            playTogether(animatorX, animatorY)
            duration = 800 // Increased from 300ms to 800ms
            addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    onComplete?.invoke()
                }
            })
        }
        
        animatorSet.start()
    }

    /**
     * Step 2: Move card to the top of the stack while preserving its rotation
     */
    fun moveCardToTop(onComplete: (() -> Unit)? = null) {
        // Calculate the center position where all cards should be
        val parent = parent as? FrameLayout ?: return
        val cardWidth = 100f * resources.displayMetrics.density
        val cardHeight = 160f * resources.displayMetrics.density
        val centerX = parent.width / 2f - cardWidth / 2f
        val centerY = parent.height / 2f - cardHeight / 2f
        
        // Move to center position but keep current rotation
        val animatorX = ObjectAnimator.ofFloat(this, "x", x, centerX)
        val animatorY = ObjectAnimator.ofFloat(this, "y", y, centerY)
        
        val animatorSet = android.animation.AnimatorSet().apply {
            playTogether(animatorX, animatorY)
            duration = 1200 // Increased from 400ms to 1200ms - much slower for visibility
            addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    onComplete?.invoke()
                }
            })
        }
        
        animatorSet.start()
    }

    /**
     * Step 3: Adjust all cards to their final positions and rotations
     */
    fun adjustToFinalPosition(finalRotation: Float, finalZOrder: Int, onComplete: (() -> Unit)? = null) {
        // Only rotate to final position, position is already correct
        ObjectAnimator.ofFloat(this, "rotation", rotation, finalRotation).apply {
            duration = 800 // Increased from 300ms to 800ms
            addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    // Set final elevation
                    setStackPosition(finalZOrder)
                    onComplete?.invoke()
                }
            })
            start()
        }
    }
} 