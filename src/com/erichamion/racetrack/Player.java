package com.erichamion.racetrack;

/**
 * Created by me on 8/16/15.
 */
public class Player {
    private GridPoint mPosition;
    private GridPoint mVelocity = new GridPoint(0, 0);
    private boolean mIsCrashed = false;

    public Player(final GridPoint position) {
        setPos(position);
    }

    public Player(final int row, final int col) {
        mPosition = new GridPoint(row, col);
    }

    public GridPoint getPos() {
        return new GridPoint(mPosition);
    }

    /**
     * Return the position that will apply after the next move at the
     * current velocity. Does not complete the move, so the current
     * position remains unchanged.
     * @return Expected position after the next move
     */
    public GridPoint getNextPos() {
        return GridPoint.add(mPosition, mVelocity);
    }

    /**
     * Set this Player's position directly, regardless of current position
     * and velocity.
     * @param pos The new position
     */
    public void setPos(final GridPoint pos) {
        mPosition = new GridPoint(pos);
    }

    public GridPoint getVelocity() {
        return new GridPoint(mVelocity);
    }

    /**
     * Add the specified amounts to this Player's velocity. Changes only
     * velocity, not position.
     * @param acceleration A GridPoint containing the amounts to add to
     *                     the velocity in each dimension (row and column)
     */
    public void accelerate(final GridPoint acceleration) {
        mVelocity = GridPoint.add(mVelocity, acceleration);
    }

    /**
     * Update this Player's position based on its current velocity.
     */
    public void move() {
        mPosition = GridPoint.add(mPosition, mVelocity);
    }

    /**
     * Mark this Player as having crashed.
     */
    public void crash() {
        mIsCrashed = true;
    }

    /**
     * Determine whether this Player has been marked as crashed.
     * @return Returns true if crash has been called on this Player, false
     * otherwise.
     */
    public boolean isCrashed() {
        return mIsCrashed;
    }

}
