package com.foxykeep.fbpuzzle.widget;

import java.util.ArrayList;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewConfiguration;

import com.foxykeep.fbpuzzle.config.LogConfig;

public class Puzzle extends View {

    private static final String LOG_TAG = Puzzle.class.getSimpleName();

    private static final int NB_PUZZLE_TILES = 15;
    private static final int NB_COLS = 4;
    private static final int NB_ROWS = 4;

    private static final int LAST_TILE_INDEX = 15;

    private static final int INVALID_POS = -1;

    private static final int DIRECTION_LEFT = 0;
    private static final int DIRECTION_RIGHT = 1;
    private static final int DIRECTION_UP = 2;
    private static final int DIRECTION_DOWN = 3;

    // Variables set by the user
    private int mBackgroundColor;
    private Drawable[] mDrawableArray;

    // Other variables
    private final Paint mPaint = new Paint();

    private int mTileWidth;
    private int mWorkingAreaMinX;
    private int mWorkingAreaMaxX;
    private int mWorkingAreaMinY;

    private final Tile[][] mTileArray = new Tile[4][4];
    private final SparseArray<Tile> mTileSparseArray = new SparseArray<Puzzle.Tile>();

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
        setBackgroundColor(Color.DKGRAY);

        // Create the tiles
        for (int i = 0; i < NB_COLS; i++) {
            for (int j = 0; j < NB_ROWS; j++) {

                Tile tile = null;

                final int index = i + j * NB_COLS;
                if (index != LAST_TILE_INDEX) {
                    tile = new Tile(index);
                    tile.row = i;
                    tile.column = j;

                    mTileSparseArray.put(index, tile);
                }

                mTileArray[i][j] = tile;
            }
        }

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

    /**
     * Set the list of {@link Drawable}s for the puzzle. It should be a list of 15 {@link Drawable}s.
     * <p>
     * If there are more, we will take only the first 15. If there are less, we will loop until we have 15.
     * 
     * @param drawableList an ArrayList of {@link Drawable}s for the puzzle
     */
    public void setDrawableList(final ArrayList<Drawable> drawableList) {
        final int drawableListSize = drawableList.size();
        int nbDrawableSet = 0;

        do {
            for (int i = 0; i < drawableListSize && i < NB_PUZZLE_TILES; i++) {
                if (mDrawableArray == null) {
                    mDrawableArray = new Drawable[15];
                }
                mDrawableArray[nbDrawableSet++] = drawableList.get(i);
            }
        } while (nbDrawableSet < NB_PUZZLE_TILES);

        resetPuzzle();
    }

    /**
     * Give an array of {@link Drawable}s for the images of the puzzle. Must have exactly 15 things or an {@link IllegalArgumentException} will be
     * thrown
     * 
     * @param drawableArray an array of {@link Drawable}s for the images of the puzzle
     */
    public void setDrawableArray(final Drawable[] drawableArray) {
        if (drawableArray == null || drawableArray.length != 15) {
            throw new IllegalArgumentException("The given drawableArray must be not null and exactly 15 Drawables long");
        }

        mDrawableArray = drawableArray;

        resetPuzzle();
    }

    /**
     * Reset the puzzle to the correct order
     */
    public void resetPuzzle() {
        for (int i = 0; i < NB_COLS; i++) {
            for (int j = 0; j < NB_ROWS; j++) {

                final Tile tile;

                final int index = i + j * NB_COLS;
                if (index != LAST_TILE_INDEX) {
                    tile = mTileSparseArray.get(index);
                    tile.drawable = mDrawableArray[index];
                    tile.row = i;
                    tile.column = j;
                } else {
                    tile = null;
                }
                mTileArray[i][j] = tile;
            }
        }

        computeMovability();
        computeTileFixedPosition();

        invalidate();
    }

    private void computeTileFixedPosition() {
        for (int i = 0; i < NB_COLS; i++) {
            for (int j = 0; j < NB_ROWS; j++) {
                final Tile tile = mTileArray[i][j];
                if (tile == null) {
                    continue;
                }
                tile.anchorX = mWorkingAreaMinX + i * mTileWidth;
                tile.anchorY = mWorkingAreaMinY + j * mTileWidth;
            }
        }

    }

    @Override
    protected void onMeasure(final int widthMeasureSpec, final int heightMeasureSpec) {

        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        final int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);
        final int heightMode = MeasureSpec.getMode(heightMeasureSpec);

