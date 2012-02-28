package com.foxykeep.fbpuzzle.widget;

import java.util.ArrayList;
import java.util.Random;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.animation.DecelerateInterpolator;
import android.widget.Scroller;

import com.foxykeep.fbpuzzle.config.LogConfig;

public class Puzzle extends View {

    /**
     * This listener is called when the puzzle is completed. It's not triggered by the resetPuzzle method
     * 
     * @author Foxykeep
     */
    public interface OnPuzzleCompletedListener {
        /**
         * Called when the puzzle is completed
         */
        public void onPuzzleCompleted();
    }

    private static final String LOG_TAG = Puzzle.class.getSimpleName();

    private static final int NB_PUZZLE_TILES = 16;
    private static final int NB_PUZZLE_DRAWABLES = 15;
    private static final int NB_COLS = 4;
    private static final int NB_ROWS = 4;

    private static final int TILE_PADDING_UNSCALED = 2;

    private static final int ID_EMPTY_TILE = 15;

    private static final int INVALID_STATE = -1;

    private static final int DIRECTION_LEFT = 0;
    private static final int DIRECTION_RIGHT = 1;
    private static final int DIRECTION_UP = 2;
    private static final int DIRECTION_DOWN = 3;

    private static final int VELOCITY_UNITS = 1000;
    private static final int FRAME_RATE = 1000 / 60;

    private static final int INVALID_ID = -1;

    // Variables set by the user
    private int mBackgroundColor;
    private int mEmptyTileColor;
    private Drawable[] mDrawableArray;

    // Other variables
    private final Paint mEmptyTilePaint = new Paint();

    private int mTileWidth;
    private int mTilePadding;
    private int mWorkingAreaMinX;
    private int mWorkingAreaMinY;
    private int mWorkingAreaSize;

    private final Tile[][] mTileArray = new Tile[4][4];
    private final SparseArray<Tile> mTileSparseArray = new SparseArray<Puzzle.Tile>();

    private VelocityTracker mVelocityTracker;
    private ViewConfiguration mViewConfiguration;
    private Scroller mScroller;
    private boolean mIsScrolling = false;
    private int mStartingX;
    private int mStartingY;
    private Tile mStartingTile;
    private int mStartingPointerId = INVALID_ID;
    private boolean mIsATap;
    private boolean mIsOvershooting;
    private boolean mIsOvershootingEndMovement;

    private boolean mHasMovementStarted = false;
    private boolean mFinishCurrentMovement = false;
    private int mMovementDirection = INVALID_STATE;
    private ArrayList<Tile> mOtherMovingTileList = new ArrayList<Tile>();

    private int mMinFlingVelocity;
    private int mMaxFlingVelocity;
    private int mTileSlop;

    private Random mRandom = new Random();

    private boolean mNoInvalidate;

    private OnPuzzleCompletedListener mOnPuzzleCompletedListener;

    /**
     * Indicates whether or not onSizeChanged has been done.
     */
    private boolean mLayoutDone = false;

    private Runnable mScrollerRunnable = new Runnable() {
        @Override
        public void run() {
            final Scroller scroller = mScroller;
            if (!scroller.isFinished()) {
                scroller.computeScrollOffset();

                int dx = scroller.getCurrX() - mStartingTile.anchorX;
                int dy = scroller.getCurrY() - mStartingTile.anchorY;

                mStartingTile.anchorX += dx;
                mStartingTile.anchorY += dy;

                if (mOtherMovingTileList.size() > 0) {
                    for (Tile tile : mOtherMovingTileList) {
                        tile.anchorX += dx;
                        tile.anchorY += dy;
                    }
                }

                postDelayed(this, FRAME_RATE);
                invalidate();
            } else {
                mIsScrolling = false;
                computeTilesNewCoordinates();
                computeTilesStartPosition();
                computeMovability();
                computePuzzleCompleted();

                stopAnimations();
            }
        }
    };

    public Puzzle(final Context context) {
        this(context, null);
    }

