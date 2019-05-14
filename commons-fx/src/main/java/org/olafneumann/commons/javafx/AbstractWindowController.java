package org.olafneumann.commons.javafx;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextInputDialog;
import javafx.scene.image.Image;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.WindowEvent;

@SuppressWarnings("restriction")
abstract class AbstractWindowController {
	private static String getName(final Class<?> clazz) {
		String name = clazz.getSimpleName();
		if (name.toLowerCase().endsWith("controller")) {
			name = name.substring(0, name.length() - "controller".length());
		}
		return name;
	}

	/**
	 * TODO Write JavaDoc
	 *
	 * @param <T>
	 * @param controllerClass
	 * @param stage
	 * @return
	 */
	protected static <T> T load(final Class<T> controllerClass, final Stage stage) {
		// Prepare our knowledge about the window
		final String name = getName(controllerClass);
		final String path = controllerClass.getPackage().getName() + "/";

		// Load window information
		final FXMLLoader loader = new FXMLLoader();
		Parent root;
		try (InputStream stream = getResourceAsStream(path + name + ".fxml")) {
			if (stream == null) {
				throw new RuntimeException("Cannot find FXML file for " + controllerClass.getName());
			}
			root = loader.load(stream);
		} catch (final IOException e) {
			throw new RuntimeException("Unable to load window for " + controllerClass.getName(), e);
		}

		// Create and set up the window
		final Scene scene = new Scene(root);
		stage.setScene(scene);
		final T controller = loader.getController();
		if (controller instanceof AbstractWindowController) {
			final AbstractWindowController atc = (AbstractWindowController) controller;
			atc.stage = stage;
			stage.setOnShowing(atc::onShowing);
			stage.setOnShown(atc::onShown);
			stage.setOnCloseRequest(atc::onCloseRequest);
			stage.setOnHiding(atc::onHiding);
			stage.setOnHidden(atc::onHidden);
		}

		// Maybe make the window beautiful
		for (final int size : new int[] { 16, 24, 32, 40, 48, 64, 72, 96, 128 }) {
			addIcon(stage, path + name + "_" + size + ".png");
		}
		return controller;
	}

	private static InputStream getResourceAsStream(final String path) {
		return AbstractWindowController.class.getClassLoader().getResourceAsStream(path);
	}

	private static void addIcon(final Stage stage, final String path) {
		try (InputStream stream = getResourceAsStream(path)) {
			if (stream != null) {
				final Image image = new Image(stream);
				stage.getIcons().add(image);
			}
		} catch (final IOException ignore) {
			// do nothing
		}
	}

	protected static <T> T createWindow(final Class<T> controllerClass,
			final Stage owner,
			final String title,
			final StageStyle stageStyle,
			final Modality modality) {
		final Stage stage = new Stage(stageStyle);
		stage.initModality(modality);
		stage.initOwner(owner);
		stage.setTitle(title);
		final T controller = load(controllerClass, stage);
		return controller;
	}

	protected <T> T createWindow(final Class<T> controllerClass,
			final String title,
			final StageStyle stageStyle,
			final Modality modality) {
		return createWindow(controllerClass, getStage(), title, stageStyle, modality);
	}

	protected Properties getProperties() {
		final Properties properties = new Properties();
		try (InputStream stream = getClass().getResourceAsStream(getName(getClass()) + ".properties")) {
			properties.load(stream);
		} catch (final IOException ignore) {
			// do nothing
		}
		return properties;
	}

	private Stage stage;

	protected Stage getStage() {
		return stage;
	}

	/**
	 * Method that is called when the controller is initialized by JavaFX.<br>
	 * The implementation in {@link AbstractWindowController} does nothing so it can
	 * be overwritten by sub classes.
	 */
	@FXML
	protected void initialize() {
		// empty to be overwritten by sub classes
	}

	protected void close() {
		stage.close();
	}

	protected void show() {
		getStage().show();
	}

	protected void showAndWait() {
		getStage().showAndWait();
	}

	protected void onShowing(final WindowEvent event) {
		// empty to be overwritten by sub classes
	}

	protected void onShown(final WindowEvent event) {
		// empty to be overwritten by sub classes
	}

	protected void onCloseRequest(final WindowEvent event) {
		// empty to be overwritten by sub classes
	}

