package com.erichamion.racetrack;

import java.util.*;

/**
 * Created by me on 8/19/15.
 */
public class PathFollower {
    private final PathFinder mPathFinder;
    private final Track mTrack;
    private final int mPlayerIndex;
    private GridPoint mGoal;
    // private GridPoint mNextGoal;
    // private GridPoint mNextGoalDiff;

    private static final GridPoint[] ALL_DIRECTIONS = new GridPoint[9];
    static {
        int i = 0;
        for (int row = -1; row <= 1; row++) {
            for (int col = -1; col <= 1; col++) {
                ALL_DIRECTIONS[i++] = new GridPoint(row, col);
            }
        }
    }

    public PathFollower(final Track track, final PathFinder pathFinder, final int playerIndex) {
        mTrack = track;
        mPathFinder = pathFinder;
        mPlayerIndex = playerIndex;
        updateGoal();
    }

    /**
     * Calculate the acceleration needed for the next move. Will attempt
     * to follow the path that was supplied as a PathFinder argument to
     * this object's constructor.
     * @return A GridPoint containing the row and column acceleration to
     * apply. The acceleration in each dimension will be in the range
     * [-1, 1].
     */
    public GridPoint getMove() {
        GridPoint currentPosition = mTrack.getPlayerPos(mPlayerIndex);
        GridPoint currentVelocity = mTrack.getPlayerVelocity(mPlayerIndex);

        if (currentPosition.equals(mGoal)) {
            updateGoal();
        }

        MoveMap moveMap = new MoveMap(currentPosition, mGoal, currentVelocity, ALL_DIRECTIONS);
        Comparator<GridPoint> comparator;

        // If we're moving the wrong direction or the goal is farther than
        // the distance needed to stop, rush headlong toward the goal.
        if (isWrongDirection(currentPosition, currentVelocity, mGoal) ||
                isPastStoppingDistance(GridPoint.add(currentPosition, currentVelocity), currentVelocity, mGoal)) {
            comparator = new LongDistanceComparator(moveMap);

        } else if (Math.abs(currentVelocity.getRow()) > 1 || Math.abs(currentVelocity.getCol()) > 1) {
            // Once we're down to the stopping distance, decelerate to -1, 0,
            // or 1 in each direction.
            comparator = new DecelerateComparator(moveMap);

        } else {
            // Once velocity is within +/- 1 in each direction, head toward
            // the goal while keeping velocity low.
            comparator = new LimpComparator(moveMap);

        }

        Queue<GridPoint> candidates = new PriorityQueue<>(ALL_DIRECTIONS.length, comparator);
        Collections.addAll(candidates, ALL_DIRECTIONS);

        // Get the best move that doesn't crash immediately into a wall or
        // another player.
        GridPoint result = null;
        do {
            GridPoint candidate = candidates.remove();
            if (!mTrack.willPlayerCrash(mPlayerIndex, moveMap.getPosition(candidate))) {
                result = candidate;
            }
        } while (result == null && !candidates.isEmpty());

        return (result == null) ? new GridPoint(0, 0) : result;
    }



    private void updateGoal() {
        mGoal = mPathFinder.getNextPathPoint();
//        mNextGoal = mPathFinder.peekNextPathPoint();
//        if (mNextGoal != null) {
//            mNextGoalDiff = GridPoint.subtract(mNextGoal, mGoal);
//        } else {
//            mNextGoalDiff = null;
//        }
    }

    /**
     * Calculates the number of spaces on each axis needed to stop, given
     * an initial velocity.
     * @param startVelocity A starting velocity
     * @return A GridPoint containing the number of spaces needed to stop
     * from the initial velocity. Both the row and column values will be
     * non-negative, representing distance, not displacement.
     */
    private GridPoint getStoppingDistance(GridPoint startVelocity) {
        // For each axis with an initial speed S, stopping requires a
        // number of spaces equal to:
        // S + (S - 1) + (S - 2) + ... + 2 + 1
        // This sum is equal to S * (S + 1) / 2

        GridPoint result = new GridPoint();
        for (GridPoint.Axis axis : GridPoint.Axis.values()) {
            int velocity = startVelocity.getValueOnAxis(axis);
            int speed = Math.abs(velocity);
            int distanceNeeded = speed * (speed + 1) / 2;
            result.setValueOnAxis(axis, distanceNeeded);
        }

        return result;
    }

