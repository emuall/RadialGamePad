package com.swordfish.radialgamepad.library;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

import androidx.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;


public class TouchTraceView extends View
{
    Context mContext;
    private Paint line_paint, text_paint, countPaint;
    int screenW, screenH;
    private int paintColor = Color.RED;
    Map<Integer, TouchPoint> pointMap;
    float back_x1, back_y1, back_x2, back_y2;

    public TouchTraceView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        WindowManager manager=(WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics displayMetrics=new DisplayMetrics();
        Display display=manager.getDefaultDisplay();
        display.getMetrics(displayMetrics);
        this.screenW=displayMetrics.widthPixels;
        this.screenH=displayMetrics.heightPixels;
        mContext = context;
        pointMap = new HashMap<>();
        initPaint();
        setBackgroundColor(Color.TRANSPARENT);
    }

    private void initPaint()
    {
        line_paint = new Paint();
        line_paint.setAntiAlias(true);
        line_paint.setColor(paintColor);
        text_paint = new Paint();
        text_paint.setAntiAlias(true);
        text_paint.setColor(Color.GREEN);
        text_paint.setTextSize(30);
        countPaint = new Paint();
        countPaint.setAntiAlias(true);
        countPaint.setColor(Color.GREEN);
        countPaint.setTextSize(60);
    }

    @Override
    protected void onDraw(Canvas canvas)
    {
        super.onDraw(canvas);
        int num = pointMap.size();
        if (num == 0)
        {
            clearDraw(canvas);
            return;
        }
        for (Map.Entry<Integer, TouchPoint> entry : pointMap.entrySet())
        {
            TouchPoint point = entry.getValue();
            canvas.drawLine(0, point.y, getWidth(), point.y, line_paint);
            canvas.drawLine(point.x, 0, point.x, getHeight(), line_paint);
            if (num == 1)
            {
                canvas.drawText(" (" + (int)point.x + "," + (int)point.y + ")", point.x, point.y, text_paint);
            } else
            {
                canvas.drawText(String.valueOf(pointMap.size()), point.x, point.y, countPaint);
            }
        }
    }


    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        int index = event.getActionIndex();
        int id = event.getPointerId(index);
        int pointerIndex = event.findPointerIndex(id);
        int pointerCount = event.getPointerCount();
        int historySize = event.getHistorySize();
        switch (event.getActionMasked())
        {
            case MotionEvent.ACTION_POINTER_DOWN:
                pointMap.put(pointerIndex, new TouchPoint(event.getX(pointerIndex), event.getY(pointerIndex)));
                break;
            case MotionEvent.ACTION_POINTER_UP:
                pointMap.remove(pointerIndex);
                break;
            case MotionEvent.ACTION_MOVE:
                for (int h = 0; h < historySize; h++)
                {
                    for (int p = 0; p < pointerCount; p++)
                    {
                        pointMap.put(p, new TouchPoint(event.getHistoricalX(p, h), event.getHistoricalY(p, h)));
                    }
                }
                for (int p = 0; p < pointerCount; p++)
                {
                    pointMap.put(p, new TouchPoint(event.getX(p), event.getY(p)));
                }

                break;
            case MotionEvent.ACTION_DOWN:
                pointMap.put(0, new TouchPoint(event.getX(pointerIndex), event.getY(pointerIndex)));
                back_x1 = event.getX();
                back_y1 = event.getY();
                break;
            case MotionEvent.ACTION_UP:
                back_x2 = event.getX();
                back_y2 = event.getY();
                if (Math.abs(back_x1 - back_x2) > screenW / 2 && Math.abs(back_y1 - back_y2) > screenH / 2)
                {
                    callOnClick();
                }
                pointMap.clear();
                break;
            default:
                break;
        }
        if (event.getPointerCount() == 0) pointMap.clear();
        invalidate();
        return true;
    }

    class TouchPoint
    {
        public float x = 0;
        public float y = 0;

        TouchPoint(float x, float y)
        {
            this.x = x;
            this.y = y;
        }
    }

    void clearDraw(Canvas canvas)
    {
        Paint paint = new Paint();
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        canvas.drawPaint(paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC));
        canvas.drawColor(Color.WHITE);
    }
}