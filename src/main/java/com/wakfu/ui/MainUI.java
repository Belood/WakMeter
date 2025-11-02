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

    public MainUI(Stage primaryStage) {
        this.primaryStage = primaryStage;
        this.root = new BorderPane();
        this.headerBox = createHeader();
        this.appStatusLabel = new Label("App Status: \"En attente\"");
        this.centerContainer = new VBox(6);
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
        topSection.getChildren().addAll(headerBox, appStatusLabel);

        // Padding pour appStatusLabel
        appStatusLabel.setPadding(new Insets(0, 10, 0, 15));

        // Center: Players list (scrollable)
        ScrollPane centerScroll = new ScrollPane(centerContainer);
        centerScroll.setFitToWidth(true);
        centerScroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        centerScroll.setBackground(Background.EMPTY);
        centerScroll.setPrefViewportHeight(400);
        centerContainer.setFillWidth(true);

        // Right pane: Breakdown (initially empty)
        rightPane.setStyle("-fx-border-color: #333; -fx-border-width: 0 0 0 1;");
        rightPane.setPrefWidth(400);
        rightPane.setMinWidth(350);

        // Layout with SplitPane or HBox
        HBox centerAndRight = new HBox(0);
        HBox.setHgrow(centerScroll, Priority.ALWAYS);
        centerAndRight.getChildren().addAll(centerScroll, rightPane);

        root.setTop(topSection);
        root.setCenter(centerAndRight);
        root.setPadding(new Insets(0));

        // Set up the stage
        javafx.scene.Scene scene = new javafx.scene.Scene(root, 1000, 600);
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
}
