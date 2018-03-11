package org.sourceheads.bridjfx.taskbar;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Slider;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * (...)
 *
 * @author Stefan Fiedler
 */
public class TaskbarListProgressDemo extends Application {

    protected TaskbarList taskbarList;

    @Override
    public void start(final Stage primaryStage) throws Exception {
        final Slider slider = new Slider(0, 100, 0);
        slider.setShowTickMarks(true);
        slider.valueProperty().addListener((observable, oldValue, newValue) ->
                taskbarList.setProgressValue(newValue.longValue(), 100));

        final HBox hbButtons = new HBox(
                new ValueButton(slider, 25),
                new ValueButton(slider, 50),
                new ValueButton(slider, 75),
                new ValueButton(slider, 100),
                new StateButton("Normal", ProgressState.NORMAL),
                new StateButton("Paused", ProgressState.PAUSED),
                new StateButton("Error", ProgressState.ERROR),
                new StateButton("Indeterminate", ProgressState.INDETERMINATE),
                new StateButton("No progress", ProgressState.NOPROGRESS));
        hbButtons.setSpacing(8);

        final VBox vBox = new VBox(slider, hbButtons);
        vBox.setSpacing(8);
        vBox.setPadding(new Insets(8, 8, 8, 8));

        final Scene scene = new Scene(vBox);
        primaryStage.setScene(scene);
        primaryStage.setTitle("TaskbarList progress demo");

        primaryStage.show();

        taskbarList.init(primaryStage);
    }

    @Override
    public void init() throws Exception {
        super.init();
        taskbarList = new TaskbarList();
    }

    @Override
    public void stop() throws Exception {
        super.stop();
        taskbarList.destroy();
    }

    public static void main(final String... args) {
        launch(args);
    }

    private class ValueButton extends Button {

        public ValueButton(final Slider slider, final long value) {
            super(String.valueOf(value));
            setOnAction(event -> slider.setValue(value));
        }
    }

    private class StateButton extends Button {

        public StateButton(final String text, final ProgressState state) {
            super(text);
            setOnAction(event -> taskbarList.setProgressState(state));
        }
    }
}
