package de.pbma.nearflyexample.lala.scenarios;

import android.graphics.Color;
import android.graphics.Paint;

import java.util.Random;

public class AvailableColors {
    private int[] intColors = {Color.RED, Color.GREEN, Color.BLUE,
            Color.YELLOW, Color.CYAN, Color.MAGENTA
    };

    private Paint[] paintColors = new Paint[intColors.length];

    public AvailableColors() {
        for (int i=0; i<intColors.length; i++){
            paintColors[i] = new Paint();
            paintColors[i].setColor(intColors[i]);
        }
    }

    public int getRandomTpColorIndex(){
        return new Random().nextInt(intColors.length);
    }

    public Paint getPaint(int index){
        return paintColors[index];
    }
}
