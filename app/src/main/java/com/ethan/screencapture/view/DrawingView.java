package com.ethan.screencapture.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author ethan
 * 2018/04/08
 */
public class DrawingView extends View {
    public static final int SHAPE_CURVE = 0;
    public static final int SHAPE_LINE = 1;
    public static final int SHAPE_RECT = 2;
    public static final int SHAPE_ROUND_RECT = 3;
    public static final int SHAPE_CIRCLE = 4;
    public static final int SHAPE_OVAL = 5;
    public static final int SHAPE_ARROW = 6;
    public static final int SHAPE_OTHERS = 7;

    public static final int STROKE_WIDTH_THIN = 8;
    public static final int STROKE_WIDTH_MEDIUM = 16;
    public static final int STROKE_WIDTH_THICK = 32;
    public static final int STROKE_WIDTH_DEFAULT = STROKE_WIDTH_MEDIUM;

    private Path mPath;
    private Paint mPaint;
    private static Map<Path, Paint> mData = new HashMap<>();
    private static List<Path> mHistory = new ArrayList<>();

    public DrawingView(Context context) {
        super(context);
        setupDrawing();
    }

    private void setupDrawing() {
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setDither(true);
        mPaint.setColor(0xbfff0000);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeJoin(Paint.Join.ROUND);
        mPaint.setStrokeCap(Paint.Cap.ROUND);
        mPaint.setStrokeWidth(STROKE_WIDTH_DEFAULT);
        mPath = new Path();

    }

    protected void onDraw(Canvas canvas) {
        for (int i = 0; i < mHistory.size(); i++) {
            Path path = mHistory.get(i);
            canvas.drawPath(path, mData.get(path));
        }
        canvas.drawPath(mPath, mPaint);
    }

    private float mX, mY;
    private float x1;
    private float y1;
    private float x2;
    private float y2;
    private int mShape = 0;

    private static final float TOUCH_TOLERANCE = 4;

    private void touch_down(float x, float y) {
        mPath.reset();
        mPath.moveTo(x, y);
        mX = x;
        mY = y;
        x1 = x;
        y1 = y;
        x2 = x;
        y2 = y;
    }

    private void touch_move(float x, float y) {
        float f1 = Math.abs(x - mX);
        float f2 = Math.abs(y - mY);
        Path.Direction localDirection = Path.Direction.CCW;
        if (x2 <= x1)
            localDirection = Path.Direction.CW;
        if ((f1 >= TOUCH_TOLERANCE) || (f2 >= TOUCH_TOLERANCE)) {

            switch (mShape) {
                case SHAPE_CURVE:
                    mPath.quadTo(mX, mY, (x + mX) / 2.0F, (y + mY) / 2.0F);
                    break;
                case SHAPE_LINE:
                    mPath.reset();
                    mPath.moveTo(x1, y1);
                    mPath.lineTo(x2, y2);
                    break;

                case SHAPE_RECT:
                    mPath.reset();
                    mPath.moveTo(x1, y1);
                    mPath.addRect(x1, y1, x2, y2, localDirection);
                    break;
                case SHAPE_ROUND_RECT:
                    mPath.reset();
                    mPath.moveTo(x1, y1);
                    mPath.addRoundRect(new RectF(x1, y1, x2, y2),
                            new float[]{10.0F, 10.0F, 10.0F, 10.0F, 10.0F, 10.0F,
                                    10.0F, 10.0F}, localDirection);
                    break;
                case SHAPE_CIRCLE:
                    mPath.reset();
                    mPath.moveTo(x1, y1);
                    float f3 = Math.abs(x1 - x2);
                    float f4 = Math.abs(y1 - y2);
                    mPath.addCircle((x2 + x1) / 2.0F, (y2 + y1) / 2.0F,
                            (float) Math.sqrt(f3 * f3 + f4 * f4) / 2.0F,
                            localDirection);
                    break;
                case SHAPE_OVAL:
                    mPath.reset();
                    mPath.moveTo(x1, y1);
                    mPath.addOval(new RectF(x1, y1, x2, y2), localDirection);
                    break;
                case SHAPE_ARROW:
                    mPath.reset();
                    mPath.moveTo(x1, y1);
                    drawArrow(x1, y1, x2, y2);
                    break;
                case SHAPE_OTHERS:
                    mPath.reset();
                    return;

                default:
                    mPath.quadTo(mX, mY, (x + mX) / 2.0F, (y + mY) / 2.0F);
            }
            mX = x;
            mY = y;
            x2 = x;
            y2 = y;
        }

    }

