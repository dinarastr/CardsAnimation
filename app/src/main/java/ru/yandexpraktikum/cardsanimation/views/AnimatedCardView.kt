package ru.yandexpraktikum.cardsanimation.views

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

    fun setStackPosition(index: Int) {
        cardView.cardElevation = (4 + index * 1).toFloat() * resources.displayMetrics.density
    }

    // TODO: Add animation methods here
    // Hint: Use ObjectAnimator for smooth property animations
    
    // TODO: Add rotation animation method
    // fun animateToRotation(targetRotation: Float, duration: Long = 300) { ... }
    
    // TODO: Add horizontal movement animation for card swapping
    // fun moveCardRight(onComplete: (() -> Unit)? = null) { ... }
    
    // TODO: Add movement to top animation for card swapping  
    // fun moveCardToTop(onComplete: (() -> Unit)? = null) { ... }
    
    // TODO: Add final position adjustment animation
    // fun adjustToFinalPosition(finalRotation: Float, finalZOrder: Int, onComplete: (() -> Unit)? = null) { ... }
} 