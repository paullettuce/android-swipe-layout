package pl.paullettuce.swipelayout.lib

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import androidx.core.view.forEachIndexed
import pl.paullettuce.swipelayout.lib.helpers.*
import pl.paullettuce.swipelayout.lib.helpers.animation.SwipeAnimator
import pl.paullettuce.swipelayout.lib.helpers.background.BackgroundController
import pl.paullettuce.swipelayout.lib.helpers.drag.DragHelper

class SwipeLayout @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr),
    View.OnTouchListener {

    private val allowedSwipeDirection: AllowedSwipeDirectionState = obtainSwipeAllowanceState(context, attrs)
    private val dragHelper =
        DragHelper(this, allowedSwipeDirection)
    private val backgroundController =
        BackgroundController(this, startingMoveThresholdPx = 5f)
    private val swipeAnimator = SwipeAnimator()
    var swipeListener: SwipeListener? = null

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        throwExceptionIfNotBuildProperly()
        dragHelper.onAttachedToWindow()
        hideAllButDraggableViews()
        setOnTouchListener(this)
    }

    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
        Log.d("SwipeLayout", "onTouch: ${v}, event=$event")
        event ?: return false
        return dragHelper.onTouchEvent(event)
    }

    override fun onInterceptTouchEvent(event: MotionEvent?): Boolean {
        return dragHelper.isDragAction(event)
    }

    fun reset() {
        backgroundController.onReset()
        dragHelper.onReset()
    }

    fun swipeToLeft() {
        doOnRightSideBGView { show() }
        swipeAnimator.animateToLeft(getDraggableView())
        swipeListener?.swipedToLeft()
    }

    fun swipeToRight() {
        doOnLeftSideBGView { show() }
        swipeAnimator.animateToRight(getDraggableView())
        swipeListener?.swipedToRight()
    }

    internal fun getDraggableView(): View = getChildAt(draggableViewIndex())

    internal fun onMove(touchPointX: Float, currentX: Float) {
        backgroundController.onMove(touchPointX, currentX)
    }

    internal fun showLeftBGView() {
        doOnLeftSideBGView { show() }
    }

    internal fun hideLeftBGView() {
        doOnLeftSideBGView { hide() }
    }

    internal fun showRightBGView() {
        doOnRightSideBGView { show() }
    }

    internal fun hideRightBGView() {
        doOnRightSideBGView { hide() }
    }

    private fun doOnLeftSideBGView(action: View.() -> Unit) {
        val index = getLeftUnderlyingLayoutIndex()
        if (index < 0) return
        getChildAt(index).action()
    }

    private fun doOnRightSideBGView(action: View.() -> Unit) {
        val index = getRighUnderlyingLayoutIndex()
        if (index < 0) return
        getChildAt(index).action()
    }

    private fun getLeftUnderlyingLayoutIndex(): Int {
        return allowedSwipeDirection.getLeftLayoutIndex(childCount)
    }

    private fun getRighUnderlyingLayoutIndex(): Int {
        return allowedSwipeDirection.getRightLayoutIndex(childCount)
    }

    private fun hideAllButDraggableViews() {
        forEachIndexed { index, view ->
            if (index != draggableViewIndex()) view.hide()
        }
    }

    private fun draggableViewIndex() = childCount - 1

    private fun throwExceptionIfNotBuildProperly() {
        if (childCount == 0 || childCount > 3 || childCount == 2 && allowedSwipeDirection is SwipeBothSides) {
            throw LayoutNotBuiltProperlyException()
        }
    }

    interface SwipeListener {
        fun swipedToLeft()
        fun swipedToRight()
    }

    inner class LayoutNotBuiltProperlyException : Exception() {
        override val message: String?
            get() {
                return if (childCount == 2) {
                    "You have to specify swipe direction side explicitly. You can do that using swipeEnabled xml attribute"
                } else {
                    "This layout must have one, two or three children, current children count=$childCount."
                }
            }
    }
}