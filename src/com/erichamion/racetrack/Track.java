package com.erichamion.racetrack;



import java.util.*;

/**
 * Created by me on 8/14/15.
 */
public class Track {
    public static final int MAX_PLAYERS = 9;
    public static final int NO_WINNER = -1;

    private static final char CRASH_INDICATOR = 'X';

    protected List<Player> mPlayers = new ArrayList<>();
    private int mWidth = 0;
    private int mHeight = 0;
    protected List<List<SpaceType>> mGrid = new ArrayList<>();
    private int mCurrentPlayer = 0;
    private int mWinner = NO_WINNER;


    public enum SpaceType {
        WALL('#'),
        TRACK(' '),
        FINISH_UP('^'),
        FINISH_DOWN('v'),
        FINISH_LEFT('<'),
        FINISH_RIGHT('>');

        private final char value;

        SpaceType(final char c) {
            value = c;
        }


    }


    /**
     * Initialize a Track from an input source.
     * @param scanner A java.util.Scanner connected to an input source
     *                that holds the track data. Track data must be a
     *                rectangular grid of text. Empty lines at the start
     *                are ignored. Processing stops at the first empty
     *                line following a non-empty line, or at the end of
     *                the stream. The first character in the first
     *                non-empty line is considered a wall. A space
     *                character (' ') is open track. Any of '<', '>', '^',
     *                or 'v' represent a finish line and indicate the
     *                direction the car needs to be moving in order to
     *                successfully cross. Any other character indicates
     *                the starting position for a car, and there must be
     *                between 1 and MAX_PLAYERS of these (one for each
     *                player - either the same or different characters).
     * @throws InvalidTrackFormatException
     */
    public Track(final Scanner scanner) throws InvalidTrackFormatException {
        char borderChar = '\0';
        while (scanner.hasNextLine()) {
            String currentLine = scanner.nextLine();
            int lineLength = currentLine.length();
            if (lineLength == 0) {
                if (mHeight == 0) {
                    continue;
                } else {
                    break;
                }
            }

            if (mWidth == 0) {
                mWidth = lineLength;
            } else {
                if (lineLength != mWidth) {
                    throw new InvalidTrackFormatException("Track does not have a consistent width");
                }
            }

            if (borderChar == '\0') {
                borderChar = currentLine.charAt(0);
            }

            addGridRow(currentLine, borderChar);
        }

        // Final sanity checks
        if (mHeight == 0) throw new InvalidTrackFormatException("No track data supplied");
        if (mPlayers.size() == 0) throw new InvalidTrackFormatException("No player positions");

        mCurrentPlayer = 0;
    }

    /**
     * Return a String representation of the track, including the
     * player locations.
     * @return A String representation of the track
     */
    public String toString() {
        StringBuilder result = new StringBuilder();
        for (int rowIndex = 0; rowIndex < mGrid.size(); rowIndex++) {
            List<SpaceType> currentRow = mGrid.get(rowIndex);
            for (int colIndex = 0; colIndex < currentRow.size(); colIndex++) {
                SpaceType currentSpace = currentRow.get(colIndex);
                boolean hasPlayer = false;
                for (int playerNum = 0; playerNum < mPlayers.size(); playerNum++) {
                    Player player = mPlayers.get(playerNum);
                    if (player.getPos().getCol() == colIndex && player.getPos().getRow() == rowIndex) {
                        hasPlayer = true;
                        result.append(player.isCrashed() ? CRASH_INDICATOR : Integer.toString(playerNum + 1));

                        // Only put one player indicator in a given space
                        break;
                    }
                }
                if (!hasPlayer) {
                    result.append(currentSpace.value);
                }
            }
            result.append('\n');
        }

        return result.toString();
    }

    /**
     * Return the width (number of columns) of the track grid.
     * @return Width of the track grid
     */
    public int getWidth() {
        return mWidth;
    }

    /**
     * Return the height (number of rows) of the track grid.
     * @return Height of the track grid
     */
    public int getHeight() {
        return mHeight;
    }

    /**
     * Return the number of players.
     * @return Number of players
     */
    public int getPlayerCount() {
        return mPlayers.size();
    }

    /**
     * Return the current player. Player numbers are zero-based, so the
     * first player is 0, and the last player is getPlayerCount() - 1.
     * @return The zero-based number of the current player
     */
    public int getCurrentPlayer() {
        return mCurrentPlayer;
    }

    /**
     * Find the position of the specified player.
     * @param player The zero-based player number
     * @return A GridPoint containing the player's current position
     */
    public GridPoint getPlayerPos(final int player) {
        return mPlayers.get(player).getPos();
    }

    /**
     * Find the velocity of the specified player.
     * @param player The zero-based player number
     * @return A GridPoint containing the player's current velocity
     */
    public GridPoint getPlayerVelocity(final int player) {
        return mPlayers.get(player).getVelocity();
    }

    /**
     * Return the winner of the game. If the game is still in progress,
     * returns NO_WINNER.
     * @return The winning player (zero-based, see getCurrentPlayer()),
     * or NO_WINNER if the game is still in progress
     */
    public int getWinner() {
        return mWinner;
    }

