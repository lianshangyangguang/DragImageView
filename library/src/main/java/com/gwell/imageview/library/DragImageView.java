package com.gwell.imageview.library;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;

import uk.co.senab.photoview.PhotoView;

public class DragImageView extends PhotoView {
    private Paint mPaint;

    // downX
    private float mDownX;
    // down Y
    private float mDownY;

    private float mTranslateY;
    private float mTranslateX;
    private float mScale = 1;
    private int mWidth;
    private int mHeight;
    private float mMinScale = 0f;
    private int mAlpha = 255;
    private final static int MAX_TRANSLATE_Y = 500;

    private final static long DURATION = 300;
    private boolean isAnimate = false;

    //is event on PhotoView
    private boolean isTouchEvent = false;
    private OnTapListener mTapListener;
    private OnExitListener mExitListener;
    private long startTime, endTime;

    public DragImageView(Context context) {
        this(context, null);
    }

    public DragImageView(Context context, AttributeSet attr) {
        this(context, attr, 0);
    }

    public DragImageView(Context context, AttributeSet attr, int defStyle) {
        super(context, attr, defStyle);
        mPaint = new Paint();
        mPaint.setColor(Color.BLACK);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        mPaint.setAlpha(mAlpha);
        canvas.drawRect(0, 0, mWidth, mHeight, mPaint);
        canvas.translate(mTranslateX, mTranslateY);
        canvas.scale(mScale, mScale, mWidth / 2, mHeight / 2);
        super.onDraw(canvas);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        mWidth = w;
        mHeight = h;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        //only scale == 1 can drag
        if (getScale() == 1) {

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    startTime = System.currentTimeMillis();
                    Log.d("zxy", "ACTION_DOWN: ");
                    if (event.getPointerCount() == 2){
                        beforeLenght = getDistance(event);// 获取两点的距离
                    }else{
                        onActionDown(event);
                    }
                    break;
                case MotionEvent.ACTION_MOVE:
                    Log.d("zxy", "ACTION_MOVE: ");
                    if (event.getPointerCount() == 2){
                        Log.d("zxy", "双击 ");
                        onPointerDown(event);
                    }
                    //in viewpager
                    if (mTranslateY == 0 && mTranslateX != 0) {

                        //如果不消费事件，则不作操作
                        if (!isTouchEvent) {
                            mScale = 1;
                            return super.dispatchTouchEvent(event);
                        }
                    }

                    //single finger drag  down
                    if (mTranslateY <= 0 && event.getPointerCount() == 1) {
                        drag(event);
                    } else if (mTranslateY >= 0 && event.getPointerCount() == 1) {
                        onActionMove(event);
                        //如果有上下位移 则不交给viewpager
                        if (mTranslateY != 0) {
                            isTouchEvent = true;
                        }
                        return true;
                    }

                    //防止下拉的时候双手缩放
                    if (mTranslateY >= 0 && mScale < 0.95) {
                        return true;
                    }
                    break;

                case MotionEvent.ACTION_UP:
                    //防止下拉的时候双手缩放
                    endTime = System.currentTimeMillis();
                    if (endTime - startTime < 100 && event.getPointerCount() == 1) {
                        if (mTapListener != null) {
                            mTapListener.onTap(DragImageView.this);
                        }
                    }
                    if (zoomStatus == 0) {
                        Log.d("zxy", "up0: ");
                        onActionUp(event);
                        isTouchEvent = false;
                    } else if (zoomStatus == 1) {
                        Log.d("zxy", "up1: ");
                        zoomStatus = 0;
                        getScaleAnimation().start();
                        final ValueAnimator animator = ValueAnimator.ofFloat(getX(), 0);
                        animator.setDuration(DURATION);
                        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                            @Override
                            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                                mTranslateX = (float) valueAnimator.getAnimatedValue();
                            }
                        });
                        animator.start();
//                            getTranslateXAnimation().setDuration(2000).start();
                        getTranslateYAnimation().start();
                        getAlphaAnimation().start();

                    }
                    zoomStatus = 0;
                    break;

            }
        }

        return super.dispatchTouchEvent(event);
    }

    //拖到上方
    private void drag(MotionEvent event) {
        if (zoomStatus ==1){
            return;
        }
        float moveY = event.getY();
        float moveX = event.getX();
        mTranslateX = moveX - mDownX;
        mTranslateY = moveY - mDownY;
        invalidate();
    }

    private float beforeLenght, afterLenght;// 两触点距离
    private int zoomStatus = 0;

    /**
     * 两个手指 只能放大缩小
     **/
    void onPointerDown(MotionEvent event) {
        if (event.getPointerCount() == 2) {
//            mode = MODE.ZOOM;
            zoomStatus = 1;
            afterLenght = getDistance(event);// 获取两点的距离
            float gapLenght = afterLenght - beforeLenght;// 变化的长度

            if (Math.abs(gapLenght) > 50f) {
                mScale = afterLenght / beforeLenght ;// 求的缩放的比例
                if (mScale <1){
                    mScale -=0.2f;
                }
                Log.d("zxy", "mScale: "+mScale);
                beforeLenght = afterLenght;
                if (mScale < mMinScale) {
                    mScale = mMinScale;
                } else if (mScale > 1f) {
                    mScale = 1;
                }

                invalidate();
            }
        }
    }

    /**
     * 获取两点的距离
     **/
    float getDistance(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);

        return (float) Math.sqrt(x * x + y * y);
    }

    private void onActionUp(MotionEvent event) {

        if (mTranslateY > MAX_TRANSLATE_Y) {
            if (mExitListener != null) {
                mExitListener.onExit(this, mTranslateX, mTranslateY, mWidth, mHeight);
            } else {
                throw new RuntimeException("DragPhotoView: onExitLister can't be null ! call setOnExitListener() ");
            }
        } else {
            performAnimation();
        }
    }

    private void onActionMove(MotionEvent event) {
        if (zoomStatus ==1){
            return;
        }
        Log.d("zxy", "onActionMove1: mTranslateY"+mTranslateY);
        float moveY = event.getY();
        float moveX = event.getX();
        mTranslateX = moveX - mDownX;
        mTranslateY = moveY - mDownY;
        Log.d("zxy", "onActionMove2: mTranslateY"+mTranslateY);

        //保证上划到到顶还可以继续滑动
        if (mTranslateY < 0) {
            mTranslateY = 0;
        }

        float percent = mTranslateY / MAX_TRANSLATE_Y;
        if (mScale >= mMinScale && mScale <= 1f) {
            mScale = 1 - percent;
            mAlpha = (int)(255 * (1 - percent));
            if (mAlpha > 255) {
                mAlpha = 255;
            } else if (mAlpha < 0) {
                mAlpha = 0;
            }
        }
        if (mScale < mMinScale) {
            mScale = mMinScale;
        } else if (mScale > 1f) {
            mScale = 1;
        }

        invalidate();


    }

    private void performAnimation() {
        getScaleAnimation().start();
        getTranslateXAnimation().start();
        getTranslateYAnimation().start();
        getAlphaAnimation().start();
    }

    private ValueAnimator getAlphaAnimation() {
        final ValueAnimator animator = ValueAnimator.ofInt(mAlpha, 255);
        animator.setDuration(DURATION);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                mAlpha = (int) valueAnimator.getAnimatedValue();
            }
        });

        return animator;
    }

    private ValueAnimator getTranslateYAnimation() {
        final ValueAnimator animator = ValueAnimator.ofFloat(mTranslateY, 0);
        animator.setDuration(DURATION);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                mTranslateY = (float) valueAnimator.getAnimatedValue();
            }
        });

        return animator;
    }

    private ValueAnimator getTranslateXAnimation() {
        final ValueAnimator animator = ValueAnimator.ofFloat(mTranslateX, 0);
        animator.setDuration(DURATION);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                mTranslateX = (float) valueAnimator.getAnimatedValue();
            }
        });

        return animator;
    }

    private ValueAnimator getScaleAnimation() {
        final ValueAnimator animator = ValueAnimator.ofFloat(mScale, 1);
        animator.setDuration(DURATION);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                mScale = (float) valueAnimator.getAnimatedValue();
                invalidate();
            }
        });

        animator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {
                isAnimate = true;
            }

            @Override
            public void onAnimationEnd(Animator animator) {
                isAnimate = false;
                animator.removeAllListeners();
            }

            @Override
            public void onAnimationCancel(Animator animator) {

            }

            @Override
            public void onAnimationRepeat(Animator animator) {

            }
        });
        return animator;
    }

    private void onActionDown(MotionEvent event) {
        mDownX = event.getX();
        mDownY = event.getY();
    }

    public float getMinScale() {
        return mMinScale;
    }

    public void setMinScale(float minScale) {
        mMinScale = minScale;
    }

    public void setOnTapListener(OnTapListener listener) {
        mTapListener = listener;
    }

    public void setOnExitListener(OnExitListener listener) {
        mExitListener = listener;
    }

    public interface OnTapListener {
        void onTap(DragImageView view);
    }

    public interface OnExitListener {
        void onExit(DragImageView view, float translateX, float translateY, float w, float h);
    }

    public void finishAnimationCallBack() {
        mTranslateX = -mWidth / 2 + mWidth * mScale / 2;
        mTranslateY = -mHeight / 2 + mHeight * mScale / 2;
        invalidate();
    }

}