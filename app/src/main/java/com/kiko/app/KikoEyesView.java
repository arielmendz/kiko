package com.kiko.app;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.View;

public final class KikoEyesView extends View {
    private static final float EYE_SCALE = 0.90f;

    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint outlinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path eyeClip = new Path();
    private final RectF eyeBounds = new RectF();
    private KikoEyeMotion.Mode mode = KikoEyeMotion.Mode.RESTING;
    private long modeStartedAtMillis = SystemClock.uptimeMillis();

    public KikoEyesView(Context context) {
        this(context, null);
    }

    public KikoEyesView(Context context, AttributeSet attributes) {
        super(context, attributes);
        outlinePaint.setStyle(Paint.Style.STROKE);
        outlinePaint.setStrokeWidth(dp(4));
        outlinePaint.setStrokeCap(Paint.Cap.ROUND);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }

    public void setMode(KikoEyeMotion.Mode nextMode) {
        if (nextMode == null || nextMode == mode) {
            return;
        }
        mode = nextMode;
        modeStartedAtMillis = SystemClock.uptimeMillis();
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int desiredWidth = dp(300) + getPaddingLeft() + getPaddingRight();
        int desiredHeight = dp(150) + getPaddingTop() + getPaddingBottom();
        setMeasuredDimension(
                resolveSize(desiredWidth, widthMeasureSpec),
                resolveSize(desiredHeight, heightMeasureSpec)
        );
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        boolean animate = mode == KikoEyeMotion.Mode.LISTENING
                && ValueAnimator.areAnimatorsEnabled();
        long elapsedMillis = animate
                ? SystemClock.uptimeMillis() - modeStartedAtMillis
                : 0L;
        KikoEyeMotion.Sample sample = KikoEyeMotion.sample(mode, elapsedMillis);

        float contentWidth = getWidth() - getPaddingLeft() - getPaddingRight();
        float contentHeight = getHeight() - getPaddingTop() - getPaddingBottom();
        float centerY = getPaddingTop() + contentHeight * 0.52f;
        float gap = contentWidth * 0.035f;
        float baseLeftWidth = contentWidth * 0.43f;
        float baseRightWidth = contentWidth * 0.45f;
        float leftCenterX = getPaddingLeft() + baseLeftWidth * 0.5f;
        float rightCenterX = getWidth() - getPaddingRight() - baseRightWidth * 0.5f;
        float leftWidth = baseLeftWidth * EYE_SCALE;
        float rightWidth = baseRightWidth * EYE_SCALE;
        float leftHeight = contentHeight * 0.80f * EYE_SCALE;
        float rightHeight = contentHeight * 0.88f * EYE_SCALE;

        drawEye(
                canvas,
                leftCenterX - gap,
                centerY + dp(2),
                leftWidth,
                leftHeight,
                sample
        );
        drawEye(
                canvas,
                rightCenterX + gap,
                centerY - dp(3),
                rightWidth,
                rightHeight,
                sample
        );

        if (animate && isShown()) {
            postInvalidateOnAnimation();
        }
    }

    private void drawEye(
            Canvas canvas,
            float centerX,
            float centerY,
            float width,
            float fullHeight,
            KikoEyeMotion.Sample sample
    ) {
        float visibleHeight = Math.max(dp(5), fullHeight * sample.getOpenness());
        eyeBounds.set(
                centerX - width * 0.5f,
                centerY - visibleHeight * 0.5f,
                centerX + width * 0.5f,
                centerY + visibleHeight * 0.5f
        );

        fillPaint.setStyle(Paint.Style.FILL);
        fillPaint.setColor(Color.WHITE);
        canvas.drawOval(eyeBounds, fillPaint);
        outlinePaint.setColor(Color.rgb(5, 8, 14));
        canvas.drawOval(eyeBounds, outlinePaint);

        int checkpoint = canvas.save();
        eyeClip.reset();
        eyeClip.addOval(eyeBounds, Path.Direction.CW);
        canvas.clipPath(eyeClip);
        float irisRadius = Math.min(width * 0.13f, visibleHeight * 0.34f);
        float pupilCenterX = centerX + sample.getGazeX() * width * 0.20f;
        float pupilCenterY = centerY + sample.getGazeY() * visibleHeight * 0.16f;

        fillPaint.setColor(getResources().getColor(R.color.kiko_accent, null));
        canvas.drawCircle(pupilCenterX, pupilCenterY, irisRadius, fillPaint);
        fillPaint.setColor(Color.rgb(5, 8, 14));
        canvas.drawCircle(
                pupilCenterX,
                pupilCenterY,
                irisRadius * 0.62f,
                fillPaint
        );
        fillPaint.setColor(Color.WHITE);
        canvas.drawCircle(
                pupilCenterX - irisRadius * 0.22f,
                pupilCenterY - irisRadius * 0.24f,
                Math.max(dp(1), irisRadius * 0.16f),
                fillPaint
        );
        canvas.restoreToCount(checkpoint);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