    /**
     * Accelerate the current player, and update the track state.
     * @param acceleration The current player's acceleration in each
     *                     direction
     */
    public void doPlayerTurn(final GridPoint acceleration) {
        Player player = mPlayers.get(mCurrentPlayer);
        if (player.isCrashed() || mWinner != NO_WINNER) return;

        player.accelerate(acceleration);
        moveCurrentPlayer();

        if (player.isCrashed()) {
            int winCandidate = -1;
            for (int i = 0; i < mPlayers.size(); i++) {
                if (!mPlayers.get(i).isCrashed()) {
                    if (winCandidate == -1) {
                        winCandidate = i;
                    } else {
                        // More than one uncrashed players. Can't declare
                        // a winner.
                        winCandidate = -2;
                        break;
                    }
                }
            }
            if (winCandidate >= 0) {
                mWinner = winCandidate;
            }
        }

        mCurrentPlayer = getNextPlayer();
    }


    /**
     * Gets the next player who is still in the game. Skips crashed
     * players.
     * @return The next active player
     */
    private int getNextPlayer() {
        int result = mCurrentPlayer;
        do {
            result += 1;
            if (result >= getPlayerCount()) {
                result = 0;
            }
        } while (mPlayers.get(result).isCrashed());
        return result;
    }

    /**
     * Returns all of the grid spaces in the path between two spaces, for
     * use in determining line of sight.
     * @param startPoint Starting point as a GridPoint
     * @param endPoint Ending point as a GridPoint
     * @return Intervening grid spaces, as a List of GridPoints. Also
     * includes the starting and ending grid spaces. Each space is given
     * by a an array of length 2, with row first, followed by column.
     */
    public Set<GridPoint> getPath(final GridPoint startPoint, final GridPoint endPoint) {
        // First, pick the axis that has the largest movement.
        // For every grid boundary along that axis, test the line of
        // motion at both the center and the edges of the the cell,
        // identifying the position along the other axis.
        // For each of the identified positions, if the position is
        // within a grid cell, add that cell to the result set.
        // Do nothing if the position is on the boundary between two
        // cells. This means we can squeeze through diagonal corners under
        // the right conditions.

        final double EPS = 1e-8;

        Set<GridPoint> result = new HashSet<>();

        // If there's no movement, no need to do anything. Just return the
        // starting position.
        if (startPoint.equals(endPoint)) {
            result.add(new GridPoint(startPoint));
            return result;
        }

        GridPoint difference = new GridPoint(endPoint.getRow() - startPoint.getRow(),
                endPoint.getCol() - startPoint.getCol());
        GridPoint distance = new GridPoint(Math.abs(difference.getRow()), Math.abs(difference.getCol()));

        GridPoint.Axis mainAxis =
                (distance.getValueOnAxis(GridPoint.Axis.ROW) > distance.getValueOnAxis(GridPoint.Axis.COL)) ?
                        GridPoint.Axis.ROW : GridPoint.Axis.COL;
        GridPoint.Axis secondAxis = (mainAxis == GridPoint.Axis.ROW) ? GridPoint.Axis.COL : GridPoint.Axis.ROW;
        double slope = (double) difference.getValueOnAxis(secondAxis) / difference.getValueOnAxis(mainAxis);
        int stepDirection = (difference.getValueOnAxis(mainAxis) > 0) ? 1 : -1;

        int mainCoord = startPoint.getValueOnAxis(mainAxis);
        while (mainCoord != endPoint.getValueOnAxis(mainAxis)) {
            // Integer coordinate - if applicable, add just the single
            // grid space.
            double secondCoord = Util.getHeightOfLine(slope, startPoint.getValueOnAxis(mainAxis),
                    startPoint.getValueOnAxis(secondAxis), mainCoord);
            if (!Util.isHalfInteger(secondCoord, EPS)) {
                GridPoint newPoint = new GridPoint();
                newPoint.setValueOnAxis(mainAxis, mainCoord);
                newPoint.setValueOnAxis(secondAxis, (int) Math.round(secondCoord));
                result.add(newPoint);
            }
            // Half-integer coordinate - if applicable, add the grid
            // spaces to either side
            double mainHalfCoord = mainCoord + (stepDirection * 0.5);
            double secondHalfCoord = Util.getHeightOfLine(slope, startPoint.getValueOnAxis(mainAxis),
                    startPoint.getValueOnAxis(secondAxis), mainHalfCoord);
            if (!Util.isHalfInteger(secondHalfCoord, EPS)) {
                // Probably not the best names here, but I'm not sure what
                // would be better. If the main axis is the column axis,
                // and if the endPoint is to the right of the startPoint,
                // then the names leftPoint and rightPoint are accurate.
                GridPoint leftPoint = new GridPoint();
                GridPoint rightPoint = new GridPoint();
                int secondHalfInt = (int) Math.round(secondHalfCoord);
                leftPoint.setValueOnAxis(secondAxis, secondHalfInt);
                rightPoint.setValueOnAxis(secondAxis, secondHalfInt);
                leftPoint.setValueOnAxis(mainAxis, mainCoord);
                rightPoint.setValueOnAxis(mainAxis, mainCoord + stepDirection);
                result.add(leftPoint);
                result.add(rightPoint);
            }

            mainCoord += stepDirection;
        }

        result.add(new GridPoint(endPoint));

        return result;
    }

