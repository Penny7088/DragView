package com.penny.root.dragdemo.ui;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.ViewDragHelper;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;

import com.nineoldandroids.view.ViewHelper;

/**
 * Created by penny on 14-9-19.
 */
public class DragLayout extends FrameLayout {

    private ViewDragHelper mHelper; // 拖拽辅助类
    private View mLeftContent; // 左面板
    private View mMainContent; // 主面板
    private int mHeight;// 控件高度
    private int mWidth; // 控件宽度
    private int mRange; // 拖拽范围

    private Status status = Status.Close;// 默认状态
    private OnStatusUpdateListener onStatusUpdateListener; // 监听

    public static enum Status {
        Close, Open, Draging
    }

    // 状态更新监听
    public interface OnStatusUpdateListener {

        // 关闭
        void onClose();

        // 开启
        void onOpen();

        // 拖拽过程中
        void onDraging(float percent);
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public OnStatusUpdateListener getOnStatusUpdateListener() {
        return onStatusUpdateListener;
    }

    public void setOnStatusUpdateListener(
            OnStatusUpdateListener onStatusUpdateListener) {
        this.onStatusUpdateListener = onStatusUpdateListener;
    }

    // new
    public DragLayout(Context context) {
        this(context, null);
    }

    // xml
    public DragLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    // xml , style
    public DragLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        // 1. 创建ViewDragHelper
        // forParent 父布局
        // sensitivity 敏感度
        // cb 回调
        mHelper = ViewDragHelper.create(this, 1.0f, cb);
    }

    // 3. 重写事件回调方法
    ViewDragHelper.Callback cb = new ViewDragHelper.Callback() {

        // a. 返回值决定了当前child是否可以拖拽, true可以拖拽
        @Override
        public boolean tryCaptureView(View child, int pointerId) {
            System.out.println("tryCaptureView: " + child);
            // 尝试捕获View视图
            // child 手指按下的View
            // pointerId 多点触摸的手指id
            return true;
        }

        // b. 返回水平方向拖拽范围. 不是用于限制其真正的拖拽范围.
        // 用来计算动画执行时长, 水平方向是否可以被滑动开需要 >0 (有子View抢夺焦点时)
        @Override
        public int getViewHorizontalDragRange(View child) {
            return mRange;
        };

        // c. 修正水平方向的拖拽位置. 返回值决定了将要移动到的位置. (此时还未发生真正的位移)
        public int clampViewPositionHorizontal(View child, int left, int dx) {
            // child 被拖拽的子View
            // left 框架建议移动到的位置
            // dx 将要发生的位移量
            int oldLeft = mMainContent.getLeft();
            System.out.println("clampViewPositionHorizontal: left: " + left
                    + " dx: " + dx + " oldLeft: " + oldLeft);
            if (child == mMainContent) {
                left = fixLeft(left);
            }
            return left;
        }

        // d. 当View位置发生变化之后,被调用. 此时处理(伴随动画, 状态更新, 事件回调)
        @Override
        public void onViewPositionChanged(View changedView, int left, int top,
                                          int dx, int dy) {
            System.out.println("onViewPositionChanged: left: " + left + " dx: "
                    + dx);
            // changedView 位置发生变化的View
            // left : View当前的水平位置
            // dx : 刚刚发生水平方向的位移量

            // 移动之后, 把左面板放回去
            if (changedView == mLeftContent) {
                // 如果拖拽的是左面板, 放回原来位置
                mLeftContent.layout(0, 0, mWidth, mHeight);

                // 把变化量转交给主面板dx
                int newLeft = mMainContent.getLeft() + dx;
                // 修正位置
                newLeft = fixLeft(newLeft);
                mMainContent.layout(newLeft, 0, newLeft + mWidth, 0 + mHeight);// 左上右下
            }

            dispatchDragEvent();

            invalidate(); // 手动重绘界面, 让修改后的值生效. // 兼容低版本
        }

        // e. 在View被释放, 要做的事情
        @Override
        public void onViewReleased(View releasedChild, float xvel, float yvel) {
            System.out.println("onViewReleased: xvel:" + xvel);
            // releasedChild 被释放的子View
            // xvel : 释放时, 水平方向的速度 , 向左-, 向右+
            // yvel : 释放时, 竖直方向的速度, 向上-, 向下+

            // 所有开启的情况
            if (xvel == 0 && mMainContent.getLeft() > mRange * 0.5f) {
                open();
            } else if (xvel > 0) {
                open();
            } else {
                close();
            }
        }

        // f.状态更新回调
        // STATE_IDLE 空闲
        // STATE_DRAGGING 拖拽
        // STATE_SETTLING 自动化状态
        @Override
        public void onViewDragStateChanged(int state) {
            super.onViewDragStateChanged(state);
            // 状态切换的时候此方法被调用
        }

    };

    /**
     * 修正范围
     *
     * @param left
     * @return
     */
    private int fixLeft(int left) {
        if (left < 0) {
            // 限定左边界
            return 0;
        } else if (left > mRange) {
            // 限定右边界
            return mRange;
        }
        return left;
    }

