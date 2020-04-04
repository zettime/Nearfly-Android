package de.pbma.nearflyexample.lala.scenarios;

import android.graphics.Color;
import android.graphics.Paint;

import java.util.Random;

public class AvailableColors {
    private int[] mIntColors = {Color.RED, Color.GREEN, Color.BLUE, Color.BLACK,
            Color.YELLOW, Color.CYAN, Color.MAGENTA,
            Color.DKGRAY, Color.GRAY, Color.LTGRAY
    };

    private Paint[] mPaintColors = new Paint[mIntColors.length];

    public AvailableColors() {
        for (int i = 0; i< mIntColors.length; i++){
            mPaintColors[i] = new Paint();
            mPaintColors[i].setColor(mIntColors[i]);
        }
    }

    public int getRandomTpColorIndex(){
        return new Random().nextInt(mIntColors.length);
    }

    public Paint getPaint(int index){
        return mPaintColors[index];
    }
}