    public Puzzle(final Context context, final AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public Puzzle(final Context context, final AttributeSet attrs, final int defStyle) {
        super(context, attrs, defStyle);

        initPuzzle();
    }

    /**
     * Initialize the {@link Puzzle} with default values.
     */
    private void initPuzzle() {

        final Context context = getContext();

        setFocusable(true);
        setFocusableInTouchMode(true);
        requestFocus();

        final boolean tmpNoInvalidate = mNoInvalidate;
        mNoInvalidate = true;

        // General default properties
        setBackgroundColor(Color.TRANSPARENT);
        setEmptyTileColor(Color.DKGRAY);

        final float scale = getResources().getDisplayMetrics().density;
        mTilePadding = (int) (TILE_PADDING_UNSCALED * scale);

        mViewConfiguration = ViewConfiguration.get(context);
        mScroller = new Scroller(context, new DecelerateInterpolator());

        // Get the velocity
        mMinFlingVelocity = mViewConfiguration.getScaledMinimumFlingVelocity();
        mMaxFlingVelocity = mViewConfiguration.getScaledMaximumFlingVelocity();

        // Create the tiles
        for (int i = 0; i < NB_PUZZLE_TILES; i++) {
            mTileSparseArray.put(i, new Tile(i, i == ID_EMPTY_TILE));
        }
        resetPuzzle();

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
     * Defines the color of the empty tile.
     * 
     * @param color The color to set as the empty tile
     * @throw {@link IllegalStateException} When trying to change the value of this property when the component is already displayed on screen
     */
    public void setEmptyTileColor(final int color) {
        if (mLayoutDone) {
            throw new IllegalStateException("The background color cannot be changed once the component has been displayed");
        }
        if (color != mEmptyTileColor) {
            mEmptyTilePaint.setColor(color);
        }
        mEmptyTileColor = color;
    }

    /**
     * Returns the color of the empty tile.
     * 
     * @return An integer representing the color of the empty tile.
     */
    public int getEmptyTileColor() {
        return mEmptyTileColor;
    }

    /**
     * Set an OnPuzzleCompletedListener
     * 
     * @param listener an OnPuzzleCompletedListener
     */
    public void setOnPuzzleCompletedListener(final OnPuzzleCompletedListener listener) {
        mOnPuzzleCompletedListener = listener;
    }

    /**
     * Get the OnPuzzleCompletedListener associated to this puzzle if any
     * 
     * @return the OnPuzzleCompletedListener associated to this puzzle or null otherwise
     */
    public OnPuzzleCompletedListener getOnPuzzleCompletedListener() {
        return mOnPuzzleCompletedListener;
    }

    /**
     * Set the list of {@link Drawable}s for the puzzle. It should be a list of 15 {@link Drawable}s.
     * <p>
     * If there are more, we will take only the first 15. If there are less, we will loop until we have 15.
     * 
     * @param drawableList an ArrayList of {@link Drawable}s for the puzzle
     */
    public void setDrawableList(final ArrayList<Drawable> drawableList) {
        if (drawableList == null || drawableList.isEmpty()) {
            return;
        }
        final int drawableListSize = drawableList.size();
        int nbDrawableSet = 0;

        do {
            for (int i = 0; i < drawableListSize && i < NB_PUZZLE_DRAWABLES; i++) {
                if (nbDrawableSet == ID_EMPTY_TILE) {
                    mDrawableArray[nbDrawableSet] = null;
                    nbDrawableSet++;
                }
                if (mDrawableArray == null) {
                    mDrawableArray = new Drawable[NB_PUZZLE_TILES];
                }
                mDrawableArray[nbDrawableSet++] = drawableList.get(i);
            }
        } while (nbDrawableSet < NB_PUZZLE_DRAWABLES);

        for (int i = 0, j = 0; i < NB_PUZZLE_TILES; i++) {
            Tile tile = mTileSparseArray.get(i);
            if (!tile.isEmptyTile) {
                tile.drawable = mDrawableArray[j++];
            }
        }

        invalidate();
    }

    /**
     * Reset the puzzle to the correct order
     */
    public void resetPuzzle() {
        for (int i = 0; i < NB_PUZZLE_TILES; i++) {
            final Tile tile = mTileSparseArray.get(i);
            tile.column = i % 4;
            tile.row = i / 4;
            mTileArray[tile.column][tile.row] = tile;
        }

        computeMovability();
        computeTilesStartPosition();

        invalidate();
    }

    public void scramblePuzzle() {
        do {
            scramble();
        } while (!checkIfSolvable());

        computeMovability();
        computeTilesStartPosition();

        invalidate();
    }

    private void scramble() {
        int nbScrambles = 0;
        while (nbScrambles < 25) {
            final int i1 = mRandom.nextInt(NB_PUZZLE_TILES);
            final int i2 = mRandom.nextInt(NB_PUZZLE_TILES);

            if (i1 != i2) {
                final Tile tile1 = mTileSparseArray.get(i1);
                final Tile tile2 = mTileSparseArray.get(i2);
                swapTiles(tile1, tile2);
            }

            nbScrambles++;
        }
    }

    private void setTileOrder(final int[] tileOrder) {
        if (tileOrder.length != NB_PUZZLE_TILES) {
            return;
        }
        for (int i = 0; i < NB_PUZZLE_TILES; i++) {
            final int index = tileOrder[i];
            final Tile tile = mTileSparseArray.get(i);
            tile.column = index % 4;
            tile.row = index / 4;
            mTileArray[tile.column][tile.row] = tile;
        }

        computeMovability();
        computeTilesStartPosition();

        invalidate();
    }

    private boolean checkIfSolvable() {
        // Tiles solvability algorithm based on http://www.cs.bham.ac.uk/~mdr/teaching/modules04/java2/TilesSolvability.html

        int nbInvert = 0;
        Tile emptyTile = null;
        Tile[][] tileArray = mTileArray;

        for (int j = 0; j < NB_ROWS; j++) {
            for (int i = 0; i < NB_COLS; i++) {
                final int id = tileArray[i][j].id;
                if (id == ID_EMPTY_TILE) {
                    emptyTile = tileArray[i][j];
                } else {
                    for (int n = i + j * 4; n < NB_PUZZLE_TILES; n++) {
                        int nextId = tileArray[n % 4][n / 4].id;
                        if (nextId < id) {
                            nbInvert++;
                        }
                    }
                }
            }
        }

        return nbInvert % 2 == (emptyTile.row + 1) % 2;
    }

    private void computeTilesStartPosition() {
        for (int i = 0; i < NB_COLS; i++) {
            for (int j = 0; j < NB_ROWS; j++) {
                final Tile tile = mTileArray[i][j];
                if (tile == null) {
                    continue;
                }
                tile.anchorX = tile.startAnchorX = mWorkingAreaMinX + i * mTileWidth;
                tile.anchorY = tile.startAnchorY = mWorkingAreaMinY + j * mTileWidth;
            }
        }

    }

    @Override
    protected void onMeasure(final int widthMeasureSpec, final int heightMeasureSpec) {

        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        final int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);
        final int heightMode = MeasureSpec.getMode(heightMeasureSpec);

        int h = 0, w = 0;
        // Check that values are not undefined
        if (widthMode == MeasureSpec.UNSPECIFIED && heightMode == MeasureSpec.UNSPECIFIED) {
            if (mDrawableArray != null && mDrawableArray.length > 0) {
                h = w = Math.max(mDrawableArray[0].getIntrinsicHeight(), mDrawableArray[0].getIntrinsicWidth()) * 4;
            }
        } else if (widthMode == MeasureSpec.UNSPECIFIED) {
            h = w = heightSize;
        } else if (heightMode == MeasureSpec.UNSPECIFIED) {
            h = w = widthSize;
        }

        widthSize = resolveSize(h, widthMeasureSpec);
        heightSize = resolveSize(w, heightMeasureSpec);

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
        computeTilesStartPosition();
    }

    private void computeWorkingArea(final int w, final int h) {
        final int workingAreaSize = Math.min(w, h) / 4 * 4; // We want a multiple of 4 for easy sizing
        mWorkingAreaMinX = (w - workingAreaSize) / 2;
        mWorkingAreaMinY = (h - workingAreaSize) / 2;
        mWorkingAreaSize = workingAreaSize;

        Log.d(LOG_TAG, "mWorkingArea | x min : " + mWorkingAreaMinX + " | y min : " + mWorkingAreaMinY);
    }

    private void computeTileWidth() {
        mTileWidth = (mWorkingAreaSize - mTilePadding * 2) / 4;
        mTileSlop = mTileWidth / 2;

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
        final int tilePadding = mTilePadding;

        if (mBackgroundColor != Color.TRANSPARENT) {
            canvas.drawColor(mBackgroundColor);
        }

        final int workingAreaMinX = mWorkingAreaMinX;
        final int workingAreaMinY = mWorkingAreaMinY;
        final int workingAreaSize = mWorkingAreaSize;
        canvas.drawRect(new Rect(workingAreaMinX, workingAreaMinY, workingAreaMinX + workingAreaSize, workingAreaMinY + workingAreaSize),
                mEmptyTilePaint);

        canvas.translate(tilePadding, tilePadding);
        for (int i = 0; i < NB_COLS; i++) {
            for (int j = 0; j < NB_ROWS; j++) {
                final Tile tile = tileArray[i][j];
                canvas.save();
                if (tile.drawable != null) {
                    tile.drawable.setBounds(tile.anchorX + tilePadding, tile.anchorY + tilePadding, tile.anchorX + mTileWidth - tilePadding,
                            tile.anchorY + mTileWidth - tilePadding);
                    tile.drawable.draw(canvas);
                }
                canvas.restore();
            }
        }
        canvas.restore();
    }

    private void computeMovability() {
        for (int i = 0; i < NB_COLS; i++) {
            for (int j = 0; j < NB_ROWS; j++) {
                Tile tile = mTileArray[i][j];
                if (tile.id == ID_EMPTY_TILE) {
                    tile.canMoveLeft = false;
                    tile.canMoveRight = false;
                    tile.canMoveUp = false;
                    tile.canMoveDown = false;
                } else {
                    tile.canMoveLeft = canMoveFromPositionToDirection(i, j, DIRECTION_LEFT);
                    tile.canMoveRight = canMoveFromPositionToDirection(i, j, DIRECTION_RIGHT);
                    tile.canMoveUp = canMoveFromPositionToDirection(i, j, DIRECTION_UP);
                    tile.canMoveDown = canMoveFromPositionToDirection(i, j, DIRECTION_DOWN);
                }
            }
        }
    }

    private boolean canMoveFromPositionToDirection(int i, int j, final int direction) {
        switch (direction) {
            case DIRECTION_LEFT:
                if (--i < 0) {
                    return false;
                }
                break;
            case DIRECTION_RIGHT:
                if (++i >= NB_COLS) {
                    return false;
                }
                break;
            case DIRECTION_UP:
                if (--j < 0) {
                    return false;
                }
                break;
            case DIRECTION_DOWN:
                if (++j >= NB_ROWS) {
                    return false;
                }
                break;
        }

        if (mTileArray[i][j].isEmptyTile) {
            return true;
        }

        return canMoveFromPositionToDirection(i, j, direction);
    }

    @Override
    public boolean onTouchEvent(final MotionEvent event) {

        if (mIsScrolling) {
            return true;
        }

        final int pointerIndex = event.getActionIndex();
        final int id = event.getPointerId(pointerIndex);
        if (mStartingPointerId == INVALID_ID) {
            mStartingPointerId = id;
        } else if (mStartingPointerId != id) {
            // Not the first touch event on the screen so we do nothing with it
            return true;
        }

        final int action = event.getActionMasked();
        final int x = (int) event.getX(id);
        final int y = (int) event.getY(id);

        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
        mVelocityTracker.addMovement(event);

        switch (action) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN:

                // Removes callbacks to the Runnables handling scrolling and
                // flinging and sets the new center, ...
                stopAnimations();

                // Sets the computing variables
                mStartingX = x;
                mStartingY = y;
                mIsATap = true;
                mIsOvershooting = false;
                computeStartingPosition();

                invalidate();
                break;
            case MotionEvent.ACTION_MOVE:
                if (mStartingTile == null) {
                    break;
                }
                // A tap is considered as a tap if the user hasn't moved its
                // finger over a certain threshold (found in ViewConfiguration)
                if (mIsATap) {
                    double p1 = Math.pow(mStartingX - x, 2);
                    double p2 = Math.pow(mStartingY - y, 2);
                    if (Math.sqrt(p1 + p2) > mViewConfiguration.getScaledTouchSlop()) {
                        mIsATap = false;
                    }
                }

                computeTilesNewPosition(x, y);

                invalidate();
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:

                if (mStartingTile == null) {
                    break;
                }
                if (mIsATap) {
                    // The user made a tap, we just have to scroll the taped
                    // item above the focus bar.
                    handleTap();
                    break;
                }

                if (mIsOvershooting) {
                    mFinishCurrentMovement = mIsOvershootingEndMovement;
                } else {
                    // Let's look for a fling gesture
                    final VelocityTracker velocityTracker = mVelocityTracker;
                    velocityTracker.computeCurrentVelocity(VELOCITY_UNITS, mMaxFlingVelocity);

                    if (mMovementDirection == DIRECTION_LEFT || mMovementDirection == DIRECTION_RIGHT) {
                        if (Math.abs(mStartingX - x) > mTileSlop) {
                            mFinishCurrentMovement = true;
                        } else if (Math.abs((int) velocityTracker.getXVelocity(id)) > mMinFlingVelocity) {
                            mFinishCurrentMovement = true;
                        } else {
                            mFinishCurrentMovement = false;
                        }
                    } else if (mMovementDirection == DIRECTION_UP || mMovementDirection == DIRECTION_DOWN) {
                        if (Math.abs(mStartingY - y) > mTileSlop) {
                            mFinishCurrentMovement = true;
                        } else if (Math.abs((int) velocityTracker.getYVelocity(id)) > mMinFlingVelocity) {
                            mFinishCurrentMovement = true;
                        } else {
                            mFinishCurrentMovement = false;
                        }
                    }
                }

                scrollAfterMovement();

                if (mVelocityTracker != null) {
                    mVelocityTracker.recycle();
                    mVelocityTracker = null;
                }

                break;

        }

        return true;
    }