    /**
     * Find the type of track space at the given location. If the location
     * is outside the track bounds, it is considered a wall.
     * @param space The coordinates of the space to examine
     * @return The type of track space at the given location
     */
    public SpaceType getSpace(final GridPoint space) {
        // Anything out of bounds acts like a wall
        if (space.getRow() >= mHeight || space.getRow() < 0 || space.getCol() >= mWidth || space.getCol() < 0) {
            return SpaceType.WALL;
        }

        return mGrid.get(space.getRow()).get(space.getCol());
    }

    public boolean willPlayerCrash(int playerIndex, GridPoint position) {
        return (getSpace(position) == SpaceType.WALL || testPlayerCollision(playerIndex, position));
    }


    private void moveCurrentPlayer() {
        Player player = mPlayers.get(mCurrentPlayer);

        // Check for collisions and for winning
        GridPoint startPoint = player.getPos();
        GridPoint endPoint = player.getNextPos();

        Set<GridPoint> pathPoints = getPath(startPoint, endPoint);
        GridPoint winPoint = null;
        GridPoint winDirection = new GridPoint(0, 0);
        for (GridPoint currentPoint : pathPoints) {
            switch(getSpace(currentPoint)) {
                case TRACK:
                    // As long as we don't collide with another car, do
                    // nothing.
                    if (testPlayerCollision(mCurrentPlayer, endPoint)) {
                        player.crash();
                    }
                    break;
                case WALL:
                    // Crash, and move directly to the location that
                    // caused the crash. No need to keep going
                    player.crash();
                    player.setPos(currentPoint);
                    return;
                case FINISH_UP:
                    // For all of the finishes, set up a potential win,
                    // but don't act on it yet. We still might crash.
                    winDirection.setRow(-1);
                    winPoint = currentPoint;
                    break;
                case FINISH_DOWN:
                    winDirection.setRow(1);
                    winPoint = currentPoint;
                    break;
                case FINISH_LEFT:
                    winDirection.setCol(-1);
                    winPoint = currentPoint;
                    break;
                case FINISH_RIGHT:
                    winDirection.setCol(1);
                    winPoint = currentPoint;
                    break;
            }
        }

        // Test for win
        if (winPoint != null) {
            boolean isValidWin = true;
            if ((winDirection.getRow() != 0 && !Util.isSignSame(winDirection.getRow(), player.getVelocity().getRow()))
                    ||
                    (winDirection.getCol() != 0 &&
                            !Util.isSignSame(winDirection.getCol(), player.getVelocity().getCol()))) {
                isValidWin = false;
            }
            if (isValidWin) {
                mWinner = mCurrentPlayer;
                player.setPos(winPoint);
                return;
            }
        }


        player.move();
    }

    private boolean testPlayerCollision(int playerIndex, GridPoint location) {
        for (int i = 0; i < mPlayers.size(); i++) {
            // Don't check the player against itself
            if (i == playerIndex) continue;

            if (mPlayers.get(i).getPos().equals(location)) {
                return true;
            }
        }
        return false;
    }


    /**
     * Convert a string into a single row, adding it to the bottom of the
     * grid. Increments mHeight to account for the added row.
     * @param rowString A string containing a single row to add.
     * @param border The character to be interpreted as a wall/border.
     */
    private void addGridRow(final String rowString, final char border) throws InvalidTrackFormatException {
        int rowLength = rowString.length();
        List<SpaceType> row = new ArrayList<>(rowLength);
        for (int i = 0; i < rowLength; i++) {
            char currentChar = rowString.charAt(i);
            if (currentChar == border) {
                row.add(SpaceType.WALL);
            } else if (currentChar == SpaceType.TRACK.value) {
                row.add(SpaceType.TRACK);
            } else if (currentChar == SpaceType.FINISH_LEFT.value) {
                row.add(SpaceType.FINISH_LEFT);
            } else if (currentChar == SpaceType.FINISH_RIGHT.value) {
                row.add(SpaceType.FINISH_RIGHT);
            } else if (currentChar == SpaceType.FINISH_UP.value) {
                row.add(SpaceType.FINISH_UP);
            } else if (currentChar == SpaceType.FINISH_DOWN.value) {
                row.add(SpaceType.FINISH_DOWN);
            } else {
                // Unexpected character is a player, as long as we don't
                // have too many players. Since mHeight hasn't yet been
                // updated, the row is mHeight (not mHeight - 1).
                mPlayers.add(new Player(mHeight, i));
                row.add(SpaceType.TRACK);
                if (mPlayers.size() > MAX_PLAYERS) {
                    throw new InvalidTrackFormatException("Unexpected character in row " + Integer.toString(mHeight) +
                            " and column " + Integer.toString(i) + ": " + Character.toString(currentChar));
                }
            }
        }

        mGrid.add(row);
        mHeight++;
    }




}
