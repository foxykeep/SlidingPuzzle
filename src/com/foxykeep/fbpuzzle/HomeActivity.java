package com.foxykeep.fbpuzzle;

import java.util.ArrayList;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.os.Bundle;

import com.foxykeep.fbpuzzle.widget.Puzzle;

public class HomeActivity extends Activity {

    private static final int[] GLOBE_IMAGES = new int[] {
            R.drawable.globe_0, R.drawable.globe_1, R.drawable.globe_2, R.drawable.globe_3, R.drawable.globe_4, R.drawable.globe_5,
            R.drawable.globe_6, R.drawable.globe_7, R.drawable.globe_8, R.drawable.globe_9, R.drawable.globe_10, R.drawable.globe_11,
            R.drawable.globe_12, R.drawable.globe_13, R.drawable.globe_14
    };

    private Puzzle mPuzzle;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.home);

        bindViews();

        populateViews();
    }

    private void bindViews() {
        mPuzzle = (Puzzle) findViewById(R.id.p_puzzle);
    }

    private void populateViews() {
        final ArrayList<Drawable> drawableList = new ArrayList<Drawable>();
        for (int i = 0; i < GLOBE_IMAGES.length; i++) {
            drawableList.add(getResources().getDrawable(GLOBE_IMAGES[i]));
        }

        mPuzzle.setDrawableList(drawableList);
    }
}
