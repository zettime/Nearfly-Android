package de.pbma.nearflyexample.lala.scenarios.Touchpoint;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

public class CustomView extends View{
    final String TAG = "CustomView";

    private int cHeight;
    private int cWidth;

    /** The following vectors are used as a kind of limited queue, whereby
     * the non-doing touchpoints are always modified and the first 10 elements of each vector
     * are reserved for up to 10 multi-touch capable host device
     */

    final int MAX_TP = 100;
    private float[] tpX = new float[MAX_TP];
    private float[] tpY = new float[MAX_TP];
    private float[] tpRadius = new float[MAX_TP];
    private int[] tpColorIndex = new int[MAX_TP];

    AvailableColors avaibleColors;

    public float RESET_X = -500;
    public float RESET_Y = -500;
    public float RESET_R = 200;

    public CustomView(Context context) {
        super(context);
    }

    public CustomView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public CustomView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    interface CustomViewListener{
        void sendTouchpoint(float tpX, float tpY, int tpColorIndex);
    }
    CustomViewListener customViewListener;

    public void registerListener(CustomViewListener customViewListener) {
        // super(context);
        this.customViewListener = customViewListener;
        avaibleColors = new AvailableColors();

        // Init own Touchpoint(TP)-Color
        tpColorIndex[0] = avaibleColors.getRandomTpColorIndex();

        // Init the vectors
        for (int i = 0; i < MAX_TP; i++) {
            tpX[i] = RESET_X;
            tpY[i] = RESET_Y;
            tpRadius[i] = 0;
            tpColorIndex[i] = tpColorIndex[0];
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawColor(Color.WHITE);
        final int canvasWidth = canvas.getWidth();

        // Update Canvas Metrics
        if (cWidth!=canvasWidth) {
            cHeight = canvas.getHeight();
            cWidth = canvasWidth;
            RESET_R = canvasWidth/8;
            RESET_X = -canvasWidth;
            RESET_Y = -canvasWidth;
        }

        // Make Touchpoint get smaller with Time
        for (int i = 0; i < MAX_TP; i++) {
            if (tpRadius[i] > 1){
                canvas.drawCircle(tpX[i], tpY[i], tpRadius[i], avaibleColors.getPaint(tpColorIndex[i]));
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

                customViewListener.sendTouchpoint(tpX[pId]/cWidth, tpY[pId]/cHeight, tpColorIndex[pId]);
                break;
        }
        return true;
    }

    public void createTouchpoint(float percTpX, float percTpY, int tpColorIndex){
        for (int i = 10; i < MAX_TP; i++) {
            if (tpRadius[i]==0) {
                tpRadius[i] = RESET_R;
                this.tpX[i] = percTpX*cWidth;
                this.tpY[i] = percTpY*cHeight;
                this.tpColorIndex[i] = tpColorIndex;
                return;
            }
        }
    }

    public int getMyColor(){
        return avaibleColors.getPaint(tpColorIndex[0]).getColor();

    }

    private void lg(String str) {
        Log.v(TAG, str);
    }
}
