package com.foxykeep.fbpuzzle.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewConfiguration;

import com.foxykeep.fbpuzzle.config.LogConfig;

public class Puzzle extends View {

    private static final String LOG_TAG = Puzzle.class.getSimpleName();

    private int mBackgroundColor;
    private int mTileWidth;

    private final Tile[][] mTileArray = new Tile[4][4];
    private final SparseArray<Tile> mTileSparseArray = new SparseArray<Puzzle.Tile>();

    private int mWorkingAreaMinX;
    private int mWorkingAreaMaxX;
    private int mWorkingAreaMinY;
    private int mWorkingAreaMaxY;

    private final ViewConfiguration mViewConfiguration;

    private boolean mNoInvalidate;

    /**
     * Indicates whether or not onSizeChanged has been done.
     */
    protected boolean mLayoutDone = false;

    public Puzzle(final Context context) {
        this(context, null);
    }

    public Puzzle(final Context context, final AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public Puzzle(final Context context, final AttributeSet attrs, final int defStyle) {
        super(context, attrs, defStyle);

        mViewConfiguration = ViewConfiguration.get(context);

        initPuzzle();
    }

    /**
     * Initialize the {@link Puzzle} with default values.
     */
    private void initPuzzle() {

        setFocusable(true);
        setFocusableInTouchMode(true);
        requestFocus();

        final boolean tmpNoInvalidate = mNoInvalidate;
        mNoInvalidate = true;

        // General default properties
        setBackgroundColor(Color.BLACK);

        mNoInvalidate = tmpNoInvalidate;
    }

    @Override
    public void invalidate() {
        if (!mNoInvalidate) {
            super.invalidate();
        }
    }

    /**
     * Defines the background color of the component.
     * 
     * @param color The color to set as background
     * @throw {@link IllegalStateException} When trying to change the value of this property when the component is already displayed on screen
     */
    @Override
    public void setBackgroundColor(final int color) {
        if (mLayoutDone) {
            throw new IllegalStateException("The background color cannot be changed once the component has been displayed");
        }
        mBackgroundColor = color;
    }

    /**
     * Returns the background color of this view.
     * 
     * @return An integer representing the background color of this view.
     */
    public int getBackgroundColor() {
        return mBackgroundColor;
    }

    @Override
    protected void onMeasure(final int widthMeasureSpec, final int heightMeasureSpec) {

        final int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        final int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        final int heightSize = MeasureSpec.getSize(heightMeasureSpec);
        final int heightMode = MeasureSpec.getMode(heightMeasureSpec);

        // Check that values are not undefined
        if (widthMode == MeasureSpec.UNSPECIFIED || heightMode == MeasureSpec.UNSPECIFIED) {
            throw new IllegalArgumentException("Puzzle cannot have UNSPECIFIED dimensions");
        }

        setMeasuredDimension(widthSize, heightSize);

        if (LogConfig.FBP_PUZZLE_LOGS_ENABLED) {
            Log.d(LOG_TAG, "width: " + getMeasuredWidth() + " | height: " + getMeasuredHeight());
        }
    }

    @Override
    protected void onSizeChanged(final int w, final int h, final int oldw, final int oldh) {

        // Checks if compulsory parameters have been set
        // if (mMainViewHeight == -1) {
        // throw new CompulsoryParameterException("mainViewHeight has not been set");
        // } else if (mMainViewWidth == -1) {
        // throw new CompulsoryParameterException("mainViewWidth has not been set");
        // } else if (mAdapter == null) {
        // throw new CompulsoryParameterException("The Adapter has not been set");
        // }

        super.onSizeChanged(w, h, oldw, oldh);

        computeWorkingArea(w, h);
        computeTileWidth();
    }

    private void computeWorkingArea(final int w, final int h) {
        final int workingAreaSize = Math.min(w, h) / 4 * 4; // We want a multiple of 4 for easy sizing
        mWorkingAreaMinX = (w - workingAreaSize) / 2;
        mWorkingAreaMaxX = (w + workingAreaSize) / 2;
        mWorkingAreaMinY = (h - workingAreaSize) / 2;
        mWorkingAreaMaxY = (h + workingAreaSize) / 2;
    }

    private void computeTileWidth() {
        mTileWidth = (mWorkingAreaMaxX - mWorkingAreaMinX) / 4;
    }

    @Override
    protected void onLayout(final boolean changed, final int l, final int t, final int r, final int b) {
        mLayoutDone = true;
    }

    @Override
    protected void onDraw(final Canvas canvas) {
        super.onDraw(canvas);

        if (mBackgroundColor != Color.TRANSPARENT) {
            canvas.drawColor(mBackgroundColor);
        }

    }

    private class Tile {
        public int id;
        public Drawable drawable;

        public int anchorX;
        public int anchorY;
        public int column;
        public int row;

        public boolean canMoveLeft;
        public boolean canMoveTop;
        public boolean canMoveRight;
        public boolean canMoveDown;
    }
}
