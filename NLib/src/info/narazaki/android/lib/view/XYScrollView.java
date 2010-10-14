/*
 * Copyright (C) 2006 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package info.narazaki.android.lib.view;

import info.narazaki.android.lib.system.MigrationSDK4;

import java.util.List;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.FocusFinder;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.Scroller;

public class XYScrollView extends FrameLayout {
    static final int ANIMATED_SCROLL_GAP = 250;
    
    static final float MAX_SCROLL_FACTOR = 0.5f;
    
    private long mLastScroll;
    
    private final Rect mTempRect = new Rect();
    private Scroller mScroller;
    
    /**
     * Flag to indicate that we are moving focus ourselves. This is so the code
     * that watches for focus changes initiated outside this ScrollView knows
     * that it does not have to do anything.
     */
    private boolean mScrollViewMovedFocus;
    
    /**
     * Position of the last motion event.
     */
    private float mLastMotionX;
    private float mLastMotionY;
    
    /**
     * True when the layout has changed but the traversal has not come through
     * yet. Ideally the view hierarchy would keep track of this for us.
     */
    private boolean mIsLayoutDirty = true;
    
    /**
     * The child to give focus to in the event that a child has requested focus
     * while the layout is dirty. This prevents the scroll from being wrong if
     * the child has not been laid out before requesting focus.
     */
    private View mChildToScrollTo = null;
    
    /**
     * True if the user is currently dragging this ScrollView around. This is
     * not the same as 'is being flinged', which can be checked by
     * mScroller.isFinished() (flinging begins when the user lifts his finger).
     */
    private boolean mIsBeingDragged = false;
    
    /**
     * Determines speed during touch scrolling
     */
    private VelocityTracker mVelocityTracker;
    
    /**
     * When set to true, the scroll view measure its child to make it fill the
     * currently visible area.
     */
    private boolean mFillViewport;
    
    /**
     * Whether arrow scrolling is animated.
     */
    private boolean mSmoothScrollingEnabled = true;
    
    private int mTouchSlop;
    private int mMinimumVelocity;
    private int mMaximumVelocity;
    
    public XYScrollView(Context context) {
        this(context, null);
    }
    
    public XYScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initScrollView();
        setFillViewport(false);
    }
    
    public XYScrollView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initScrollView();
        setFillViewport(false);
    }
    
    private void initScrollView() {
        mScroller = new Scroller(getContext());
        setFocusable(true);
        setDescendantFocusability(FOCUS_AFTER_DESCENDANTS);
        setWillNotDraw(false);
        final ViewConfiguration configuration = ViewConfiguration.get(getContext());
        mTouchSlop = configuration.getScaledTouchSlop();
        mMinimumVelocity = configuration.getScaledMinimumFlingVelocity();
        // mMaximumVelocity = configuration.getScaledMaximumFlingVelocity();
        mMaximumVelocity = MigrationSDK4.ViewConfiguration_getScaledMaximumFlingVelocity(configuration);
    }
    
    // ////////////////////////////////////////
    // FadingEdgeStrength
    // ////////////////////////////////////////
    
    @Override
    protected float getTopFadingEdgeStrength() {
        if (getChildCount() == 0) return 0.0f;
        
        final int length = getVerticalFadingEdgeLength();
        if (getScrollY() < length) return getScrollY() / (float) length;
        
        return 1.0f;
    }
    
    @Override
    protected float getBottomFadingEdgeStrength() {
        if (getChildCount() == 0) return 0.0f;
        
        final int length = getVerticalFadingEdgeLength();
        final int bottomEdge = getHeight() - getPaddingBottom();
        final int span = getChildAt(0).getBottom() - getScrollY() - bottomEdge;
        if (span < length) return span / (float) length;
        
        return 1.0f;
    }
    
    @Override
    protected float getLeftFadingEdgeStrength() {
        if (getChildCount() == 0) return 0.0f;
        
        final int length = getHorizontalFadingEdgeLength();
        if (getScrollX() < length) return getScrollX() / (float) length;
        
        return 1.0f;
    }
    
    @Override
    protected float getRightFadingEdgeStrength() {
        if (getChildCount() == 0) return 0.0f;
        
        final int length = getHorizontalFadingEdgeLength();
        final int rightEdge = getWidth() - getPaddingRight();
        final int span = getChildAt(0).getRight() - getScrollX() - rightEdge;
        if (span < length) return span / (float) length;
        
        return 1.0f;
    }
    
    private boolean canScrollX() {
        View child = getChildAt(0);
        if (child != null) {
            int childWidth = child.getWidth();
            return getWidth() < childWidth + getPaddingLeft() + getPaddingRight();
        }
        return false;
    }
    
    private boolean canScrollY() {
        View child = getChildAt(0);
        if (child != null) {
            int childHeight = child.getHeight();
            return getHeight() < childHeight + getPaddingTop() + getPaddingBottom();
        }
        return false;
    }
    
    @Override
    public void addView(View child) {
        if (getChildCount() > 0) {
            throw new IllegalStateException(this.getClass().getName() + " can host only one direct child");
        }
        
        super.addView(child);
    }
    
    @Override
    public void addView(View child, int index) {
        if (getChildCount() > 0) {
            throw new IllegalStateException(this.getClass().getName() + " can host only one direct child");
        }
        
        super.addView(child, index);
    }
    
    @Override
    public void addView(View child, ViewGroup.LayoutParams params) {
        if (getChildCount() > 0) {
            throw new IllegalStateException(this.getClass().getName() + " can host only one direct child");
        }
        
        super.addView(child, params);
    }
    
    @Override
    public void addView(View child, int index, ViewGroup.LayoutParams params) {
        if (getChildCount() > 0) {
            throw new IllegalStateException(this.getClass().getName() + " can host only one direct child");
        }
        
        super.addView(child, index, params);
    }
    
    /**
     * Indicates whether this ScrollView's content is stretched to fill the
     * viewport.
     * 
     * @return True if the content fills the viewport, false otherwise.
     */
    public boolean isFillViewport() {
        return mFillViewport;
    }
    
    /**
     * Indicates this ScrollView whether it should stretch its content width to
     * fill the viewport or not.
     * 
     * @param fillViewport
     *            True to stretch the content's width to the viewport's
     *            boundaries, false otherwise.
     */
    public void setFillViewport(boolean fillViewport) {
        if (fillViewport != mFillViewport) {
            mFillViewport = fillViewport;
            requestLayout();
        }
    }
    
    /**
     * @return Whether arrow scrolling will animate its transition.
     */
    public boolean isSmoothScrollingEnabled() {
        return mSmoothScrollingEnabled;
    }
    
    /**
     * Set whether arrow scrolling will animate its transition.
     * 
     * @param smoothScrollingEnabled
     *            whether arrow scrolling will animate its transition
     */
    public void setSmoothScrollingEnabled(boolean smoothScrollingEnabled) {
        mSmoothScrollingEnabled = smoothScrollingEnabled;
    }
    
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        
        if (!mFillViewport) {
            return;
        }
        
        final int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        if (widthMode == MeasureSpec.UNSPECIFIED) {
            return;
        }
        
        if (getChildCount() > 0) {
            final View child = getChildAt(0);
            int width = getMeasuredWidth();
            int height = getMeasuredHeight();
            
            int childWidthMeasureSpec;
            int childHeightMeasureSpec;
            
            boolean is_measured = false;
            
            if (child.getMeasuredWidth() < width) {
                final FrameLayout.LayoutParams lp = (LayoutParams) child.getLayoutParams();
                
                childHeightMeasureSpec = getChildMeasureSpec(heightMeasureSpec, getPaddingTop() + getPaddingBottom(),
                        lp.height);
                width -= getPaddingLeft();
                width -= getPaddingRight();
                
                is_measured = true;
            }
            else {
                childHeightMeasureSpec = MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY);
            }
            
            if (child.getMeasuredHeight() < height) {
                final FrameLayout.LayoutParams lp = (LayoutParams) child.getLayoutParams();
                
                childWidthMeasureSpec = getChildMeasureSpec(widthMeasureSpec, getPaddingLeft() + getPaddingRight(),
                        lp.width);
                height -= getPaddingTop();
                height -= getPaddingBottom();
                
                is_measured = true;
            }
            else {
                childWidthMeasureSpec = MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY);
            }
            
            if (is_measured) child.measure(childWidthMeasureSpec, childHeightMeasureSpec);
        }
    }
    
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        // Let the focused view and/or our descendants get the key first
        return super.dispatchKeyEvent(event) || executeKeyEvent(event);
    }
    
    /**
     * You can call this function yourself to have the scroll view perform
     * scrolling from a key event, just as if the event had been dispatched to
     * it by the view hierarchy.
     * 
     * @param event
     *            The key event to execute.
     * @return Return true if the event was handled, else false.
     */
    public boolean executeKeyEvent(KeyEvent event) {
        mTempRect.setEmpty();
        
        if (!canScrollX() && !canScrollY()) return false;
        
        boolean handled = false;
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            switch (event.getKeyCode()) {
            case KeyEvent.KEYCODE_DPAD_LEFT:
                if (!event.isAltPressed()) {
                    handled = arrowScroll(View.FOCUS_LEFT);
                }
                else {
                    handled = fullScroll(View.FOCUS_LEFT);
                }
                break;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                if (!event.isAltPressed()) {
                    handled = arrowScroll(View.FOCUS_RIGHT);
                }
                else {
                    handled = fullScroll(View.FOCUS_RIGHT);
                }
                break;
            case KeyEvent.KEYCODE_DPAD_UP:
                if (!event.isAltPressed()) {
                    handled = arrowScroll(View.FOCUS_UP);
                }
                else {
                    handled = fullScroll(View.FOCUS_UP);
                }
                break;
            case KeyEvent.KEYCODE_DPAD_DOWN:
                if (!event.isAltPressed()) {
                    handled = arrowScroll(View.FOCUS_DOWN);
                }
                else {
                    handled = fullScroll(View.FOCUS_DOWN);
                }
                break;
            case KeyEvent.KEYCODE_SPACE:
                pageScroll(event.isShiftPressed() ? View.FOCUS_UP : View.FOCUS_DOWN);
                break;
            }
        }
        
        return handled;
    }
    
    private boolean inChild(int x, int y) {
        if (getChildCount() > 0) {
            final int scrollX = getScrollX();
            final int scrollY = getScrollY();
            final View child = getChildAt(0);
            return !(y < child.getTop() - scrollY || y >= child.getBottom() - scrollY || x < child.getLeft() - scrollX || x >= child
                    .getRight() - scrollX);
        }
        return false;
    }
    
    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        /*
         * This method JUST determines whether we want to intercept the motion.
         * If we return true, onMotionEvent will be called and we do the actual
         * scrolling there.
         */

        /*
         * Shortcut the most recurring case: the user is in the dragging state
         * and he is moving his finger. We want to intercept this motion.
         */
        final int action = ev.getAction();
        if ((action == MotionEvent.ACTION_MOVE) && (mIsBeingDragged)) {
            return true;
        }
        
        switch (action) {
        case MotionEvent.ACTION_MOVE: {
            /*
             * mIsBeingDragged == false, otherwise the shortcut would have
             * caught it. Check whether the user has moved far enough from his
             * original down touch.
             */

            // TODO : multi touch not supported
            final float y = ev.getY();
            final int yDiff = (int) Math.abs(y - mLastMotionY);
            if (yDiff > mTouchSlop) {
                mIsBeingDragged = true;
                mLastMotionY = y;
            }
            final float x = ev.getX();
            final int xDiff = (int) Math.abs(x - mLastMotionX);
            if (xDiff > mTouchSlop) {
                mIsBeingDragged = true;
                mLastMotionX = x;
            }
            break;
        }
            
        case MotionEvent.ACTION_DOWN: {
            final float x = ev.getX();
            final float y = ev.getY();
            if (!inChild((int) x, (int) y)) {
                mIsBeingDragged = false;
                break;
            }
            
            mLastMotionX = x;
            mLastMotionY = y;
            
            /*
             * If being flinged and user touches the screen, initiate drag;
             * otherwise don't. mScroller.isFinished should be false when being
             * flinged.
             */
            mIsBeingDragged = !mScroller.isFinished();
            break;
        }
            
        case MotionEvent.ACTION_CANCEL:
        case MotionEvent.ACTION_UP:
            /* Release the drag */
            mIsBeingDragged = false;
            break;
        }
        
        /*
         * The only time we want to intercept motion events is if we are in the
         * drag mode.
         */
        return mIsBeingDragged;
    }
    
    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        
        // TODO : multi touch not supported
        
        if (ev.getAction() == MotionEvent.ACTION_DOWN && ev.getEdgeFlags() != 0) {
            // Don't handle edge touches immediately -- they may actually belong
            // to one of our
            // descendants.
            return false;
        }
        
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
        mVelocityTracker.addMovement(ev);
        
        final int action = ev.getAction();
        
        switch (action) {
        case MotionEvent.ACTION_DOWN: {
            final float x = ev.getX();
            final float y = ev.getY();
            if (!(mIsBeingDragged = inChild((int) x, (int) y))) {
                return false;
            }
            
            /*
             * If being flinged and user touches, stop the fling. isFinished
             * will be false if being flinged.
             */
            if (!mScroller.isFinished()) {
                mScroller.abortAnimation();
            }
            
            // Remember where the motion event started
            mLastMotionX = x;
            mLastMotionY = y;
            break;
        }
        case MotionEvent.ACTION_MOVE:
            if (mIsBeingDragged) {
                // Scroll to follow the motion event
                final float x = ev.getX();
                final float y = ev.getY();
                final int deltaX = (int) (mLastMotionX - x);
                final int deltaY = (int) (mLastMotionY - y);
                mLastMotionX = x;
                mLastMotionY = y;
                
                scrollBy(deltaX, deltaY);
            }
            break;
        case MotionEvent.ACTION_UP:
            if (mIsBeingDragged) {
                final VelocityTracker velocityTracker = mVelocityTracker;
                // velocityTracker.computeCurrentVelocity(1000,
                // mMaximumVelocity);
                MigrationSDK4.VelocityTracker_computeCurrentVelocity(velocityTracker, 1000, mMaximumVelocity);
                int initialVelocityX = (int) velocityTracker.getXVelocity();
                int initialVelocityY = (int) velocityTracker.getYVelocity();
                
                if (getChildCount() > 0
                        && (Math.abs(initialVelocityX) > mMinimumVelocity || Math.abs(initialVelocityY) > mMinimumVelocity)) {
                    fling(-initialVelocityX, -initialVelocityY);
                }
                
                mIsBeingDragged = false;
                
                if (mVelocityTracker != null) {
                    mVelocityTracker.recycle();
                    mVelocityTracker = null;
                }
            }
            break;
        case MotionEvent.ACTION_CANCEL:
            if (mIsBeingDragged && getChildCount() > 0) {
                mIsBeingDragged = false;
                if (mVelocityTracker != null) {
                    mVelocityTracker.recycle();
                    mVelocityTracker = null;
                }
            }
            break;
        }
        return true;
    }
    
    private View findFocusableViewInMyBounds(final boolean leftFocus, final int left, final boolean topFocus,
            final int top, View preferredFocusable) {
        /*
         * The fading edge's transparent side should be considered for focus
         * since it's mostly visible, so we divide the actual fading edge length
         * by 2.
         */
        final int horizontalFadingEdgeLength = getHorizontalFadingEdgeLength() / 2;
        final int leftWithoutFadingEdge = left + horizontalFadingEdgeLength;
        final int rightWithoutFadingEdge = left + getWidth() - horizontalFadingEdgeLength;
        
        final int verticalFadingEdgeLength = getVerticalFadingEdgeLength() / 2;
        final int topWithoutFadingEdge = top + verticalFadingEdgeLength;
        final int bottomWithoutFadingEdge = top + getHeight() - verticalFadingEdgeLength;
        
        if ((preferredFocusable != null) && (preferredFocusable.getLeft() < rightWithoutFadingEdge)
                && (preferredFocusable.getRight() > leftWithoutFadingEdge)
                && (preferredFocusable.getTop() < bottomWithoutFadingEdge)
                && (preferredFocusable.getBottom() > topWithoutFadingEdge)) {
            return preferredFocusable;
        }
        
        return findFocusableViewInBounds(leftFocus, leftWithoutFadingEdge, rightWithoutFadingEdge, topFocus,
                topWithoutFadingEdge, bottomWithoutFadingEdge);
    }
    
    private View findFocusableViewInBounds(boolean leftFocus, int left, int right, boolean topFocus, int top, int bottom) {
        List<View> focusables = getFocusables(View.FOCUS_FORWARD);
        View focusCandidate = null;
        
        /*
         * A fully contained focusable is one where its top is below the bound's
         * top, and its bottom is above the bound's bottom. A partially
         * contained focusable is one where some part of it is within the
         * bounds, but it also has some part that is not within bounds. A fully
         * contained focusable is preferred to a partially contained focusable.
         */
        boolean foundFullyContainedFocusable = false;
        
        int count = focusables.size();
        for (int i = 0; i < count; i++) {
            View view = focusables.get(i);
            int viewLeft = view.getLeft();
            int viewRight = view.getRight();
            int viewTop = view.getTop();
            int viewBottom = view.getBottom();
            
            if (left < viewRight && viewLeft < right && top < viewBottom && viewTop < bottom) {
                /*
                 * the focusable is in the target area, it is a candidate for
                 * focusing
                 */

                final boolean viewIsFullyContained = (left < viewLeft) && (viewRight < right) && (top < viewTop)
                        && (viewBottom < bottom);
                
                if (focusCandidate == null) {
                    /* No candidate, take this one */
                    focusCandidate = view;
                    foundFullyContainedFocusable = viewIsFullyContained;
                }
                else {
                    final boolean viewIsCloserToBoundary = (leftFocus && viewLeft < focusCandidate.getLeft())
                            || (!leftFocus && viewRight > focusCandidate.getRight())
                            || (topFocus && viewTop < focusCandidate.getTop())
                            || (!topFocus && viewBottom > focusCandidate.getBottom());
                    
                    if (foundFullyContainedFocusable) {
                        if (viewIsFullyContained && viewIsCloserToBoundary) {
                            /*
                             * We're dealing with only fully contained views, so
                             * it has to be closer to the boundary to beat our
                             * candidate
                             */
                            focusCandidate = view;
                        }
                    }
                    else {
                        if (viewIsFullyContained) {
                            /*
                             * Any fully contained view beats a partially
                             * contained view
                             */
                            focusCandidate = view;
                            foundFullyContainedFocusable = true;
                        }
                        else if (viewIsCloserToBoundary) {
                            /*
                             * Partially contained view beats another partially
                             * contained view if it's closer
                             */
                            focusCandidate = view;
                        }
                    }
                }
            }
        }
        
        return focusCandidate;
    }
    
    public boolean pageScroll(int direction) {
        boolean right = direction == View.FOCUS_RIGHT;
        boolean down = direction == View.FOCUS_DOWN;
        int width = getWidth();
        int height = getHeight();
        
        if (right) {
            mTempRect.left = getScrollX() + width;
            int count = getChildCount();
            if (count > 0) {
                View view = getChildAt(0);
                if (mTempRect.left + width > view.getRight()) {
                    mTempRect.left = view.getRight() - width;
                }
            }
        }
        else {
            mTempRect.left = getScrollX() - width;
            if (mTempRect.left < 0) {
                mTempRect.left = 0;
            }
        }
        if (down) {
            mTempRect.top = getScrollY() + height;
            int count = getChildCount();
            if (count > 0) {
                View view = getChildAt(count - 1);
                if (mTempRect.top + height > view.getBottom()) {
                    mTempRect.top = view.getBottom() - height;
                }
            }
        }
        else {
            mTempRect.top = getScrollY() - height;
            if (mTempRect.top < 0) {
                mTempRect.top = 0;
            }
        }
        mTempRect.right = mTempRect.left + width;
        mTempRect.bottom = mTempRect.top + height;
        
        return scrollAndFocus(direction, mTempRect.left, mTempRect.right, mTempRect.top, mTempRect.bottom);
    }
    
    public boolean fullScroll(int direction) {
        boolean right = direction == View.FOCUS_RIGHT;
        boolean down = direction == View.FOCUS_DOWN;
        int width = getWidth();
        int height = getHeight();
        
        mTempRect.left = 0;
        mTempRect.right = width;
        mTempRect.top = 0;
        mTempRect.bottom = height;
        
        if (right) {
            int count = getChildCount();
            if (count > 0) {
                View view = getChildAt(0);
                mTempRect.right = view.getRight();
                mTempRect.left = mTempRect.right - width;
            }
        }
        if (down) {
            int count = getChildCount();
            if (count > 0) {
                View view = getChildAt(count - 1);
                mTempRect.bottom = view.getBottom();
                mTempRect.top = mTempRect.bottom - height;
            }
        }
        
        return scrollAndFocus(direction, mTempRect.left, mTempRect.right, mTempRect.top, mTempRect.bottom);
    }
    
    private boolean scrollAndFocus(int direction, int left, int right, int top, int bottom) {
        boolean handledX = true;
        boolean handledY = true;
        
        int width = getWidth();
        int containerLeft = getScrollX();
        int containerRight = containerLeft + width;
        boolean goLeft = direction == View.FOCUS_LEFT;
        
        int height = getHeight();
        int containerTop = getScrollY();
        int containerBottom = containerTop + height;
        boolean goUp = direction == View.FOCUS_UP;
        
        View newFocused = findFocusableViewInBounds(goLeft, left, right, goUp, top, bottom);
        if (newFocused == null) {
            newFocused = this;
        }
        
        int deltaX = 0;
        int deltaY = 0;
        if (left >= containerLeft && right <= containerRight) {
            handledX = false;
        }
        else {
            deltaX = goLeft ? (left - containerLeft) : (right - containerRight);
        }
        if (top >= containerTop && bottom <= containerBottom) {
            handledY = false;
        }
        else {
            deltaY = goUp ? (top - containerTop) : (bottom - containerBottom);
        }
        if (handledX || handledY) doScrollXY(deltaX, deltaY);
        
        if (newFocused != findFocus() && newFocused.requestFocus(direction)) {
            mScrollViewMovedFocus = true;
            mScrollViewMovedFocus = false;
        }
        
        return handledX || handledY;
    }
    
    public int getMaxScrollAmountX() {
        return (int) (MAX_SCROLL_FACTOR * (getRight() - getLeft()));
    }
    
    public int getMaxScrollAmountY() {
        return (int) (MAX_SCROLL_FACTOR * (getBottom() - getTop()));
    }
    
    /**
     * Handle scrolling in response to a left or right arrow click.
     * 
     * @param direction
     *            The direction corresponding to the arrow key that was pressed
     * @return True if we consumed the event, false otherwise
     */
    public boolean arrowScroll(int direction) {
        
        View currentFocused = findFocus();
        if (currentFocused == this) currentFocused = null;
        
        View nextFocused = FocusFinder.getInstance().findNextFocus(this, currentFocused, direction);
        
        final int maxJumpX = getMaxScrollAmountX();
        final int maxJumpY = getMaxScrollAmountY();
        
        if (nextFocused != null && isWithinDeltaOfScreen(nextFocused, maxJumpX, getWidth(), maxJumpY, getHeight())) {
            nextFocused.getDrawingRect(mTempRect);
            offsetDescendantRectToMyCoords(nextFocused, mTempRect);
            int scrollDeltaX = computeScrollDeltaToGetChildRectOnScreenX(mTempRect);
            int scrollDeltaY = computeScrollDeltaToGetChildRectOnScreenY(mTempRect);
            doScrollXY(scrollDeltaX, scrollDeltaY);
            nextFocused.requestFocus(direction);
        }
        else {
            // no new focus
            int scrollDeltaX = maxJumpX;
            int scrollDeltaY = maxJumpY;
            
            if (direction == View.FOCUS_LEFT && getScrollX() < scrollDeltaX) {
                scrollDeltaX = getScrollX();
            }
            else if (direction == View.FOCUS_RIGHT && getChildCount() > 0) {
                
                int daRight = getChildAt(0).getRight();
                
                int screenRight = getScrollX() + getWidth();
                
                if (daRight - screenRight < maxJumpX) {
                    scrollDeltaX = daRight - screenRight;
                }
            }
            if (direction == View.FOCUS_UP && getScrollY() < scrollDeltaY) {
                scrollDeltaY = getScrollY();
            }
            else if (direction == View.FOCUS_DOWN) {
                if (getChildCount() > 0) {
                    int daBottom = getChildAt(0).getBottom();
                    
                    int screenBottom = getScrollY() + getHeight();
                    
                    if (daBottom - screenBottom < maxJumpY) {
                        scrollDeltaY = daBottom - screenBottom;
                    }
                }
            }
            if (scrollDeltaX == 0 && scrollDeltaY == 0) {
                return false;
            }
            doScrollXY(direction == View.FOCUS_RIGHT ? scrollDeltaX : -scrollDeltaX,
                    direction == View.FOCUS_DOWN ? scrollDeltaY : -scrollDeltaY);
        }
        
        if (currentFocused != null && currentFocused.isFocused() && isOffScreen(currentFocused)) {
            final int descendantFocusability = getDescendantFocusability(); // save
            setDescendantFocusability(ViewGroup.FOCUS_BEFORE_DESCENDANTS);
            requestFocus();
            setDescendantFocusability(descendantFocusability); // restore
        }
        return true;
    }
    
    private boolean isOffScreen(View descendant) {
        return !isWithinDeltaOfScreen(descendant, 0, getWidth(), 0, getHeight());
    }
    
    private boolean isWithinDeltaOfScreen(View descendant, int deltaX, int width, int deltaY, int height) {
        descendant.getDrawingRect(mTempRect);
        offsetDescendantRectToMyCoords(descendant, mTempRect);
        
        return ((mTempRect.bottom + deltaY) >= getScrollY() && (mTempRect.top - deltaY) <= (getScrollY() + height))
                && ((mTempRect.right + deltaX) >= getScrollX() && (mTempRect.left - deltaX) <= (getScrollX() + getWidth()));
    }
    
    private void doScrollXY(int deltaX, int deltaY) {
        if (deltaX != 0 || deltaY != 0) {
            if (mSmoothScrollingEnabled) {
                smoothScrollBy(deltaX, deltaY);
            }
            else {
                scrollBy(deltaX, deltaY);
            }
        }
    }
    
    public final void smoothScrollBy(int dx, int dy) {
        if (getChildCount() == 0) {
            // Nothing to do.
            return;
        }
        long duration = AnimationUtils.currentAnimationTimeMillis() - mLastScroll;
        if (duration > ANIMATED_SCROLL_GAP) {
            final int width = getWidth() - getPaddingRight() - getPaddingLeft();
            final int right = getChildAt(0).getWidth();
            final int maxX = Math.max(0, right - width);
            final int scrollX = getScrollX();
            dx = Math.max(0, Math.min(scrollX + dx, maxX)) - scrollX;
            
            final int height = getHeight() - getPaddingBottom() - getPaddingTop();
            final int bottom = getChildAt(0).getHeight();
            final int maxY = Math.max(0, bottom - height);
            final int scrollY = getScrollY();
            dy = Math.max(0, Math.min(scrollY + dy, maxY)) - scrollY;
            
            mScroller.startScroll(scrollX, scrollY, dx, dy);
            invalidate();
        }
        else {
            if (!mScroller.isFinished()) {
                mScroller.abortAnimation();
            }
            scrollBy(dx, dy);
        }
        mLastScroll = AnimationUtils.currentAnimationTimeMillis();
    }
    
    public final void smoothScrollTo(int x, int y) {
        smoothScrollBy(x - getScrollX(), y - getScrollY());
    }
    
    @Override
    protected int computeVerticalScrollRange() {
        final int count = getChildCount();
        final int contentHeight = getHeight() - getPaddingBottom() - getPaddingTop();
        if (count == 0) {
            return contentHeight;
        }
        
        return getChildAt(0).getBottom();
    }
    
    @Override
    protected int computeVerticalScrollOffset() {
        return Math.max(0, super.computeVerticalScrollOffset());
    }
    
    @Override
    protected int computeHorizontalScrollRange() {
        final int count = getChildCount();
        final int contentWidth = getWidth() - getPaddingLeft() - getPaddingRight();
        if (count == 0) {
            return contentWidth;
        }
        
        return getChildAt(0).getRight();
    }
    
    @Override
    protected int computeHorizontalScrollOffset() {
        return Math.max(0, super.computeHorizontalScrollOffset());
    }
    
    @Override
    protected void measureChild(View child, int parentWidthMeasureSpec, int parentHeightMeasureSpec) {
        int childWidthMeasureSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
        int childHeightMeasureSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
        
        child.measure(childWidthMeasureSpec, childHeightMeasureSpec);
    }
    
    @Override
    protected void measureChildWithMargins(View child, int parentWidthMeasureSpec, int widthUsed,
            int parentHeightMeasureSpec, int heightUsed) {
        final MarginLayoutParams lp = (MarginLayoutParams) child.getLayoutParams();
        
        final int childHeightMeasureSpec = MeasureSpec.makeMeasureSpec(lp.topMargin + lp.bottomMargin,
                MeasureSpec.UNSPECIFIED);
        final int childWidthMeasureSpec = MeasureSpec.makeMeasureSpec(lp.leftMargin + lp.rightMargin,
                MeasureSpec.UNSPECIFIED);
        
        child.measure(childWidthMeasureSpec, childHeightMeasureSpec);
    }
    
    @Override
    public void computeScroll() {
        if (mScroller.computeScrollOffset()) {
            int x = mScroller.getCurrX();
            int y = mScroller.getCurrY();
            scrollTo(x, y);
        }
    }
    
    /**
     * Scrolls the view to the given child.
     * 
     * @param child
     *            the View to scroll to
     */
    private void scrollToChild(View child) {
        child.getDrawingRect(mTempRect);
        
        /* Offset from child's local coordinates to ScrollView coordinates */
        offsetDescendantRectToMyCoords(child, mTempRect);
        
        int scrollDeltaX = computeScrollDeltaToGetChildRectOnScreenX(mTempRect);
        int scrollDeltaY = computeScrollDeltaToGetChildRectOnScreenY(mTempRect);
        
        if (scrollDeltaX != 0 || scrollDeltaY != 0) {
            scrollBy(scrollDeltaX, scrollDeltaY);
        }
    }
    
    /**
     * If rect is off screen, scroll just enough to get it (or at least the
     * first screen size chunk of it) on screen.
     * 
     * @param rect
     *            The rectangle.
     * @param immediate
     *            True to scroll immediately without animation
     * @return true if scrolling was performed
     */
    private boolean scrollToChildRect(Rect rect, boolean immediate) {
        final int deltaX = computeScrollDeltaToGetChildRectOnScreenX(rect);
        final int deltaY = computeScrollDeltaToGetChildRectOnScreenY(rect);
        final boolean scroll = (deltaX != 0 || deltaY != 0);
        if (scroll) {
            if (immediate) {
                scrollBy(deltaX, deltaY);
            }
            else {
                smoothScrollBy(deltaX, deltaY);
            }
        }
        return scroll;
    }
    
    private int computeScrollDeltaToGetChildRectOnScreenX(Rect rect) {
        if (getChildCount() == 0) return 0;
        
        int width = getWidth();
        int screenLeft = getScrollX();
        int screenRight = screenLeft + width;
        
        int fadingEdge = getHorizontalFadingEdgeLength();
        
        // leave room for left fading edge as long as rect isn't at very left
        if (rect.left > 0) {
            screenLeft += fadingEdge;
        }
        
        // leave room for right fading edge as long as rect isn't at very right
        if (rect.right < getChildAt(0).getWidth()) {
            screenRight -= fadingEdge;
        }
        
        int scrollXDelta = 0;
        
        if (rect.right > screenRight && rect.left > screenLeft) {
            // need to move right to get it in view: move right just enough so
            // that the entire rectangle is in view (or at least the first
            // screen size chunk).
            
            if (rect.width() > width) {
                // just enough to get screen size chunk on
                scrollXDelta += (rect.left - screenLeft);
            }
            else {
                // get entire rect at right of screen
                scrollXDelta += (rect.right - screenRight);
            }
            
            // make sure we aren't scrolling beyond the end of our content
            int right = getChildAt(0).getRight();
            int distanceToRight = right - screenRight;
            scrollXDelta = Math.min(scrollXDelta, distanceToRight);
            
        }
        else if (rect.left < screenLeft && rect.right < screenRight) {
            // need to move right to get it in view: move right just enough so
            // that
            // entire rectangle is in view (or at least the first screen
            // size chunk of it).
            
            if (rect.width() > width) {
                // screen size chunk
                scrollXDelta -= (screenRight - rect.right);
            }
            else {
                // entire rect at left
                scrollXDelta -= (screenLeft - rect.left);
            }
            
            // make sure we aren't scrolling any further than the left our
            // content
            scrollXDelta = Math.max(scrollXDelta, -getScrollX());
        }
        return scrollXDelta;
    }
    
    private int computeScrollDeltaToGetChildRectOnScreenY(Rect rect) {
        if (getChildCount() == 0) return 0;
        
        int height = getHeight();
        int screenTop = getScrollY();
        int screenBottom = screenTop + height;
        
        int fadingEdge = getVerticalFadingEdgeLength();
        
        // leave room for top fading edge as long as rect isn't at very top
        if (rect.top > 0) {
            screenTop += fadingEdge;
        }
        
        // leave room for bottom fading edge as long as rect isn't at very
        // bottom
        if (rect.bottom < getChildAt(0).getHeight()) {
            screenBottom -= fadingEdge;
        }
        
        int scrollYDelta = 0;
        
        if (rect.bottom > screenBottom && rect.top > screenTop) {
            // need to move down to get it in view: move down just enough so
            // that the entire rectangle is in view (or at least the first
            // screen size chunk).
            
            if (rect.height() > height) {
                // just enough to get screen size chunk on
                scrollYDelta += (rect.top - screenTop);
            }
            else {
                // get entire rect at bottom of screen
                scrollYDelta += (rect.bottom - screenBottom);
            }
            
            // make sure we aren't scrolling beyond the end of our content
            int bottom = getChildAt(0).getBottom();
            int distanceToBottom = bottom - screenBottom;
            scrollYDelta = Math.min(scrollYDelta, distanceToBottom);
            
        }
        else if (rect.top < screenTop && rect.bottom < screenBottom) {
            // need to move up to get it in view: move up just enough so that
            // entire rectangle is in view (or at least the first screen
            // size chunk of it).
            
            if (rect.height() > height) {
                // screen size chunk
                scrollYDelta -= (screenBottom - rect.bottom);
            }
            else {
                // entire rect at top
                scrollYDelta -= (screenTop - rect.top);
            }
            
            // make sure we aren't scrolling any further than the top our
            // content
            scrollYDelta = Math.max(scrollYDelta, -getScrollY());
        }
        return scrollYDelta;
    }
    
    @Override
    public void requestChildFocus(View child, View focused) {
        if (!mScrollViewMovedFocus) {
            if (!mIsLayoutDirty) {
                scrollToChild(focused);
            }
            else {
                // The child may not be laid out yet, we can't compute the
                // scroll yet
                mChildToScrollTo = focused;
            }
        }
        super.requestChildFocus(child, focused);
    }
    
    /**
     * When looking for focus in children of a scroll view, need to be a little
     * more careful not to give focus to something that is scrolled off screen.
     * 
     * This is more expensive than the default {@link android.view.ViewGroup}
     * implementation, otherwise this behavior might have been made the default.
     */
    @Override
    protected boolean onRequestFocusInDescendants(int direction, Rect previouslyFocusedRect) {
        
        // convert from forward / backward notation to up / down / left / right
        // (ugh).
        if (direction == View.FOCUS_FORWARD) {
            direction = View.FOCUS_DOWN;
        }
        else if (direction == View.FOCUS_BACKWARD) {
            direction = View.FOCUS_UP;
        }
        
        final View nextFocus = previouslyFocusedRect == null ? FocusFinder.getInstance().findNextFocus(this, null,
                direction) : FocusFinder.getInstance().findNextFocusFromRect(this, previouslyFocusedRect, direction);
        
        if (nextFocus == null) {
            return false;
        }
        
        if (isOffScreen(nextFocus)) {
            return false;
        }
        
        return nextFocus.requestFocus(direction, previouslyFocusedRect);
    }
    
    @Override
    public boolean requestChildRectangleOnScreen(View child, Rect rectangle, boolean immediate) {
        // offset into coordinate space of this scroll view
        rectangle.offset(child.getLeft() - child.getScrollX(), child.getTop() - child.getScrollY());
        
        return scrollToChildRect(rectangle, immediate);
    }
    
    @Override
    public void requestLayout() {
        mIsLayoutDirty = true;
        super.requestLayout();
    }
    
    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        mIsLayoutDirty = false;
        // Give a child focus if it needs it
        if (mChildToScrollTo != null && isViewDescendantOf(mChildToScrollTo, this)) {
            scrollToChild(mChildToScrollTo);
        }
        mChildToScrollTo = null;
        
        // Calling this with the present values causes it to re-clam them
        scrollTo(getScrollX(), getScrollY());
    }
    
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        
        View currentFocused = findFocus();
        if (null == currentFocused || this == currentFocused) return;
        
        final int maxJumpX = getRight() - getLeft();
        
        if (isWithinDeltaOfScreen(currentFocused, maxJumpX, getWidth(), 0, oldh)) {
            currentFocused.getDrawingRect(mTempRect);
            offsetDescendantRectToMyCoords(currentFocused, mTempRect);
            int scrollDeltaX = computeScrollDeltaToGetChildRectOnScreenX(mTempRect);
            int scrollDeltaY = computeScrollDeltaToGetChildRectOnScreenY(mTempRect);
            doScrollXY(scrollDeltaX, scrollDeltaY);
        }
    }
    
    private boolean isViewDescendantOf(View child, View parent) {
        if (child == parent) {
            return true;
        }
        
        final ViewParent theParent = child.getParent();
        return (theParent instanceof ViewGroup) && isViewDescendantOf((View) theParent, parent);
    }
    
    public void fling(int velocityX, int velocityY) {
        if (getChildCount() > 0) {
            int width = getWidth() - getPaddingRight() - getPaddingLeft();
            int right = getChildAt(0).getWidth();
            int height = getHeight() - getPaddingBottom() - getPaddingTop();
            int bottom = getChildAt(0).getHeight();
            
            mScroller.fling(getScrollX(), getScrollY(), velocityX, velocityY, 0, Math.max(0, right - width), 0,
                    Math.max(0, bottom - height));
            
            final boolean movingRight = velocityX > 0;
            final boolean movingDown = velocityY > 0;
            
            View newFocused = findFocusableViewInMyBounds(movingRight, mScroller.getFinalX(), movingDown,
                    mScroller.getFinalY(), findFocus());
            if (newFocused == null) {
                newFocused = this;
            }
            
            int direction = velocityY != 0 ? (movingDown ? View.FOCUS_DOWN : View.FOCUS_UP)
                    : (movingRight ? View.FOCUS_RIGHT : View.FOCUS_LEFT);
            
            if (newFocused != findFocus() && newFocused.requestFocus(direction)) {
                mScrollViewMovedFocus = true;
                mScrollViewMovedFocus = false;
            }
            
            invalidate();
        }
        
    }
    
    @Override
    public void scrollTo(int x, int y) {
        // we rely on the fact the View.scrollBy calls scrollTo.
        if (getChildCount() > 0) {
            View child = getChildAt(0);
            x = clamp(x, getWidth() - getPaddingRight() - getPaddingLeft(), child.getWidth());
            y = clamp(y, getHeight() - getPaddingBottom() - getPaddingTop(), child.getHeight());
            if (x != getScrollX() || y != getScrollY()) {
                super.scrollTo(x, y);
            }
        }
    }
    
    private int clamp(int n, int my, int child) {
        if (my >= child || n < 0) {
            return 0;
        }
        if ((my + n) > child) {
            return child - my;
        }
        return n;
    }
}
