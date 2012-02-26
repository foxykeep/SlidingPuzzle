package com.foxykeep.fbpuzzle;

import java.util.ArrayList;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.SparseArray;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.Spinner;

import com.foxykeep.fbpuzzle.widget.Puzzle;

public class HomeActivity extends Activity implements OnClickListener {

    private static final int[][] PUZZLE_IMAGES = new int[3][15];
    static {
        PUZZLE_IMAGES[0] = new int[] {
                R.drawable.globe_0, R.drawable.globe_1, R.drawable.globe_2, R.drawable.globe_3, R.drawable.globe_4, R.drawable.globe_5,
                R.drawable.globe_6, R.drawable.globe_7, R.drawable.globe_8, R.drawable.globe_9, R.drawable.globe_10, R.drawable.globe_11,
                R.drawable.globe_12, R.drawable.globe_13, R.drawable.globe_14
        };
        PUZZLE_IMAGES[1] = new int[] {
                R.drawable.facebook_0, R.drawable.facebook_1, R.drawable.facebook_2, R.drawable.facebook_3, R.drawable.facebook_4,
                R.drawable.facebook_5, R.drawable.facebook_6, R.drawable.facebook_7, R.drawable.facebook_8, R.drawable.facebook_9,
                R.drawable.facebook_10, R.drawable.facebook_11, R.drawable.facebook_12, R.drawable.facebook_13, R.drawable.facebook_14
        };
        PUZZLE_IMAGES[2] = new int[] {
                R.drawable.golden_gate_0, R.drawable.golden_gate_1, R.drawable.golden_gate_2, R.drawable.golden_gate_3, R.drawable.golden_gate_4,
                R.drawable.golden_gate_5, R.drawable.golden_gate_6, R.drawable.golden_gate_7, R.drawable.golden_gate_8, R.drawable.golden_gate_9,
                R.drawable.golden_gate_10, R.drawable.golden_gate_11, R.drawable.golden_gate_12, R.drawable.golden_gate_13, R.drawable.golden_gate_14
        };
    }

    private Button mButtonReset;
    private Button mButtonScramble;
    private Spinner mSpinnerImages;
    private Puzzle mPuzzle;

    private SparseArray<ArrayList<Drawable>> mSparseArrayDrawableList = new SparseArray<ArrayList<Drawable>>();;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.home);

        bindViews();

        loadDrawable(0);
    }

    private void bindViews() {
        mButtonReset = (Button) findViewById(R.id.b_reset);
        mButtonReset.setOnClickListener(this);

        mButtonScramble = (Button) findViewById(R.id.b_scramble);
        mButtonScramble.setOnClickListener(this);

        mSpinnerImages = (Spinner) findViewById(R.id.sp_images);
        mSpinnerImages.setOnItemSelectedListener(new OnItemSelectedListener() {

            @Override
            public void onItemSelected(final AdapterView<?> parent, final View view, final int position, final long id) {
                loadDrawable(position);
            }

            @Override
            public void onNothingSelected(final AdapterView<?> parent) {
            }
        });

        mPuzzle = (Puzzle) findViewById(R.id.p_puzzle);
    }

    private void loadDrawable(final int imageSet) {
        if (imageSet < 0 || imageSet > PUZZLE_IMAGES.length) {
            return;
        }
        if (mSparseArrayDrawableList.get(imageSet) != null) {
            mPuzzle.setDrawableList(mSparseArrayDrawableList.get(imageSet));
        } else {
            final ArrayList<Drawable> drawableList = new ArrayList<Drawable>();
            for (int i = 0; i < PUZZLE_IMAGES[imageSet].length; i++) {
                drawableList.add(getResources().getDrawable(PUZZLE_IMAGES[imageSet][i]));
            }

            mPuzzle.setDrawableList(drawableList);
            mSparseArrayDrawableList.put(imageSet, drawableList);
        }
    }

    @Override
    public void onClick(final View v) {
        if (v == mButtonReset) {
            mPuzzle.resetPuzzle();
        } else if (v == mButtonScramble) {
            mPuzzle.scramblePuzzle();
        }
    }
}