    private void stopAnimations() {
        removeCallbacks(mScrollerRunnable);
        mScroller.forceFinished(true);

        mStartingTile = null;
        mOtherMovingTileList.clear();
        mMovementDirection = INVALID_STATE;
        mHasMovementStarted = false;
    }

    private void computeStartingPosition() {
        final int startingX = mStartingX;
        final int startingY = mStartingY;

        final int startingColumn = (int) Math.floor((startingX - mWorkingAreaMinX) / (float) mTileWidth);
        final int startingRow = (int) Math.floor((startingY - mWorkingAreaMinY) / (float) mTileWidth);

        if (startingColumn < 0 || startingColumn >= NB_COLS || startingRow < 0 || startingRow >= NB_ROWS) {
            mStartingTile = null;
        } else {
            mStartingTile = mTileArray[startingColumn][startingRow];
        }
    }

    private void computeTilesNewPosition(final int x, final int y) {

        final Tile startingTile = mStartingTile;
        final ArrayList<Tile> otherMovingTileList = mOtherMovingTileList;
        int moveX = mStartingX - x;
        int moveY = mStartingY - y;

        if (!mHasMovementStarted) {
            // We are not already in a movement
            // Check if the movement is doing something (ie if the tile can move in the current direction)
            if (startingTile.canMoveLeft && moveX > 0) {
                mMovementDirection = DIRECTION_LEFT;
                mHasMovementStarted = true;
                computeOtherMovingTiles();
            } else if (startingTile.canMoveRight && moveX < 0) {
                mMovementDirection = DIRECTION_RIGHT;
                mHasMovementStarted = true;
                computeOtherMovingTiles();
            } else if (startingTile.canMoveUp && moveY > 0) {
                mMovementDirection = DIRECTION_UP;
                mHasMovementStarted = true;
                computeOtherMovingTiles();
            } else if (startingTile.canMoveDown && moveY < 0) {
                mMovementDirection = DIRECTION_DOWN;
                mHasMovementStarted = true;
                computeOtherMovingTiles();
            }
        }

        if (mMovementDirection == DIRECTION_LEFT || mMovementDirection == DIRECTION_RIGHT) {
            if (Math.abs(moveX) > mTileWidth) {
                if (!mIsOvershooting) {
                    // First overshooting
                    moveX = moveX > 0 ? mTileWidth : -mTileWidth;
                    mIsOvershooting = true;
                    mIsOvershootingEndMovement = true;
                } else {
                    // Overshooting
                    return;
                }
            } else if ((moveX < 0 && mMovementDirection == DIRECTION_LEFT) || (moveX > 0 && mMovementDirection == DIRECTION_RIGHT)) {
                if (!mIsOvershooting) {
                    // First overshooting
                    moveX = 0;
                    mIsOvershooting = true;
                    mIsOvershootingEndMovement = false;
                } else {
                    // Subsequent overshooting
                    return;
                }
            } else {
                mIsOvershooting = false;
            }

            mStartingTile.anchorX = mStartingTile.startAnchorX - moveX;
            if (otherMovingTileList.size() > 0) {
                for (Tile tile : otherMovingTileList) {
                    tile.anchorX = tile.startAnchorX - moveX;
                }
            }
        } else if (mMovementDirection == DIRECTION_UP || mMovementDirection == DIRECTION_DOWN) {
            if (Math.abs(moveY) > mTileWidth) {
                if (!mIsOvershooting) {
                    // First overshooting
                    moveY = moveY > 0 ? mTileWidth : -mTileWidth;
                    mIsOvershooting = true;
                    mIsOvershootingEndMovement = true;
                } else {
                    // Overshooting
                    return;
                }
            } else if ((moveY < 0 && mMovementDirection == DIRECTION_UP) || (moveY > 0 && mMovementDirection == DIRECTION_DOWN)) {
                if (!mIsOvershooting) {
                    // First overshooting
                    moveY = 0;
                    mIsOvershooting = true;
                    mIsOvershootingEndMovement = false;
                } else {
                    // Subsequent overshooting
                    return;
                }
            } else {
                mIsOvershooting = false;
            }

            mStartingTile.anchorY = mStartingTile.startAnchorY - moveY;
            if (otherMovingTileList.size() > 0) {
                for (Tile tile : otherMovingTileList) {
                    tile.anchorY = tile.startAnchorY - moveY;
                }
            }
        }
    }

