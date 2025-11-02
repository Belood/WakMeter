package com.wakfu.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.stage.Stage;

/**
 * MainUI represents the base layout of the application using a BorderPane.
 * - Top: Header with controls and status
 * - Center: PlayerUI list
 * - Right: Breakdown panel (initially empty, populated on demand)
 */
public class MainUI {

    private final Stage primaryStage;
    private final BorderPane root;
    private final HBox headerBox;
    private final Label appStatusLabel;
    private final VBox centerContainer;
    private final Pane rightPane;
    private final Label modeLabel;
    private final HBox modeButtonsBox;

    public MainUI(Stage primaryStage) {
        this.primaryStage = primaryStage;
        this.root = new BorderPane();
        this.headerBox = createHeader();
        this.appStatusLabel = new Label("App Status: \"En attente\"");
        this.modeLabel = new Label("Mode:");
        this.modeButtonsBox = new HBox(5);
        this.centerContainer = new VBox(3);
        this.rightPane = new StackPane();

        setupLayout();
    }

    /**
     * Creates the header HBox containing control buttons.
     * This can be customized further by the controller.
     */
    private HBox createHeader() {
        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(10));
        header.setStyle("-fx-border-color: #333; -fx-border-width: 0 0 1 0;");
        return header;
    }

    /**
     * Configures the main BorderPane layout.
     */
    private void setupLayout() {
        // Header section (top)
        VBox topSection = new VBox(0);

        // Status bar avec Mode et boutons
        HBox statusBar = new HBox(15);
        statusBar.setAlignment(Pos.CENTER_LEFT);
        statusBar.setPadding(new Insets(5, 10, 5, 15));

        appStatusLabel.setPadding(new Insets(0));
        modeLabel.setPadding(new Insets(0));
        modeLabel.setStyle("-fx-font-weight: bold;");

        statusBar.getChildren().addAll(appStatusLabel, modeLabel, modeButtonsBox);
        HBox.setHgrow(appStatusLabel, Priority.ALWAYS);

        topSection.getChildren().addAll(headerBox, statusBar);

        // Center: Players list (scrollable)
        ScrollPane centerScroll = new ScrollPane(centerContainer);
        centerScroll.setFitToWidth(true);
        centerScroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        centerScroll.setBackground(Background.EMPTY);
        centerScroll.setPrefViewportHeight(400);
        centerContainer.setFillWidth(true);

        // Right pane: Breakdown (initially empty)
        rightPane.setStyle("-fx-border-color: #333; -fx-border-width: 0 0 0 1;");
        rightPane.setPrefWidth(680);
        rightPane.setMinWidth(480);

        // Layout with SplitPane for resizable divider
        javafx.scene.control.SplitPane splitPane = new javafx.scene.control.SplitPane();
        splitPane.setDividerPositions(0.6);
        splitPane.getItems().addAll(centerScroll, rightPane);
        javafx.scene.control.SplitPane.setResizableWithParent(rightPane, false);

        root.setTop(topSection);
        root.setCenter(splitPane);
        root.setPadding(new Insets(0));

        // Set up the stage
        javafx.scene.Scene scene = new javafx.scene.Scene(root, 1000, 680);
        var css = getClass().getResource("/dark-theme.css");
        if (css != null) {
            scene.getStylesheets().add(css.toExternalForm());
        }
        primaryStage.setScene(scene);

        var iconStream = getClass().getResourceAsStream("/assets/icon.ico");
        if (iconStream != null) {
            primaryStage.getIcons().add(new Image(iconStream));
        }
        primaryStage.setTitle("WakMeter");
        primaryStage.setMinWidth(800);
        primaryStage.setMinHeight(500);
        primaryStage.show();
    }

    /**
     * Adds a component to the header.
     */
    public void addToHeader(javafx.scene.Node node) {
        headerBox.getChildren().add(node);
    }

    /**
     * Adds multiple components to the header.
     */
    public void addAllToHeader(java.util.List<javafx.scene.Node> nodes) {
        headerBox.getChildren().addAll(nodes);
    }

    /**
     * Updates the app status label.
     */
    public void setAppStatus(String status) {
        appStatusLabel.setText("App Status: \"" + (status == null ? "" : status) + "\"");
    }

    /**
     * Gets the center container for adding player UI components.
     */
    public VBox getCenterContainer() {
        return centerContainer;
    }

    /**
     * Sets the breakdown panel content in the right pane.
     */
    public void setBreakdownPanel(javafx.scene.layout.Pane panel) {
        rightPane.getChildren().clear();
        if (panel != null) {
            rightPane.getChildren().add(panel);
        }
    }

    /**
     * Gets the right pane for manual manipulation if needed.
     */
    public Pane getRightPane() {
        return rightPane;
    }

    /**
     * Gets the BorderPane root.
     */
    public BorderPane getRoot() {
        return root;
    }

    /**
     * Gets the header box.
     */
    public HBox getHeader() {
        return headerBox;
    }

    /**
     * Gets the mode buttons box for adding Total/Tour buttons.
     */
    public HBox getModeButtonsBox() {
        return modeButtonsBox;
    }

    /**
     * Gets the mode label.
     */
    public Label getModeLabel() {
        return modeLabel;
    }

    /**
     * Sets the center content to display a different pane (for switching between Total and Tour modes)
     */
    public void setCenterContent(javafx.scene.Node content) {
        javafx.scene.control.SplitPane splitPane = (javafx.scene.control.SplitPane) root.getCenter();
        if (splitPane != null && splitPane.getItems().size() > 0) {
            ScrollPane centerScroll = new ScrollPane(content);
            centerScroll.setFitToWidth(true);
            centerScroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
            centerScroll.setBackground(Background.EMPTY);
            centerScroll.setPrefViewportHeight(400);

            splitPane.getItems().set(0, centerScroll);
        }
    }
}
