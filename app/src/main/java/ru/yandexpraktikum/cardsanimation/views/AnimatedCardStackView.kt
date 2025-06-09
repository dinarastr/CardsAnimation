package ru.yandexpraktikum.cardsanimation.views

import android.content.Context
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.widget.FrameLayout
import ru.yandexpraktikum.cardsanimation.R
import ru.yandexpraktikum.cardsanimation.model.CardData
import kotlin.math.abs

class AnimatedCardStackView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val cards = mutableListOf<AnimatedCardView>()
    private var cardDataList = listOf<CardData>()

    private var cardOffset = 0
    private var isRotated = false
    private var dragOffsetY = 0f

    // Инстанс GestureDetector для обработки жестов
    private val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {

        override fun onScroll(
            e1: MotionEvent?,
            e2: MotionEvent,
            distanceX: Float,
            distanceY: Float
        ): Boolean {
            val horizontalMovement = -distanceX
            val verticalMovement = -distanceY

            handleGestureMovement(horizontalMovement, verticalMovement)
            return true
        }

        override fun onFling(
            e1: MotionEvent?,
            e2: MotionEvent,
            velocityX: Float,
            velocityY: Float
        ): Boolean {
            handleGestureEnd()
            return true
        }
    })

    init {
        setCards(
            listOf(
                CardData(R.drawable.card_clover),
                CardData(R.drawable.card_hearts),
                CardData(R.drawable.card_spades),
                CardData(R.drawable.card_diamond)
            )
        )
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        gestureDetector.onTouchEvent(event)

        if (event.action == MotionEvent.ACTION_UP) {
            handleGestureEnd()
        }

        return true
    }

    fun setCards(newCardDataList: List<CardData>) {
        cardDataList = newCardDataList
        setupCards()
    }

    private fun setupCards() {
        clearCards()
        cardDataList.forEachIndexed { index, cardData ->
            val cardView = AnimatedCardView(context).apply {
                setCardData(cardData)
                setStackPosition(index)
            }
            cards.add(cardView)
            addView(cardView)
        }

        // Возврат в исходное положение
        cardOffset = 0
        isRotated = false

        updateCardPositions()
    }

    private fun clearCards() {
        cards.clear()
        removeAllViews()
    }

    private fun updateCardPositions() {
        val cardCount = cards.size

        cards.forEachIndexed { index, cardView ->
            // Расчёт расположения карт в исходной позиции
            val baseRotation = if (cardCount > 1) {
                val angleStep = 45f / (cardCount - 1)
                22.5f - (index * angleStep)
            } else {
                0f
            }

            // Расчёт финальной позиции (для эффекта раскрытой колоды карт)
            val targetRotation = if (isRotated) {
                val angleStep = if (cardCount > 1) 180f / (cardCount - 1) else 0f
                -90f + (index * angleStep)
            } else {
                baseRotation
            }

            val cardWidth = 100f * resources.displayMetrics.density
            val cardHeight = 160f * resources.displayMetrics.density
            val sharedX = width / 2f - cardWidth / 2f
            val sharedY = height / 2f - cardHeight / 2f

            cardView.x = sharedX
            cardView.y = sharedY

            cardView.pivotX = cardWidth / 2f
            cardView.pivotY = cardHeight
            cardView.animateToRotation(targetRotation)
        }
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        if (changed) {
            updateCardPositions()
        }
    }

    /**
     * Метод для обработки жестов и определения, в каком направлении произошёл свайп
     */
    private fun handleGestureMovement(horizontalMovement: Float, verticalMovement: Float) {
        val isHorizontalSwipe = abs(horizontalMovement) > abs(verticalMovement)
        val isVerticalSwipe = abs(verticalMovement) > abs(horizontalMovement)

        if (isVerticalSwipe) {
            dragOffsetY += verticalMovement
        }

        if (isHorizontalSwipe) {
            handleHorizontalSwipe(horizontalMovement)
        }
    }

    /**
     * При свайпе вправо или влево нужно перетасовать карты
     */
    private fun handleHorizontalSwipe(horizontalMovement: Float) {
        val swipeThreshold = 50f

        if (abs(horizontalMovement) > swipeThreshold) {
            cardOffset = if (cardOffset - 1 < 0) cardDataList.size - 1 else cardOffset - 1
            updateCardData()
        }
    }

    /**
     * Обработка окончания свайпа (когда пользователь убирает палец с экрана)
     */
    private fun handleGestureEnd() {
        val threshold = 100f

        when {
            dragOffsetY < -threshold -> isRotated = true   // Swiped up = fan out
            dragOffsetY > threshold -> isRotated = false   // Swiped down = fold back
        }

        dragOffsetY = 0f
        updateCardPositions()
    }

    /**
     * Метод для перетасовки карт
     */
    private fun updateCardData() {
        cards.forEachIndexed { index, cardView ->
            val actualCardIndex = (index + cardOffset) % cardDataList.size
            cardView.setCardData(cardDataList[actualCardIndex])
        }
    }
}