    private void computeOtherMovingTiles() {
        final Tile startingTile = mStartingTile;
        final ArrayList<Tile> otherMovingTileList = mOtherMovingTileList;
        otherMovingTileList.clear();
        final Tile[][] tileArray = mTileArray;

        switch (mMovementDirection) {
            case DIRECTION_LEFT:
                for (int i = startingTile.column - 1; i > 0; i--) {
                    final Tile tile = tileArray[i][startingTile.row];
                    if (tile.isEmptyTile) {
                        break;
                    } else {
                        otherMovingTileList.add(tile);
                    }
                }
                break;
            case DIRECTION_RIGHT:
                for (int i = startingTile.column + 1; i < NB_COLS; i++) {
                    final Tile tile = tileArray[i][startingTile.row];
                    if (tile.isEmptyTile) {
                        break;
                    } else {
                        otherMovingTileList.add(tile);
                    }
                }
                break;
            case DIRECTION_UP:
                for (int j = startingTile.row - 1; j > 0; j--) {
                    final Tile tile = tileArray[startingTile.column][j];
                    if (tile.isEmptyTile) {
                        break;
                    } else {
                        otherMovingTileList.add(tile);
                    }
                }
                break;
            case DIRECTION_DOWN:
                for (int j = startingTile.row + 1; j < NB_ROWS; j++) {
                    final Tile tile = tileArray[startingTile.column][j];
                    if (tile.isEmptyTile) {
                        break;
                    } else {
                        otherMovingTileList.add(tile);
                    }
                }
                break;
        }
    }