    /**
     * Determines whether at least one of the velocity components (row or
     * column component) points away from the goal.
     * @param position The current position
     * @param velocity The velocity
     * @param goal The target position
     * @return Returns true if one or both velocity components point away
     * from the goal, false otherwise.
     */
    private boolean isWrongDirection(final GridPoint position, final GridPoint velocity, final GridPoint goal) {
        for (GridPoint.Axis axis : GridPoint.Axis.values()) {
            int goalDistance = goal.getValueOnAxis(axis) - position.getValueOnAxis(axis);
            int velocityComponent = velocity.getValueOnAxis(axis);

            // If either the velocity or the distance to the goal on this
            // axis is 0, it isn't wrong.
            if (goalDistance == 0 || velocityComponent == 0) continue;

            if (!Util.isSignSame(goalDistance, velocityComponent)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Returns true if the goal is outside the required stopping
     * distance with the current velocity, in at least one component (row
     * or column).
     * @param position The current position
     * @param velocity The current velocity
     * @param goal The target position
     * @return Returns false if both components of the distance to the
     * goal are equal to or less than the required stopping distance, true
     * otherwise.
     */
    private boolean isPastStoppingDistance(final GridPoint position, final GridPoint velocity, final GridPoint goal) {
        GridPoint stoppingVector = getStoppingDistance(velocity);
        for (GridPoint.Axis axis : GridPoint.Axis.values()) {
            int goalDistance = Math.abs(goal.getValueOnAxis(axis) - position.getValueOnAxis(axis));
            int stoppingDistance = stoppingVector.getValueOnAxis(axis);

            if (goalDistance > stoppingDistance) {
                return true;
            }
        }

        return false;
    }



    /**
     * Prioritizes the closest direction to the goal, with near matches
     * being broken by the highest velocity.
     */
    private static class LongDistanceComparator implements Comparator<GridPoint> {
        private static final double DOT_PRODUCT_CUTOFF = 0.01;

        private final MoveMap mMap;
        private final GridPoint mFromStartToGoal;

        public LongDistanceComparator(MoveMap moveMap) {
            mMap = moveMap;
            mFromStartToGoal = GridPoint.subtract(mMap.getGoalPoint(), mMap.getStartPoint());
        }


        @Override
        public int compare(GridPoint o1, GridPoint o2) {
            GridPoint velocity1 = mMap.getVelocity(o1);
            GridPoint velocity2 = mMap.getVelocity(o2);

            // (0, 0) doesn't have a unit vector, so it needs special
            // handling. There's no reason to sit stationary, so give
            // (0, 0) the lowest priority.
            if (velocity1.getRow() == 0 && velocity1.getCol() == 0) {
                if (velocity2.getRow() == 0 && velocity2.getCol() == 0) {
                    return 0;
                } else {
                    return 1;
                }
            } else if (velocity2.getRow() == 0 && velocity2.getCol() == 0) {
                return -1;
            }

            double dotResult = GridPoint.unitDotProduct(mFromStartToGoal, velocity2) -
                    GridPoint.unitDotProduct(mFromStartToGoal, velocity1);

            // Velocities of (2, 0) and (2,1) will have a unit dot product
            // of (2 / Math.sqrt(5)), which is about 0.89.
            if (Math.abs(dotResult) < DOT_PRODUCT_CUTOFF) {
                // The directions are close. Prioritize high velocity.
                return (velocity2.getRow() * velocity2.getRow() + velocity2.getCol() * velocity2.getCol()) -
                        (velocity1.getRow() * velocity1.getRow() + velocity1.getCol() * velocity1.getCol());
            } else if (dotResult < 0) {
                return -1;
            } else {
                return 1;
            }
        }
    }

    /**
     * Prioritizes deceleration. If two directions have equally low
     * resulting speeds, then the one that points closest to the goal
     * wins.
     */
    private static class DecelerateComparator implements Comparator<GridPoint> {
        private final MoveMap mMap;
        private final GridPoint mFromStartToGoal;

        public DecelerateComparator(MoveMap moveMap) {
            mMap = moveMap;
            mFromStartToGoal = GridPoint.subtract(mMap.getGoalPoint(), mMap.getStartPoint());
        }

        @Override
        public int compare(GridPoint o1, GridPoint o2) {
            // First-order comparison: minimize the total resulting
            // velocity (measured as the sum of the velocity coordinates).
            GridPoint velocity1 = mMap.getVelocity(o1);
            GridPoint velocity2 = mMap.getVelocity(o2);
            int comparison = (Math.abs(velocity1.getRow()) + Math.abs(velocity1.getCol())) -
                    (Math.abs(velocity2.getRow()) + Math.abs(velocity2.getCol()));

            // Break ties by pointing toward the goal (use unit dot
            // product, not just dot product, because we want to reward
            // only direction, not speed.
            if (comparison == 0) {
                double secondComparison = GridPoint.unitDotProduct(mFromStartToGoal, velocity2) -
                        GridPoint.unitDotProduct(mFromStartToGoal, velocity1);
                if (secondComparison < 0.0) {
                    comparison = -1;
                } else if (secondComparison >= 0.0) {
                    comparison = 1;
                }
            }

            return comparison;
        }
    }

    /**
     * Limps along toward the goal. The first order comparison breaks
     * directions into two groups: directions which result in velocities
     * that have both components in the range [-1, 1], and directions
     * which don't result in such velocities. The first group is
     * preferred. Within each group, the direction that results in
     * movement closest to the direction of the goal is preferred.
     */
    private static class LimpComparator implements Comparator<GridPoint> {
        private final MoveMap mMap;
        private final GridPoint mFromStartToGoal;

        public LimpComparator(MoveMap moveMap) {
            mMap = moveMap;
            mFromStartToGoal = GridPoint.subtract(mMap.getGoalPoint(), mMap.getStartPoint());
        }

        @Override
        public int compare(GridPoint o1, GridPoint o2) {
            GridPoint velocity1 = mMap.getVelocity(o1);
            GridPoint velocity2 = mMap.getVelocity(o2);

            boolean isSlow1 = Math.abs(velocity1.getRow()) <= 1 && Math.abs(velocity1.getCol()) <= 1;
            boolean isSlow2 = Math.abs(velocity2.getRow()) <= 1 && Math.abs(velocity2.getCol()) <= 1;

            if (isSlow1 != isSlow2) {
                // Exactly one of the resulting velocities has a component
                // greater than 1 (or less than -1). Prioritize the other
                // one.
                return isSlow1 ? -1 : 1;
            }

            // Either both velocities are either acceptably slow, or
            // neither is. Prioritize the highest unit dot product between
            // the velocity vector and the vector to the goal.
            double comparison = GridPoint.unitDotProduct(mFromStartToGoal, velocity2) -
                    GridPoint.unitDotProduct(mFromStartToGoal, velocity1);
            if (comparison < 0.0) {
                return -1;
            } else if (comparison >= 0.0) {
                return 1;
            }
            return 0;
        }
    }


    private static class MoveMap {
        private static final int VEL = 0;
        private static final int POS = 1;

        private final GridPoint mStart;
        private final GridPoint mGoal;
        private final Map<GridPoint, GridPoint[]> mMap = new HashMap<>();


        public MoveMap(final GridPoint start, final GridPoint goal, final GridPoint baseVelocity,
                       final GridPoint[] directions) {
            mStart = start;
            mGoal = goal;
            for (GridPoint direction : directions) {
                GridPoint[] newEntry = new GridPoint[2];
                newEntry[VEL] = GridPoint.add(baseVelocity, direction);
                newEntry[POS] = GridPoint.add(mStart, newEntry[VEL]);
                mMap.put(direction, newEntry);
            }
        }

        public GridPoint getStartPoint() {
            return mStart;
        }

        public GridPoint getGoalPoint() {
            return mGoal;
        }

        public GridPoint getPosition(final GridPoint direction) {
            GridPoint[] entry = mMap.get(direction);
            return (entry == null) ? null : entry[POS];
        }

        public GridPoint getVelocity(final GridPoint direction) {
            GridPoint[] entry = mMap.get(direction);
            return (entry == null) ? null : entry[VEL];
        }
    }
}
