package com.erichamion.racetrack;

import javafx.scene.image.Image;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

import java.util.List;
import java.util.Scanner;

/**
 * Created by me on 9/8/15.
 */
public class TrackFX extends Track {
    private static final Color COLOR_WALL = Color.SLATEGRAY;
    private static final Color COLOR_TRACK = Color.PALEGOLDENROD;
    private static final Color COLOR_FINISH = Color.GOLD;
    private static final Color COLOR_CRASH = Color.RED;
    public static final Color COLOR_PLAYER_DEFAULT = Color.BLUE;


    /**
     * Initialize a Track from an input source.
     *
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
    public TrackFX(Scanner scanner) throws InvalidTrackFormatException {
        super(scanner);
    }

    public Image toImage(List<Color> playerColors) {
        WritableImage returnVal = new WritableImage(getWidth(), getHeight());
        PixelWriter writer = returnVal.getPixelWriter();

        // Set the track (without players) first
        for (int rowIndex = 0; rowIndex < getHeight(); rowIndex++) {
            List<SpaceType> currentRow = mGrid.get(rowIndex);
            for (int colIndex = 0; colIndex < getWidth(); colIndex++) {
                switch (currentRow.get(colIndex)) {
                    case WALL:
                        writer.setColor(colIndex, rowIndex, COLOR_WALL);
                        break;
                    case TRACK:
                        writer.setColor(colIndex, rowIndex, COLOR_TRACK);
                        break;
                    case FINISH_LEFT:
                    case FINISH_RIGHT:
                    case FINISH_DOWN:
                    case FINISH_UP:
                        writer.setColor(colIndex, rowIndex, COLOR_FINISH);
                }
            }
        }

        // Add the players in
        for (int playerIndex = 0; playerIndex < mPlayers.size(); playerIndex++) {
            Player currentPlayer = mPlayers.get(playerIndex);

            Color playerColor = COLOR_PLAYER_DEFAULT;
            if (currentPlayer.isCrashed()) {
                playerColor = COLOR_CRASH;
            } else if (playerColors != null && playerIndex < playerColors.size()) {
                playerColor = playerColors.get(playerIndex);
            }

            writer.setColor(currentPlayer.getPos().getCol(), currentPlayer.getPos().getRow(), playerColor);
        }



        return returnVal;
    }
}