    /**
     * @param finishCurrentMovement if finishCurrentMovement is true, we move the moving tiles to the direction given by mMovementDirection.
     *            otherwise, we reset the moving tiles to their origin position
     * @param x The current x position
     * @param y The current y position
     */
    private void scrollAfterMovement() {

        final boolean finishCurrentMovement = mFinishCurrentMovement;
        int dx = 0, dy = 0;
        switch (mMovementDirection) {
            case DIRECTION_LEFT:
                dx = finishCurrentMovement ? (mStartingTile.startAnchorX - mStartingTile.anchorX) - mTileWidth : mStartingTile.startAnchorX
                        - mStartingTile.anchorX;
                dy = 0;
                break;
            case DIRECTION_RIGHT:
                dx = finishCurrentMovement ? (mStartingTile.startAnchorX - mStartingTile.anchorX) + mTileWidth : mStartingTile.startAnchorX
                        - mStartingTile.anchorX;
                dy = 0;
                break;
            case DIRECTION_UP:
                dx = 0;
                dy = finishCurrentMovement ? (mStartingTile.startAnchorY - mStartingTile.anchorY) - mTileWidth : mStartingTile.startAnchorY
                        - mStartingTile.anchorY;
                break;
            case DIRECTION_DOWN:
                dx = 0;
                dy = finishCurrentMovement ? (mStartingTile.startAnchorY - mStartingTile.anchorY) + mTileWidth : mStartingTile.startAnchorY
                        - mStartingTile.anchorY;
                break;
        }

        if (dx == 0 && dy == 0) {
            computeTilesNewCoordinates();
            computeTilesStartPosition();
            computeMovability();
            computePuzzleCompleted();
            return;
        }

        mIsScrolling = true;
        mScroller.startScroll(mStartingTile.anchorX, mStartingTile.anchorY, dx, dy);
        post(mScrollerRunnable);
    }