        // Check that values are not undefined
        if (widthMode == MeasureSpec.UNSPECIFIED && heightMode == MeasureSpec.UNSPECIFIED) {
            throw new IllegalArgumentException("Puzzle cannot have both UNSPECIFIED dimensions");
        }

        if (widthMode == MeasureSpec.UNSPECIFIED) {
            widthSize = resolveSizeAndState(heightSize, widthMeasureSpec, 0);
        } else if (heightMode == MeasureSpec.UNSPECIFIED) {
            heightSize = resolveSizeAndState(widthSize, heightMeasureSpec, 0);
        }

        setMeasuredDimension(widthSize, heightSize);

        if (LogConfig.FBP_PUZZLE_LOGS_ENABLED) {
            Log.d(LOG_TAG, "width: " + getMeasuredWidth() + " | height: " + getMeasuredHeight());
        }
    }

    @Override
    protected void onSizeChanged(final int w, final int h, final int oldw, final int oldh) {

        super.onSizeChanged(w, h, oldw, oldh);

        computeWorkingArea(w, h);
        computeTileWidth();
        computeTileFixedPosition();
    }

    private void computeWorkingArea(final int w, final int h) {
        final int workingAreaSize = Math.min(w, h) / 4 * 4; // We want a multiple of 4 for easy sizing
        mWorkingAreaMinX = (w - workingAreaSize) / 2;
        mWorkingAreaMaxX = (w + workingAreaSize) / 2;
        mWorkingAreaMinY = (h - workingAreaSize) / 2;

        Log.d(LOG_TAG, "mWorkingArea | x (min,max) : " + mWorkingAreaMinX + " " + mWorkingAreaMaxX + " | y min : " + mWorkingAreaMinY);
    }

    private void computeTileWidth() {
        mTileWidth = (mWorkingAreaMaxX - mWorkingAreaMinX) / 4;

        Log.d(LOG_TAG, "mTileWidth : " + mTileWidth);
    }

    @Override
    protected void onLayout(final boolean changed, final int l, final int t, final int r, final int b) {
        mLayoutDone = true;
    }

    @Override
    protected void onDraw(final Canvas canvas) {
        super.onDraw(canvas);

        final Tile[][] tileArray = mTileArray;

        if (mBackgroundColor != Color.TRANSPARENT) {
            canvas.drawColor(mBackgroundColor);
        }

        for (int i = 0; i < NB_COLS; i++) {
            for (int j = 0; j < NB_ROWS; j++) {
                final Tile tile = tileArray[i][j];
                if (tile == null) {
                    continue;
                }
                canvas.save();
                tile.drawable.setBounds(tile.anchorX, tile.anchorY, tile.anchorX + mTileWidth, tile.anchorY + mTileWidth);
                tile.drawable.draw(canvas);
                canvas.restore();
            }
        }
    }

    private void computeMovability() {
        for (int i = 0; i < NB_COLS; i++) {
            for (int j = 0; j < NB_ROWS; j++) {
                Tile tile = mTileArray[i][j];
                if (tile == null) {
                    continue;
                }
                tile.canMoveLeft = canMoveFromPositionToDirection(i, j, DIRECTION_LEFT);
                tile.canMoveRight = canMoveFromPositionToDirection(i, j, DIRECTION_RIGHT);
                tile.canMoveUp = canMoveFromPositionToDirection(i, j, DIRECTION_UP);
                tile.canMoveDown = canMoveFromPositionToDirection(i, j, DIRECTION_DOWN);
            }
        }
    }

    private boolean canMoveFromPositionToDirection(final int i, final int j, final int direction) {
        int x = i;
        int y = j;

        switch (direction) {
            case DIRECTION_LEFT:
                if (--x < 0) {
                    return false;
                }
                break;
            case DIRECTION_RIGHT:
                if (++x >= NB_COLS) {
                    return false;
                }
                break;
            case DIRECTION_UP:
                if (--y < 0) {
                    return false;
                }
                break;
            case DIRECTION_DOWN:
                if (++y >= NB_ROWS) {
                    return false;
                }
                break;
        }

        if (mTileArray[x][y] == null) {
            return true;
        }

        return canMoveFromPositionToDirection(x, y, direction);
    }

    private class Tile {
        public int id;
        public Drawable drawable = null;

        public int anchorX = INVALID_POS;
        public int anchorY = INVALID_POS;
        public int row = INVALID_POS;
        public int column = INVALID_POS;

        public boolean canMoveLeft = false;
        public boolean canMoveRight = false;
        public boolean canMoveUp = false;
        public boolean canMoveDown = false;

        public Tile(final int id) {
            this.id = id;
        }
    }
}
