package com.erichamion.racetrack;

import com.sun.javaws.exceptions.InvalidArgumentException;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URL;
import java.nio.IntBuffer;
import java.util.*;

public class RacetrackFXMLController implements Initializable {

    private static final String COLOR_PICKER_PREFIX = "colorPicker";
    private static final String PLAYER_CHECKBOX_PREFIX = "playerCheck";

    enum InputMode {
        NONE, DIRECTIONS, ANY
    }


    @FXML private MenuItem menuRestart;
    @FXML private ImageView trackImg;
    @FXML private BorderPane mainLayout;
    @FXML private ScrollPane trackBoundingBox;
    @FXML private VBox playerListBox;
    @FXML private Label headerComp;
    @FXML private Label currentPlayerLabel;
    @FXML private Button ULButton;
    @FXML private Button UpButton;
    @FXML private Button URButton;
    @FXML private Button LeftButton;
    @FXML private Button CenterButton;
    @FXML private Button RightButton;
    @FXML private Button DLButton;
    @FXML private Button DownButton;
    @FXML private Button DRButton;

    private Scene myScene;
    private Image baseTrackImage;
    private TrackFX mTrack;
    private final Map<Integer, PathFollower> mComputerPlayers = new HashMap<>();
    private final List<Color> mPlayerColors = new ArrayList<>();
    private InputMode mInputMode = InputMode.NONE;
    private File mTrackFile;


    private final Map<Button, GridPoint> DIRMAP = new HashMap<>(9);
    private final Map<KeyCode, Button> KEYMAP = new HashMap<>(30);


