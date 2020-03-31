package de.pbma.nearflyexample.lala.scenarios;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;

import java.util.Random;

public class CustomView extends View{
    private int[] avaibleColors = {Color.RED, Color.GREEN, Color.BLUE,
            Color.YELLOW, Color.CYAN, Color.MAGENTA
    };
    private Paint paint;

    final String TAG = "CustomView";

    final int MAX_TP = 100;
    private float[] tpX = new float[MAX_TP];
    private float[] tpY = new float[MAX_TP];
    private float[] tpRadius = new float[MAX_TP];
    private int tpColor;

    public final float RESET_X = -500;
    public final float RESET_Y = -500;
    public final float RESET_R = 200;

    interface CustomViewListener{
        void onAction(float tpX, float tpY, int tpColor);
    }
    CustomViewListener customViewListener;

    public CustomView(Context context, CustomViewListener customViewListener) {
        super(context);
        this.customViewListener = customViewListener;

        // Init Touchpoints(TPs)
        tpColor = avaibleColors[new Random().nextInt(avaibleColors.length)];
        paint = new Paint();
        paint.setColor(tpColor);

        for (int i = 0; i < MAX_TP; i++) {
            tpX[i] = RESET_X;
            tpY[i] = RESET_Y;
            tpRadius[i] = 0;
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawColor(Color.WHITE);

        for (int i = 0; i < MAX_TP; i++) {
            if (tpRadius[i] > 1){
                canvas.drawCircle(tpX[i], tpY[i], tpRadius[i], paint);
                tpRadius[i] *= 0.8;
            }
            if (tpRadius[i] < 1 && tpRadius[i]!= 0) {
                tpX[i] = RESET_X;
                tpX[i] = RESET_Y;
                tpRadius[i] = 0;
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int index = event.getActionIndex();
        int action = event.getActionMasked();
        int pointerId = event.getPointerId(index);
        int pId = event.findPointerIndex(pointerId);

        /** For more @see https://developer.android.com/training/gestures/movement **/
        /** @see also https://developer.android.com/training/gestures/multi */
        switch (action) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN:
                lg(tpX[pId] + " " + tpY[pId]);
                lg("tpid: " + pId);

                tpX[pId] = event.getX(pId);
                tpY[pId] = event.getY(pId);
                tpRadius[pId] = RESET_R;

                customViewListener.onAction(tpX[pId], tpY[pId], tpColor);
                break;
        }
        return true;
    }

    public void createTouchpoint(float tpX, float tpY, int tpColor){
        for (int i = 0; i < MAX_TP; i++) {
            if (tpRadius[i]==0) {
                tpRadius[i] = RESET_R;
                this.tpX[i] = tpX;
                this.tpY[i] = tpY;
                return;
            }
        }
    }

    private void lg(String str) {
        Log.v(TAG, str);
    }
}