    private void computeTilesNewCoordinates() {
        if (!mFinishCurrentMovement) {
            return;
        }

        final Tile startingTile = mStartingTile;
        final ArrayList<Tile> otherMovingTileList = mOtherMovingTileList;

        int dcolumn = 0;
        int drow = 0;
        switch (mMovementDirection) {
            case DIRECTION_LEFT:
                dcolumn = -1;
                break;
            case DIRECTION_RIGHT:
                dcolumn = 1;
                break;
            case DIRECTION_UP:
                drow = -1;
                break;
            case DIRECTION_DOWN:
                drow = 1;
                break;
        }

        startingTile.newColumn = startingTile.column + dcolumn;
        startingTile.newRow = startingTile.row + drow;

        if (otherMovingTileList.size() > 0) {
            for (Tile tile : otherMovingTileList) {
                tile.newColumn = tile.column + dcolumn;
                tile.newRow = tile.row + drow;
            }
        }

        swapTiles(startingTile);

        if (otherMovingTileList.size() > 0) {
            for (Tile tile : otherMovingTileList) {
                swapTiles(tile);
            }
        }
    }

    private void computePuzzleCompleted() {
        if (mOnPuzzleCompletedListener != null) {
            for (int i = 0; i < NB_PUZZLE_TILES; i++) {
                final Tile tile = mTileSparseArray.get(i);
                if (tile.column + tile.row * 4 != tile.id) {
                    // The tile is not in its right position
                    return;
                }
            }

            // Every tiles are in their right position
            mOnPuzzleCompletedListener.onPuzzleCompleted();
        }
    }

