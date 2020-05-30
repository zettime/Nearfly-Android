package de.pbma.nearflyexample.scenarios.BouncingBall;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentLinkedDeque;

import de.pbma.nearflyexample.R;

public class GameView extends View {
    final String TAG = "GameView";
    private Bitmap mBitmap;
    private int mMyX;
    private int mMyY;
    private OrientationData mOrientationData;
    private int mCanvasWidth;
    private int mCanvasHeight;
    private int mUnit;
    private LinearLayout mGameOverScreen;
    private TextView mGameOverText;
    private GameViewListener mGameViewListener;
    private long mStartTime;
    public final static int STATE_GAMEOVER = 1;
    public final static int STATE_PLAYING = 2;
    private int mState = STATE_PLAYING;

    private long mGameOverWaitTime;
    private volatile long mScore = 0;

    public GameView(Context context) {
        super(context);
    }

    public GameView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public GameView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void logIt(final String str) {
        Log.v(TAG, str);
    }

    interface GameViewListener {
        void onStateChanged(int state);
        void onStep(float vOrientation, float hOrientation); // Ori = orientatonData value
    }

    public void registerListener(GameViewListener gameViewListener) {
        mGameViewListener = gameViewListener;
    }

    public void onCreate(Context context) {
        mBitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.bouncingball);
        mOrientationData = new OrientationData(context);
        mOrientationData.register();
        mStartTime = System.currentTimeMillis();
    }

    public void initDraw(Canvas canvas) {
        mCanvasHeight = canvas.getHeight();
        mCanvasWidth = canvas.getWidth();
        mUnit = mCanvasWidth / 10;
        mBitmap = Bitmap.createScaledBitmap(mBitmap, mUnit, mUnit, false);
        mMyX = mCanvasWidth / 2;
        mMyY = mCanvasHeight / 2;
    }

    public Bitmap rotateBitmap(Bitmap original, float degrees) {
        Bitmap bInput = Bitmap.createBitmap(original);
        Matrix matrix = new Matrix();
        matrix.setRotate(degrees);

        return Bitmap.createBitmap(bInput, 0, 0, original.getWidth(), original.getHeight(), matrix, false);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getActionMasked();

        /** For more @see https://developer.android.com/training/gestures/movement **/
        /** @see also https://developer.android.com/training/gestures/multi */
        switch (action) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN:
                if (mState == STATE_GAMEOVER && (System.currentTimeMillis()-mGameOverWaitTime) >= 1000)
                    changeState(STATE_PLAYING);
                break;
        }
        return true;
    }

    public void changeState(int state) {
        mState = state;
        mGameViewListener.onStateChanged(state);

        if (state == STATE_GAMEOVER) {
            mScore = System.currentTimeMillis() - mStartTime;
            mGameOverWaitTime = System.currentTimeMillis();
        }

        if (state == STATE_PLAYING) {
            mMyX = mCanvasWidth / 2;
            mMyY = mCanvasHeight / 2;
            mStartTime = System.currentTimeMillis();
        }
    }

    public long getScore() {
        return mScore;
    }

    public void addValToPosition(float vOrientation, float hOrientation){
        mMyY -= vOrientation * mUnit;
        mMyX += hOrientation * mUnit;

        /*try {
            mMyY -= vOrientationBuff.remove() * mUnit;
            mMyX += hOrientationBuff.remove() * mUnit;
        }catch (NoSuchElementException e){
            e.printStackTrace();
        }*/
    }

    Queue<Float> vOrientationBuff = new ArrayDeque<>();
    Queue<Float> hOrientationBuff = new ArrayDeque<>();

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawColor(Color.WHITE);

        if (mCanvasWidth != canvas.getWidth()) {
            initDraw(canvas);
        }

        if (mState == STATE_PLAYING) {
            if (mOrientationData.getOrientation() != null && mOrientationData.getStartOrientation() != null) {
                float vOrientation = mOrientationData.getOrientation()[1];
                float hOrientation = mOrientationData.getOrientation()[2];

                logIt("v: "+vOrientation+"  h: "+hOrientation);
                mGameViewListener.onStep(vOrientation, hOrientation);

                /*if (vOrientationBuff.isEmpty()) {
                    vOrientationBuff.add(vOrientation);
                    hOrientationBuff.add(hOrientation);
                }else{*/
                    mMyY -= vOrientation * mUnit;
                    mMyX += hOrientation * mUnit;
                //}
            }

            // GameOver Condition
            if (mMyX < 0 || mMyY < 0 || mMyX > mCanvasWidth-mUnit || mMyY > mCanvasHeight-mUnit) {
                changeState(STATE_GAMEOVER);
            }
        }

        mScore = System.currentTimeMillis() - mStartTime;
        mBitmap = rotateBitmap(mBitmap, 90);
        canvas.drawBitmap(mBitmap, mMyX, mMyY, null);
    }
}