	protected void onHiding(final WindowEvent event) {
		// empty to be overwritten by sub classes
	}

	protected void onHidden(final WindowEvent event) {
		// empty to be overwritten by sub classes
	}

	private static void enhance(final Stage owner, final Alert alert) {
		if (owner != null) {
			((Stage) alert.getDialogPane().getScene().getWindow()).initOwner(owner);
			((Stage) alert.getDialogPane().getScene().getWindow()).getIcons().addAll(owner.getIcons());
		}
	}

	public static void showMessageBoxOK(final Stage owner, final String title, final String message) {
		showMessageBoxOK(owner, title, null, message);
	}

	public static void showMessageBoxOK(final Stage owner,
			final String title,
			final String header,
			final String message) {
		final Alert alert = new Alert(AlertType.INFORMATION);
		alert.setTitle(title);
		alert.setHeaderText(header);
		alert.setContentText(message);
		enhance(owner, alert);
		alert.showAndWait();
	}

	public static boolean showMessageBoxYesNo(final Stage owner, final String text) {
		return showMessageBoxYesNo(owner, null, text);
	}

	public static boolean showMessageBoxYesNo(final Stage owner, final String header, final String text) {
		final Alert alert = new Alert(AlertType.CONFIRMATION, text, ButtonType.NO, ButtonType.YES);
		alert.setHeaderText(header);
		enhance(owner, alert);
		return alert.showAndWait().get() == ButtonType.YES;
	}

	public static void showMessageBoxError(final Stage owner, final String text, final Throwable throwable) {
		showMessageBoxError(owner, text, throwable.getLocalizedMessage(), throwable);
	}

	public static void showMessageBoxError(final Stage owner,
			final String text,
			final String message,
			final Throwable throwable) {
		final Alert alert = new Alert(AlertType.ERROR);
		alert.setTitle("Error");
		alert.setHeaderText(text);
		alert.setContentText(message);

		// Create expandable Exception.
		final StringWriter sw = new StringWriter();
		final PrintWriter pw = new PrintWriter(sw);
		throwable.printStackTrace(pw);
		final String exceptionText = sw.toString();

		final Label label = new Label("The stacktrace for this exception:");

		final TextArea textArea = new TextArea(exceptionText);
		textArea.setEditable(false);
		textArea.setWrapText(true);

		textArea.setMaxWidth(Double.MAX_VALUE);
		textArea.setMaxHeight(Double.MAX_VALUE);
		GridPane.setVgrow(textArea, Priority.ALWAYS);
		GridPane.setHgrow(textArea, Priority.ALWAYS);

		final GridPane expContent = new GridPane();
		expContent.setMaxWidth(Double.MAX_VALUE);
		expContent.add(label, 0, 0);
		expContent.add(textArea, 0, 1);

		// Set expandable Exception into the dialog pane.
		alert.getDialogPane().setExpandableContent(expContent);
		enhance(owner, alert);
		alert.showAndWait();
	}

	public static Optional<String> showMessageBoxInput(final String title,
			final String header,
			final String input,
			final String defaultValue) {
		final TextInputDialog dialog = new TextInputDialog(defaultValue);
		dialog.setTitle(title);
		dialog.setHeaderText(header);
		dialog.setContentText(input);
		return dialog.showAndWait();
	}

	protected static Future<?> run(final Runnable anything) {
		return runAndGui(anything, null);
	}

	protected static Future<?> runAndGui(final Runnable anything, final Runnable javaFx) {
		return ForkJoinPool.commonPool().submit(() -> {
			anything.run();
			if (javaFx != null) {
				Platform.runLater(javaFx);
			}
		});
	}

	/**
	 * Runs an action A in a background thread and uses the output of that action A
	 * as input for another action B that will be executed in the UI thread. This is
	 * useful to execute a long running task and once it's finished we
	 * 
	 * @param <T>
	 * @param supplier
	 * @param consumer
	 */
	protected static <T> void produceAndGui(final Supplier<T> supplier, final Consumer<T> consumer) {
		ForkJoinPool.commonPool().execute(() -> {
			final T value = supplier.get();
			Platform.runLater(() -> consumer.accept(value));
		});
	}
}