    private void swapTiles(final Tile tile1) {
        final Tile tile2 = mTileArray[tile1.newColumn][tile1.newRow];
        swapTiles(tile1, tile2);
    }

    private void swapTiles(final Tile tile1, final Tile tile2) {
        final Tile[][] tileArray = mTileArray;

        int save;
        save = tile1.column;
        tile1.column = tile2.column;
        tile2.column = save;

        save = tile1.row;
        tile1.row = tile2.row;
        tile2.row = save;

        tileArray[tile2.column][tile2.row] = tile2;
        tileArray[tile1.column][tile1.row] = tile1;
    }

    private void handleTap() {
        final Tile startingTile = mStartingTile;

        boolean tapMovement = false;
        if (startingTile.canMoveLeft) {
            mMovementDirection = DIRECTION_LEFT;
            tapMovement = true;
            computeOtherMovingTiles();
        } else if (startingTile.canMoveRight) {
            mMovementDirection = DIRECTION_RIGHT;
            tapMovement = true;
            computeOtherMovingTiles();
        } else if (startingTile.canMoveUp) {
            mMovementDirection = DIRECTION_UP;
            tapMovement = true;
            computeOtherMovingTiles();
        } else if (startingTile.canMoveDown) {
            mMovementDirection = DIRECTION_DOWN;
            tapMovement = true;
            computeOtherMovingTiles();
        }

        if (tapMovement) {
            computeOtherMovingTiles();
            mFinishCurrentMovement = true;
            scrollAfterMovement();
        }
    }

