package com.ethan.screencapture.view;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import com.ethan.screencapture.Const;
import com.ethan.screencapture.service.FloatBallService;

/**
 * @author wxl
 * 2018/05/21
 */
public class RegionView extends View {

    public static final int STROKE_WIDTH_THIN = 3;
    public static final int STROKE_WIDTH_MEDIUM = 5;
    public static final int STROKE_WIDTH_THICK = 10;
    public static final int STROKE_WIDTH_DEFAULT = STROKE_WIDTH_THIN;

    private Path mPath;
    private Paint mPaint;
    private Rect mBounds;
    private float mTextStartX;
    private float mTextEndX;
    private float mTextStartY;
    private float mTextEndY;
//    private static Map<Path, Paint> mData = new HashMap<>();
//    private static List<Path> mHistory = new ArrayList<>();
    private SharedPreferences sp;
    public RegionView(Context context) {
        super(context);
        setupDrawing();
        sp = PreferenceManager.getDefaultSharedPreferences(context);
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
        mBounds = new Rect();
        mPath = new Path();

    }

    protected void onDraw(Canvas canvas) {
//        for (int i = 0; i < mHistory.size(); i++) {
//            Path path = mHistory.get(i);
//            canvas.drawPath(path, mData.get(path));
//        }

//        mPaint.setTextSize(30);
//        String text = "Icon";
//        // 获取文字的宽和高
//        mPaint.getTextBounds(text, 0, text.length(), mBounds);
//        float textWidth = mBounds.width();
//        float textHeight = mBounds.height();
//
//        mPaint.setColor(Color.BLUE);
//        // 绘制一个填充色为蓝色的矩形
//        canvas.drawRect(ScreenCaptureApplication.mScreenWidth - textWidth-120, ScreenCaptureApplication.mScreenHeight -textHeight-80, ScreenCaptureApplication.mScreenWidth -20, ScreenCaptureApplication.mScreenHeight-20, mPaint);
//        // 绘制字符串
//        mPaint.setColor(Color.YELLOW);
//        canvas.drawText(text, ScreenCaptureApplication.mScreenWidth - textWidth -70, ScreenCaptureApplication.mScreenHeight -50, mPaint);
//        mPaint.setColor(Color.RED);

        if (x1 != x2 || y1!= y2){
            reDrawText(canvas);
        }
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setColor(Color.RED);
        canvas.drawPath(mPath, mPaint);
    }

    private float mX, mY;
    private float x1;
    private float y1;
    private float x2;
    private float y2;
    private int mShape = 0;
    //added by wangxl for partScreen
    private int X;
    private int Y;
    private int width;
    private int height;

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

                    mPath.reset();
                    mPath.moveTo(x1, y1);
                    mPath.addRoundRect(new RectF(x1, y1, x2, y2),
                            new float[]{0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F,
                                    0.0F, 0.0F}, localDirection);
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
                mPath.reset();
                mPath.moveTo(x1, y1);
                mPath.addRoundRect(new RectF(x1, y1, x2, y2), new float[]{0.0F,
                                0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F},
                        localDirection);
//        Path localPath = new Path(mPath);
//        mHistory.add(localPath);
//        mData.put(localPath, new Paint(mPaint));
//        mPath.reset();
        Log.d("wxl","-----x1----"+x1+"------x2------"+x2+"---------y1-------"+y1+"-------y2-------"+y2);
        X = (int)(x1 <x2 ? x1 :x2);
        Y = (int)(y1 <y2 ? y1 :y2);
        width = (int) Math.abs((int)(x1 -x2));
        height = (int) Math.abs((int)(y1-y2));
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
                if ( (x > mTextStartX && x < mTextEndX) &&( y > mTextStartY && y <mTextEndY) ){
                    Log.d("wxl","haahahahahahahhaha");
                    Log.d("wxl","x----"+X+"-----Y----"+Y+"-------width----"+width+"-------height-----"+height);
                    String partMsg = X+","+Y+","+width+","+height;
                    sp.edit().putString(Const.REGION_LOCATION,partMsg).commit();
                    FloatBallService.mRegionBgView.setVisibility(GONE);
                    this.setVisibility(GONE);
                    this.clearFocus();
                }
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
//        if ((mHistory != null) && (mHistory.size() > 0)) {
//            Path path = mHistory.remove(mHistory.size() - 1);
//            mData.remove(path);
//            invalidate();
//        }
    }

    public void clear() {
//        mHistory.clear();
//        mData.clear();
//        invalidate();
    }

    private void reDrawText(Canvas canvas){
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setTextSize(30);
        String text = "确认";
        // 获取文字的宽和高
        mPaint.getTextBounds(text, 0, text.length(), mBounds);
        float textWidth = mBounds.width();
        float textHeight = mBounds.height();

//        mPaint.setColor(Color.BLUE);
//        // 绘制一个填充色为蓝色的矩形
//        canvas.drawRect(ScreenCaptureApplication.mScreenWidth - textWidth-120, ScreenCaptureApplication.mScreenHeight -textHeight-80, ScreenCaptureApplication.mScreenWidth -20, ScreenCaptureApplication.mScreenHeight-20, mPaint);
        // 绘制字符串
        mPaint.setColor(Color.GREEN);
        mTextStartX =  (x1 < x2 ? x2 -100 : x1 -100);
        mTextEndX = ((x1 < x2 ? x2 -100 : x1 -100) + textWidth +50);
        mTextStartY = ( y1 < y2 ? y1  :y2 );
        mTextEndY = (( y1 < y2 ? y1  :y2 ) + textHeight+100);
        canvas.drawText(text,  x1 < x2 ? x2 -100 : x1 -100, y1 < y2 ? y1 +  50 :y2 + 50, mPaint);
    }
}
