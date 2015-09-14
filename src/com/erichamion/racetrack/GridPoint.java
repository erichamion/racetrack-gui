package com.erichamion.racetrack;

/**
 * Started as a simple class to hold a point in a (row, column) grid
 * system, but now also used for vectors and a small set of vector
 * operations in the same (row, column) coordinate system. I'm not sure of
 * a better name that captures both the point and vector usage.
 *
 * Created by me on 8/17/15.
 */
public final class GridPoint implements Cloneable {
    private int mRow;
    private int mCol;



    public enum Axis {
        ROW,
        COL
    }

    /**
     * Adds two vectors or a point and a vector together, returning a new
     * GridPoint.
     * @param obj1 A point or a vector
     * @param obj2 A point or a vector
     * @return A new GridPoint holding the result of the addition. If both
     * arguments are points (not vectors), the result is mathematically
     * correct but meaningless.
     */
    public static GridPoint add(final GridPoint obj1, final GridPoint obj2) {
        return new GridPoint(obj1.getRow() + obj2.getRow(), obj1.getCol() + obj2.getCol());
    }

    /**
     * Subtracts obj2 from obj1.
     * @param obj1 The GridPoint from which to subtract obj2
     * @param obj2 The GridPoint to subtract from obj1
     * @return The result of (obj1 - obj2).
     */
    public static GridPoint subtract(final GridPoint obj1, final GridPoint obj2) {
        return new GridPoint(obj1.getRow() - obj2.getRow(), obj1.getCol() - obj2.getCol());
    }

    /**
     * Returns the dot product of two 2D vectors. The dot product
     * multiplies the lengths of the parallel components of the vectors.
     * @param vectorA A GridPoint representing a vector
     * @param vectorB A GridPoint representing a vector
     * @return The dot product (vectorA * vectorB). Since vectorA and
     * vectorB are GridPoints, and GridPoints hold only integer
     * coordinates, the resulting dot product is an integer.
     */
    public static int dotProduct(final GridPoint vectorA, final GridPoint vectorB) {
        return (vectorA.getRow() * vectorB.getRow()) + (vectorA.getCol() * vectorB.getCol());
    }

    /**
     * Returns the dot product of the unit vectors associated with two
     * specified 2D vectors (a unit vector has the same direction as the
     * given vector, but a length of 1). The unit vector dot product is a
     * measure of how parallel the vectors are. Parallel unit vectors have
     * a dot product of 1, perpendicular vectors have a dot product of 0,
     * and parallel but opposite unit vectors have a dot product of -1.
     * @param vectorA A GridPoint representing a vector
     * @param vectorB A GridPoint representing a vector
     * @return The dot product of the two unit vectors associated with
     * vectorA and vectorB. Although vectorA and vectorB have only
     * integer coordinates, their corresponding unit vectors may have
     * non-integer coordinates, and therefore the dot product is not
     * generally an integer.
     */
    public static double unitDotProduct(final GridPoint vectorA, final GridPoint vectorB) {
        int dot = dotProduct(vectorA, vectorB);
        double lengthASquared = Math.pow(vectorA.getRow(), 2) + Math.pow(vectorA.getCol(), 2);
        double lengthBSquared = Math.pow(vectorB.getRow(), 2) + Math.pow(vectorB.getCol(), 2);
        return dot / Math.sqrt(lengthASquared * lengthBSquared);
    }



    public GridPoint(final int row, final int col) {
        mRow = row;
        mCol = col;
    }

    public GridPoint(final GridPoint other) {
        mRow = other.getRow();
        mCol = other.getCol();
    }

    public GridPoint() {
        mRow = 0;
        mCol = 0;
    }

    public int getRow() {
        return mRow;
    }

    public void setRow(final int row) {
        this.mRow = row;
    }

    public int getCol() {
        return mCol;
    }

    public void setCol(final int col) {
        this.mCol = col;
    }

    public int getValueOnAxis(final Axis axis) {
        return (axis == Axis.ROW) ? mRow : mCol;
    }

    public void setValueOnAxis(final Axis axis, int value) {
        if (axis == Axis.ROW) {
            mRow = value;
        } else {
            mCol = value;
        }
    }

    @Override
    public boolean equals(final Object other) {
        if (!(other instanceof GridPoint)) throw new ClassCastException();
        final GridPoint otherGridPoint = (GridPoint) other;
        return mRow == otherGridPoint.getRow() && mCol == otherGridPoint.getCol();
    }

    @Override
    public String toString() {
        return "R " + Integer.toString(mRow) + ", C " + Integer.toString(mCol);
    }
}