    private class Tile {
        public int id;
        public Drawable drawable = null;
        public boolean isEmptyTile;

        public int anchorX = INVALID_STATE;
        public int anchorY = INVALID_STATE;
        public int startAnchorX = INVALID_STATE;
        public int startAnchorY = INVALID_STATE;

        public int row = INVALID_STATE;
        public int column = INVALID_STATE;
        public int newRow = INVALID_STATE;
        public int newColumn = INVALID_STATE;

        public boolean canMoveLeft = false;
        public boolean canMoveRight = false;
        public boolean canMoveUp = false;
        public boolean canMoveDown = false;

        public Tile(final int id, final boolean isEmptyTile) {
            this.id = id;
            this.isEmptyTile = isEmptyTile;
        }
    }

    static class SavedState extends BaseSavedState {
        int[] tileOrder;

        SavedState(final Parcelable superState) {
            super(superState);
        }

        private SavedState(final Parcel in) {
            super(in);
            tileOrder = new int[in.readInt()];
            in.readIntArray(tileOrder);
        }

        @Override
        public void writeToParcel(final Parcel out, final int flags) {
            super.writeToParcel(out, flags);
            out.writeInt(tileOrder.length);
            out.writeIntArray(tileOrder);
        }

        public static final Parcelable.Creator<SavedState> CREATOR = new Parcelable.Creator<SavedState>() {
            public SavedState createFromParcel(final Parcel in) {
                return new SavedState(in);
            }

            public SavedState[] newArray(final int size) {
                return new SavedState[size];
            }
        };
    }

    @Override
    public Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        SavedState ss = new SavedState(superState);

        ss.tileOrder = new int[NB_PUZZLE_TILES];

        for (int i = 0; i < NB_PUZZLE_TILES; i++) {
            final Tile tile = mTileSparseArray.get(i);
            ss.tileOrder[tile.id] = tile.row * 4 + tile.column;
        }

        return ss;
    }

    @Override
    public void onRestoreInstanceState(final Parcelable state) {
        SavedState ss = (SavedState) state;
        super.onRestoreInstanceState(ss.getSuperState());

        setTileOrder(ss.tileOrder);
    }
}