    private void touch_up() {
        Path.Direction localDirection = Path.Direction.CCW;
        if (x2 <= x1)
            localDirection = Path.Direction.CW;
        switch (mShape) {

            case SHAPE_CURVE:
                mPath.lineTo(mX, mY);
                break;
            case SHAPE_LINE:
                mPath.reset();
                mPath.moveTo(x1, y1);
                mPath.lineTo(x2, y2);
                break;

            case SHAPE_RECT:
                mPath.reset();
                mPath.moveTo(x1, y1);
                mPath.addRect(x1, y1, x2, y2, localDirection);
                break;
            case SHAPE_ROUND_RECT:
                mPath.reset();
                mPath.moveTo(x1, y1);
                mPath.addRoundRect(new RectF(x1, y1, x2, y2), new float[]{10.0F,
                                10.0F, 10.0F, 10.0F, 10.0F, 10.0F, 10.0F, 10.0F},
                        localDirection);
                break;
            case SHAPE_CIRCLE:
                mPath.reset();
                mPath.moveTo(x1, y1);
                float f1 = Math.abs(x1 - x2);
                float f2 = Math.abs(y1 - y2);
                mPath.addCircle((x2 + x1) / 2.0F, (y2 + y1) / 2.0F,
                        (float) Math.sqrt(f1 * f1 + f2 * f2) / 2.0F, localDirection);
                break;
            case SHAPE_OVAL:
                mPath.reset();
                mPath.moveTo(x1, y1);
                mPath.addOval(new RectF(x1, y1, x2, y2), localDirection);
                break;
            case SHAPE_ARROW:
                mPath.reset();
                mPath.moveTo(x1, y1);
                drawArrow(x1, y1, x2, y2);
                break;
            case SHAPE_OTHERS:
                mPath.reset();
                return;
            default:
                mPath.lineTo(mX, mY);
        }

        Path localPath = new Path(mPath);

        mHistory.add(localPath);
        mData.put(localPath, new Paint(mPaint));
        mPath.reset();
    }

    public void drawArrow(float x1, float y1, float x2, float y2) {
        float height = 16 + mPaint.getStrokeWidth();
        float half_base = 7 + mPaint.getStrokeWidth();
        float x3, y3, x4, y4;
        double awrad = Math.atan(half_base / height);
        double arrow_len = Math.sqrt(half_base * half_base + height * height);
        double[] arrXY_1 = rotateVec(x2 - x1, y2 - y1, awrad, true, arrow_len);
        double[] arrXY_2 = rotateVec(x2 - x1, y2 - y1, -awrad, true, arrow_len);
        x3 = (float) (x2 - arrXY_1[0]);
        y3 = (float) (y2 - arrXY_1[1]);
        x4 = (float) (x2 - arrXY_2[0]);
        y4 = (float) (y2 - arrXY_2[1]);

        mPath.moveTo(x1, y1);
        mPath.lineTo(x2, y2);
        mPath.moveTo(x2, y2);
        mPath.lineTo(x3, y3);
        mPath.moveTo(x2, y2);
        mPath.lineTo(x4, y4);

    }

    public double[] rotateVec(float f, float g, double ang, boolean isChLen,
                              double newLen) {
        double mathStr[] = new double[2];
        double vx = f * Math.cos(ang) - g * Math.sin(ang);
        double vy = f * Math.sin(ang) + g * Math.cos(ang);
        if (isChLen) {
            double d = Math.sqrt(vx * vx + vy * vy);
            vx = vx / d * newLen;
            vy = vy / d * newLen;
            mathStr[0] = vx;
            mathStr[1] = vy;
        }
        return mathStr;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        float x = event.getRawX();
        float y = event.getRawY();


        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                touch_down(x, y);
                invalidate();
                break;
            case MotionEvent.ACTION_MOVE:
                touch_move(x, y);
                invalidate();
                break;
            case MotionEvent.ACTION_UP:
                touch_up();
                invalidate();
                break;
        }
        return true;
    }

    public void setPenColor(int newColor) {
        mPaint.setXfermode(null);
        mPaint.setColor(newColor);
    }

    public void setPenSize(int newSize) {
        mPaint.setXfermode(null);
        mPaint.setStrokeWidth(newSize);
    }

    public void setShape(int shape) {
        mShape = shape;
    }

    public void undo() {
        if ((mHistory != null) && (mHistory.size() > 0)) {
            Path path = mHistory.remove(mHistory.size() - 1);
            mData.remove(path);
            invalidate();
        }
    }

    public void clear() {
        mHistory.clear();
        mData.clear();
        invalidate();
    }
}