    private final ChangeListener<Number> resizeListener= new ChangeListener<Number>() {
        @Override
        public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
            if (baseTrackImage != null) {
                setTrackSize();
            }
        }
    };



    public void setScene(Scene scene) {
        myScene = scene;
        myScene.setOnKeyPressed(this::handleKeyAction);
    }



    @Override
    public void initialize(URL location, ResourceBundle resources) {
        mainLayout.widthProperty().addListener(resizeListener);
        mainLayout.heightProperty().addListener(resizeListener);

        DIRMAP.put(ULButton, new GridPoint(-1, -1));
        DIRMAP.put(UpButton, new GridPoint(-1, 0));
        DIRMAP.put(URButton, new GridPoint(-1, 1));
        DIRMAP.put(LeftButton, new GridPoint(0, -1));
        DIRMAP.put(CenterButton, new GridPoint(0, 0));
        DIRMAP.put(RightButton, new GridPoint(0, 1));
        DIRMAP.put(DLButton, new GridPoint(1, -1));
        DIRMAP.put(DownButton, new GridPoint(1, 0));
        DIRMAP.put(DRButton, new GridPoint(1, 1));


        KEYMAP.put(KeyCode.HOME, ULButton);
        KEYMAP.put(KeyCode.DIGIT7, ULButton);
        KEYMAP.put(KeyCode.NUMPAD7, ULButton);
        KEYMAP.put(KeyCode.UP, UpButton);
        KEYMAP.put(KeyCode.KP_UP, UpButton);
        KEYMAP.put(KeyCode.DIGIT8, UpButton);
        KEYMAP.put(KeyCode.NUMPAD8, UpButton);
        KEYMAP.put(KeyCode.PAGE_UP, URButton);
        KEYMAP.put(KeyCode.DIGIT9, URButton);
        KEYMAP.put(KeyCode.NUMPAD9, URButton);
        KEYMAP.put(KeyCode.LEFT, LeftButton);
        KEYMAP.put(KeyCode.KP_LEFT, LeftButton);
        KEYMAP.put(KeyCode.DIGIT4, LeftButton);
        KEYMAP.put(KeyCode.NUMPAD4, LeftButton);
        KEYMAP.put(KeyCode.DIGIT5, CenterButton);
        KEYMAP.put(KeyCode.NUMPAD5, CenterButton);
        KEYMAP.put(KeyCode.RIGHT, RightButton);
        KEYMAP.put(KeyCode.KP_RIGHT, RightButton);
        KEYMAP.put(KeyCode.DIGIT6, RightButton);
        KEYMAP.put(KeyCode.NUMPAD6, RightButton);
        KEYMAP.put(KeyCode.END, DLButton);
        KEYMAP.put(KeyCode.DIGIT1, DLButton);
        KEYMAP.put(KeyCode.NUMPAD1, DLButton);
        KEYMAP.put(KeyCode.DOWN, DownButton);
        KEYMAP.put(KeyCode.KP_DOWN, DownButton);
        KEYMAP.put(KeyCode.DIGIT2, DownButton);
        KEYMAP.put(KeyCode.NUMPAD2, DownButton);
        KEYMAP.put(KeyCode.PAGE_DOWN, DRButton);
        KEYMAP.put(KeyCode.DIGIT3, DRButton);
        KEYMAP.put(KeyCode.NUMPAD3, DRButton);

        setInputMode(InputMode.NONE);
        headerComp.setMaxWidth(Control.USE_PREF_SIZE);
    }



    @FXML
    private void handleMenuActionLoad(final ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        File file = fileChooser.showOpenDialog(null);
        if (file != null) {
            loadTrack(file);
        }

    }

    @FXML
    private void handleMenuActionRestart(final ActionEvent event) {
        loadTrack(mTrackFile);
    }

    @FXML
    private void handleButtonAction(final ActionEvent event) {
        if (mInputMode == InputMode.NONE) {
            // Do nothing
            return;
        }

        GridPoint acceleration;
        if (mInputMode == InputMode.DIRECTIONS) {
            // We were waiting for human input, so use that input.
            acceleration = DIRMAP.get(event.getTarget());
        } else {
            // mInputMode == InputMode.ANY
            // We were just waiting for some signal to continue with a
            // computer-controlled move.
            acceleration = getComputerMove();
        }

        mTrack.doPlayerTurn(acceleration);
        updateGameStatus();
    }

    @FXML
    private void handleKeyAction(final KeyEvent event) {
        Button simulatedButton = null;
        switch (mInputMode) {
            case NONE:
                break;
            case DIRECTIONS:
                // If the code isn't in KEYMAP, this will return null.
                simulatedButton = KEYMAP.get(event.getCode());
                break;
            case ANY:
                simulatedButton = CenterButton;
                break;
        }

        if (simulatedButton != null) {
            simulatedButton.fire();
        }
    }

    private void handleColorAction(final ActionEvent event) {
        ColorPicker picker = (ColorPicker) event.getTarget();
        String pickerId = picker.getId();
        int playerIndex = getNumericSuffix(pickerId, COLOR_PICKER_PREFIX);
        mPlayerColors.set(playerIndex, picker.getValue());
        updateGameStatus();
    }

    private void handleCheckBoxAction(final ActionEvent event) {
        CheckBox checkBox = (CheckBox) event.getTarget();
        String boxId = checkBox.getId();
        int playerIndex = getNumericSuffix(boxId, PLAYER_CHECKBOX_PREFIX);
        if (checkBox.isSelected()) {
            PathFinder playerFinder = new PathFinder(mTrack, playerIndex);
            PathFollower playerFollower = new PathFollower(mTrack, playerFinder, playerIndex);
            mComputerPlayers.put(playerIndex, playerFollower);
        } else {
            mComputerPlayers.remove(playerIndex);
        }
        updateGameStatus();
    }


    private void reset() {
        mTrack = null;
        mComputerPlayers.clear();
        mPlayerColors.clear();

        HBox headerBox = null;
        for (Node node : playerListBox.getChildren()) {
            HBox hBox = (HBox) node;
            for (Node childNode : hBox.getChildren()) {
                if (childNode.getId().equals("headerComp")) {
                    headerBox = hBox;
                    break;
                }
            }
            if (headerBox != null) {
                break;
            }
        }
        playerListBox.getChildren().clear();
        playerListBox.getChildren().add(headerBox);

        setTrackImage(null);
    }

    private void loadTrack(File trackFile) {
        reset();


        try {
            mTrack = new TrackFX(new Scanner(trackFile));
            for (int i = 0; i < mTrack.getPlayerCount(); i++) {
                addPlayer();
            }
            mTrackFile = trackFile;
        } catch (InvalidTrackFormatException e) {
            showMessage("Racetrack", "Could not load track from " + trackFile.toString(), e.getMessage());
        } catch (FileNotFoundException e) {
            showMessage("Racetrack", "Could not find file:", trackFile.toString());
        } finally {
            updateGameStatus();
        }
    }

    private void updateGameStatus() {
        if (mTrack == null) {
            reset();
            return;
        }

        Image image = mTrack.toImage(mPlayerColors);
        setTrackImage(image);

        int winner = mTrack.getWinner();
        String playerLabelString;
        Color playerLabelColor;
        if (winner == Track.NO_WINNER) {
            int currentPlayer = mTrack.getCurrentPlayer();
            playerLabelString = "Player " + Integer.toString(currentPlayer + 1);
            playerLabelColor = mPlayerColors.get(currentPlayer);
            if (mComputerPlayers.containsKey(currentPlayer)) {
                // Computer player, just wait for any key press
                setInputMode(InputMode.ANY);
                playerLabelString += " (Comp)";
            } else {
                // Wait for human input
                setInputMode(InputMode.DIRECTIONS);
            }

        } else {
            setInputMode(InputMode.NONE);
            playerLabelString = "Winner:\nPlayer " + Integer.toString(winner + 1);
            playerLabelColor = mPlayerColors.get(winner);
        }

        currentPlayerLabel.setText(playerLabelString);
        BackgroundFill labelBgFill = new BackgroundFill(playerLabelColor, new CornerRadii(3), null);
        currentPlayerLabel.setBackground(new Background(labelBgFill));
    }

    private void setTrackImage(Image image) {
        baseTrackImage = image;
        setTrackSize();
    }

    private void setTrackSize() {
        trackImg.setImage(null);
        mainLayout.requestLayout();

        if (baseTrackImage == null) {
            // No image, nothing else to do
            return;
        }

        int layoutWidth = (int) trackBoundingBox.getWidth();
        int layoutHeight = (int) trackBoundingBox.getHeight();
        int sourceWidth = (int) baseTrackImage.getWidth();
        int sourceHeight = (int) baseTrackImage.getHeight();

        // Always scale by an integer. This simplifies the logic, and it gives a cleaner display (for non-integer
        // scaling without any smoothing, some rows/columns would appear to be different sizes than others).
        int widthScaleFactor = layoutWidth / sourceWidth;
        int heightScaleFactor = layoutHeight / sourceHeight;
        int scaleFactor = (widthScaleFactor < heightScaleFactor) ? widthScaleFactor : heightScaleFactor;
        if (scaleFactor == 0) {
            scaleFactor = 1;
        }

        // Don't bother copying if nothing needs to be scaled up.
        Image resizedImage = (scaleFactor == 1) ? baseTrackImage : scaleImage(baseTrackImage, scaleFactor);
        trackImg.setImage(resizedImage);
        centerTrackImage(layoutWidth, layoutHeight, sourceWidth * scaleFactor, sourceHeight * scaleFactor);


    }

    private Image scaleImage(Image baseImage, int scaleFactor) {
        // Using the JavaFX scaling always does some smoothing, even when smooth="false". We don't want that,
        // so we have to copy the base image into a new image, doing the scaling for ourselves.

        // Scaling by an integer scaleFactor simplifies the logic, and it gives a cleaner display (for non-integer
        // scaling no smoothing, some rows/columns would appear to be different sizes than others).


        int sourceWidth = (int) baseImage.getWidth();
        int sourceHeight = (int) baseImage.getHeight();

        WritableImage resizedImage = new WritableImage(sourceWidth * scaleFactor, sourceHeight * scaleFactor);
        PixelReader reader = baseImage.getPixelReader();
        PixelWriter writer = resizedImage.getPixelWriter();

        WritablePixelFormat<IntBuffer> pixelFormat = PixelFormat.getIntArgbInstance();
        int[] rowPixels = new int[sourceWidth];
        for (int sourceY = 0; sourceY < sourceHeight; sourceY++) {
            reader.getPixels(0, sourceY, sourceWidth, 1, pixelFormat, rowPixels, 0, sourceWidth);
            int baseDestY = sourceY * scaleFactor;
            for (int sourceX = 0; sourceX < sourceWidth; sourceX++) {
                int argb = rowPixels[sourceX];
                int baseDestX = sourceX * scaleFactor;
                for (int modY = 0; modY < scaleFactor; modY++) {
                    int destY = baseDestY + modY;
                    for (int modX = 0; modX < scaleFactor; modX++) {
                        int destX = baseDestX + modX;
                        writer.setArgb(destX, destY, argb);
                    }
                }
            }
        }

        return resizedImage;
    }

    private void centerTrackImage(int boundingWidth, int boundingHeight, int imageWidth, int imageHeight) {
        trackImg.setTranslateX((boundingWidth - imageWidth) / 2);
        trackImg.setTranslateY((boundingHeight - imageHeight) / 2);
    }

    private void addPlayer() {
        int playerIndex = mPlayerColors.size();
        Color color = mTrack.COLOR_PLAYER_DEFAULT.deriveColor(100 * playerIndex, 1.0, 1.0, 1.0);
        mPlayerColors.add(color);

        HBox playerRow = new HBox();
        CheckBox checkBox = new CheckBox();
        checkBox.setId(PLAYER_CHECKBOX_PREFIX + Integer.toString(playerIndex));
        checkBox.setMaxWidth(headerComp.getWidth());
        checkBox.setMinWidth(headerComp.getWidth());
        checkBox.setAlignment(Pos.CENTER);
        checkBox.setOnAction(this::handleCheckBoxAction);
        ColorPicker colorPicker = new ColorPicker(color);
        colorPicker.getStyleClass().add(ColorPicker.STYLE_CLASS_BUTTON);
        colorPicker.setMaxHeight(checkBox.getHeight());
        colorPicker.setStyle("-fx-color-label-visible: false ;");
        colorPicker.setId(COLOR_PICKER_PREFIX + Integer.toString(playerIndex));
        colorPicker.setOnAction(this::handleColorAction);
        Label playerLabel = new Label("Player " + Integer.toString(playerIndex + 1));
        playerLabel.setPadding(new Insets(3.0));
        playerRow.getChildren().addAll(checkBox, colorPicker, playerLabel);
        playerRow.setPadding(new Insets(1.0));
        playerListBox.getChildren().add(playerRow);
    }

    private void setInputMode(InputMode mode) {
        mInputMode = mode;
        final Button[] outerButtons = {
                ULButton, UpButton, URButton, LeftButton, RightButton, DLButton, DownButton, DRButton
        };
        final Button[] allButtons = {
                ULButton, UpButton, URButton, LeftButton, CenterButton, RightButton, DLButton, DownButton, DRButton
        };

        switch (mInputMode) {
            case NONE:
                for (Button button : allButtons) {
                    button.setDisable(true);
                }
                break;
            case DIRECTIONS:
                for (Button button : allButtons) {
                    button.setDisable(false);
                }
                menuRestart.setDisable(false);
                break;
            case ANY:
                for (Button button : outerButtons) {
                    button.setDisable(true);
                }
                CenterButton.setDisable(false);
                menuRestart.setDisable(false);
        }
    }

    private GridPoint getComputerMove() {
        PathFollower follower = mComputerPlayers.get(mTrack.getCurrentPlayer());
        return follower.getMove();
    }


    /**
     * Show an informational dialog message to the user. Same as
     * showMessage(String, String, String, boolean) with the isError parameter
     * set to false.
     */
    private void showMessage(String title, String header, String content) {
        showMessage(title, header, content, false);
    }

    /**
     * Show a modal dialog message to the user. Any of the string parameters
     * can be null.
     * @param title      The text shown in the dialog title bar
     * @param header     A message shown in the top portion of the dialog. If
     *                   separate header and content are not needed, this
     *                   should be null and content should be non-null.
     * @param content    The message shown to the user. If header is non-null,
     *                   content will be in the lower portion of the dialog.
     * @param isError    If true, the alert will show as an error. If false, it
     *                   will show as an informational dialog.
     */
    private void showMessage(String title, String header, String content, boolean isError) {
        Alert.AlertType alertType = isError ? Alert.AlertType.ERROR : Alert.AlertType.INFORMATION;
        Alert alert = new Alert(alertType);
        if (title != null) {
            alert.setTitle(title);
        }
        if (header != null) {
            alert.setHeaderText(header);
        }
        if (content != null) {
            alert.setContentText(content);
        }
        alert.showAndWait();
    }

    private int getNumericSuffix(String value, String prefix)
            throws IllegalArgumentException, NumberFormatException {
        if (!value.startsWith(prefix)) {
            throw new IllegalArgumentException("getNumericSuffix error: value does not start with prefix");
        }

        String suffix = value.substring(prefix.length());
        return Integer.parseInt(suffix);
    }
}
