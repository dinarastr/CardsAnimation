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

    private var cardDataList: List<CardData> = emptyList()
    private val cards = mutableListOf<AnimatedCardView>()

    private var isRotated = false
    private var horizontalDragOffset = 0f
    private var verticalDragOffset = 0f

    private var isAnimating = false
    private var animationStep = 0

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
            val isHorizontalSwipe = abs(horizontalMovement) > abs(verticalMovement)

            if (isVerticalSwipe) {
                verticalDragOffset += verticalMovement
            }
            if (isHorizontalSwipe) {
                horizontalDragOffset += horizontalMovement
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
            handleDragEnd()
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
                90f - (index * angleStep)
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
        if (isAnimating) return

        val bottomCard = cards.firstOrNull() ?: return
        startCardSwapAnimation(bottomCard)
    }

    /**
     * Начинает анимацию перестановки карт
     * Шаг 1: Перемещение нижней карты вправо
     */
    private fun startCardSwapAnimation(bottomCard: AnimatedCardView) {
        isAnimating = true
        animationStep = 1

        bottomCard.moveCardRight {
            animationStep = 2
            bringCardToFront(bottomCard)
            moveCardToTopPosition(bottomCard)
        }
    }

    /**
     * Выносит карту на передний план с максимальной высотой
     */
    private fun bringCardToFront(card: AnimatedCardView) {
        card.bringToFront()
        val maxElevation = (4 + cards.size + 20).toFloat() * resources.displayMetrics.density
        card.cardView.cardElevation = maxElevation
    }

    /**
     * Шаг 2: Перемещение карты в верхнюю позицию с сохранением поворота
     */
    private fun moveCardToTopPosition(bottomCard: AnimatedCardView) {
        bottomCard.moveCardToTop {
            animationStep = 3
            reorderCardsData()
            animateAllCardsToFinalPositions()
        }
    }

    /**
     * Обновляет порядок данных карт и представлений
     */
    private fun reorderCardsData() {
        // Обновляем порядок данных карт
        val reorderedCards = cardDataList.drop(1) + cardDataList.first()
        cardDataList = reorderedCards

        // Переставляем карту в списке представлений
        val bottomCardView = cards.removeAt(0)
        cards.add(bottomCardView)

        // Обновляем данные карт в соответствии с новым порядком
        cards.forEachIndexed { index, cardView ->
            cardView.setCardData(cardDataList[index])
        }
    }

    /**
     * Шаг 3: Анимирует все карты к их финальным позициям
     */
    private fun animateAllCardsToFinalPositions() {
        var completedAnimations = 0
        val totalAnimations = cards.size

        cards.forEachIndexed { index, cardView ->
            val finalRotation = calculateFinalRotation(index)

            cardView.adjustToFinalPosition(finalRotation, index) {
                completedAnimations++
                if (completedAnimations == totalAnimations) {
                    finalizeCardPositions()
                }
            }
        }
    }

    /**
     * Вычисляет финальный поворот для карты на заданной позиции
     */
    private fun calculateFinalRotation(cardIndex: Int): Float {
        val cardCount = cards.size
        return if (isRotated) {
            // Если карты развернуты веером
            val angleStep = if (cardCount > 1) 180f / (cardCount - 1) else 0f
            90f - (cardIndex * angleStep)
        } else {
            // Если карты сложены в стопку
            val angleStep = if (cardCount > 1) 45f / (cardCount - 1) else 0f
            22.5f - (cardIndex * angleStep)
        }
    }

    /**
     * Финальная настройка позиций всех карт после завершения анимации
     */
    private fun finalizeCardPositions() {
        cards.forEachIndexed { index, card ->
            card.setStackPosition(index)
            // Устанавливаем точный финальный поворот
            val correctRotation = calculateFinalRotation(index)
            card.rotation = correctRotation
        }

        isAnimating = false
        animationStep = 0
    }

    /**
     * Обработка окончания любого свайпа - определяет доминирующее направление
     */
    private fun handleDragEnd() {
        if (isAnimating) return

        val threshold = 100f
        val isVerticalDominant = abs(verticalDragOffset) > abs(horizontalDragOffset)
        val isHorizontalDominant = abs(horizontalDragOffset) > abs(verticalDragOffset)

        when {
            isVerticalDominant && abs(verticalDragOffset) > threshold -> {
                handleVerticalSwipe()
            }
            isHorizontalDominant && abs(horizontalDragOffset) > threshold -> {
                handleHorizontalSwipeFromDrag()
            }
        }

        // Сбрасываем оба offset'а в любом случае
        verticalDragOffset = 0f
        horizontalDragOffset = 0f
    }

    /**
     * Обработка окончания свайпа вверх или вниз (когда пользователь убирает палец с экрана)
     */
    private fun handleVerticalSwipe() {
        when {
            verticalDragOffset < -100f -> isRotated = true
            verticalDragOffset > 100f -> isRotated = false
        }
        updateCardPositions()
    }

    /**
     * Обработка окончания горизонтального свайпа через drag offset
     */
    private fun handleHorizontalSwipeFromDrag() {
        val bottomCard = cards.firstOrNull()
        if (bottomCard != null) {
            startCardSwapAnimation(bottomCard)
        }
    }
}