    /**
     * 伴随动画, 状态更新, 回调
     */
    protected void dispatchDragEvent() {
        // 时间轴 0.0 -> 1.0
        float percent = mMainContent.getLeft() * 1.0f / mRange;

        System.out.println("percent: " + percent);

        // 执行伴随动画
        animView(percent);

        if (onStatusUpdateListener != null) {
            onStatusUpdateListener.onDraging(percent);
        }

        // 更新状态
        Status lastStatus = status;// 记录改变之前的状态
        status = updateStatus(percent);

        // 当前状态和上一次状态不一致, 调用回调
        if (lastStatus != status) {
            if (status == Status.Close) {
                onStatusUpdateListener.onClose();
            } else if (status == Status.Open) {
                onStatusUpdateListener.onOpen();
            }
        }

    }

    // 更新状态
    private Status updateStatus(float percent) {
        if (percent == 0) {
            return Status.Close;
        } else if (percent == 1) {
            return Status.Open;
        }
        return Status.Draging;
    }

    private void animView(float percent) {
        ViewHelper.setScaleX(mLeftContent, evaluate(percent, 0.5f, 1.0f));
        ViewHelper.setScaleY(mLeftContent, evaluate(percent, 0.5f, 1.0f));

        // 平移动画
        // -mWidth / 2.0f -> 0
        ViewHelper.setTranslationX(mLeftContent,
                evaluate(percent, -mWidth / 2.0f, 0));

        // 透明度 0.2 -> 1.0
        ViewHelper.setAlpha(mLeftContent, evaluate(percent, 0.2f, 1.0f));

        // 2. 主面板: 缩放动画 1.0 -> 0.6
        ViewHelper.setScaleX(mMainContent, evaluate(percent, 1.0f, 0.8f));
        ViewHelper.setScaleY(mMainContent, evaluate(percent, 1.0f, 0.8f));

    }

    public Float evaluate(float fraction, Number startValue, Number endValue) {
        float startFloat = startValue.floatValue();
        return startFloat + fraction * (endValue.floatValue() - startFloat);
    }

    public Object evaluateColor(float fraction, Object startValue,
                                Object endValue) {
        int startInt = (Integer) startValue;
        int startA = (startInt >> 24) & 0xff;
        int startR = (startInt >> 16) & 0xff;
        int startG = (startInt >> 8) & 0xff;
        int startB = startInt & 0xff;

        int endInt = (Integer) endValue;
        int endA = (endInt >> 24) & 0xff;
        int endR = (endInt >> 16) & 0xff;
        int endG = (endInt >> 8) & 0xff;
        int endB = endInt & 0xff;

        return (int) ((startA + (int) (fraction * (endA - startA))) << 24)
                | (int) ((startR + (int) (fraction * (endR - startR))) << 16)
                | (int) ((startG + (int) (fraction * (endG - startG))) << 8)
                | (int) ((startB + (int) (fraction * (endB - startB))));
    }

    /**
     * 关闭
     */
    protected void close() {
        close(true);
    }

    public void close(boolean isSmooth) {
        int finalLeft = 0;
        if (isSmooth) {
            // 1. 触发平滑动画
            if (mHelper.smoothSlideViewTo(mMainContent, finalLeft, 0)) {
                // true 动画(数据模拟)还没有结束, 需要引发界面重绘
                ViewCompat.postInvalidateOnAnimation(this);
            }
        } else {
            mMainContent.layout(finalLeft, 0, finalLeft + mWidth, 0 + mHeight);
        }
    }

    /**
     * 打开
     */
    protected void open() {
        open(true);
    }

    public void open(boolean isSmooth) {
        int finalLeft = mRange;
        if (isSmooth) {
            // 1. 触发平滑动画
            if (mHelper.smoothSlideViewTo(mMainContent, finalLeft, 0)) {
                // true 动画(数据模拟)还没有结束, 需要引发界面重绘
                ViewCompat.postInvalidateOnAnimation(this);
            }
        } else {
            mMainContent.layout(finalLeft, 0, finalLeft + mWidth, 0 + mHeight);
        }
    }

    @Override
    public void computeScroll() {
        super.computeScroll();

        // 2. 维持平滑动画的继续
        if (mHelper.continueSettling(true)) {
            // true 动画(数据模拟)还没有结束, 需要引发界面重绘
            ViewCompat.postInvalidateOnAnimation(this);
        }
    }

    // 2. 转交触摸事件拦截判断, 触摸事件处理
    public boolean onInterceptTouchEvent(android.view.MotionEvent ev) {
        return mHelper.shouldInterceptTouchEvent(ev);
    };

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        try {
            mHelper.processTouchEvent(event);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return true;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        // xml填充完毕, 查找子控件

        // 得到两个子布局的引用
        mLeftContent = getChildAt(0);
        mMainContent = getChildAt(1);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    // 在测量后, 发现有控件宽高发生变化时, 被调用
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        mHeight = getMeasuredHeight();
        mWidth = getMeasuredWidth();

        mRange = (int) (mWidth * 0.6f);
    }
}