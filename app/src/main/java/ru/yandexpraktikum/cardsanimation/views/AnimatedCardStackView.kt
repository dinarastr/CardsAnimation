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
            val isVerticalSwipe = abs(verticalMovement) > abs(horizontalMovement)

            if (isVerticalSwipe) {
                dragOffsetY += verticalMovement
            }
            return true
        }

        override fun onFling(
            e1: MotionEvent?,
            e2: MotionEvent,
            velocityX: Float,
            velocityY: Float
        ): Boolean {
            val isHorizontalFling = abs(velocityX) > abs(velocityY)
            val isVerticalFling = abs(velocityY) > abs(velocityX)

            if (isHorizontalFling && abs(velocityX) > 500f) {
                handleHorizontalSwipe()
            }

            if (isVerticalFling) {
                handleVerticalSwipe()
            }
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
            handleVerticalSwipe()
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
     * При свайпе вправо или влево нужно перетасовать карты
     * Перемещает нижнюю карту (первый элемент) наверх (в конец списка)
     * Вызывается из onFling() - выполняется только один раз на жест
     */
    private fun handleHorizontalSwipe() {
        // Берём нижнюю карту (первый элемент) и перемещаем в конец списка
        val reorderedCards = cardDataList.drop(1) + cardDataList.first()

        // Обновляем данные карт без пересоздания view
        cardDataList = reorderedCards
        cards.forEachIndexed { index, cardView ->
            cardView.setCardData(cardDataList[index])
        }
    }

    /**
     * Обработка окончания свайпа вверх или вниз (когда пользователь убирает палец с экрана)
     */
    private fun handleVerticalSwipe() {
        val threshold = 100f

        when {
            dragOffsetY < -threshold -> isRotated = true
            dragOffsetY > threshold -> isRotated = false
        }

        dragOffsetY = 0f
        updateCardPositions()
    }
}