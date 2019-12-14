package org.olafneumann.commons.javafx;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/**
 * A generic loader for JavaFX windows.
 *
 * @author Olaf Neumann
 *
 */
public abstract class FXWindowLoader {
	private static final int[] ICON_SIZES = { 16, 24, 32, 40, 48, 64, 72, 96, 128, 256, 512 };

	private static InputStream getResourceAsStream(final Class<?> relativClass, final String path) {
		return relativClass.getClassLoader().getResourceAsStream(path);
	}

	/**
	 * Generate a common name from the given class name. This name is expected to be
	 * used for FXML file names.
	 *
	 * @param clazz the class to derive the name from
	 * @return the derived name
	 */
	public static String getName(final Class<?> clazz) {
		String name = clazz.getSimpleName();
		if (name.toLowerCase().endsWith("controller")) {
			name = name.substring(0, name.length() - "controller".length());
		}
		return name;
	}

	private static <T> String getNameWithPath(final Class<T> controllerClass) {
		final String name = getName(controllerClass);
		final String path = controllerClass.getPackage().getName() + "/";
		return path + name;
	}

	/**
	 * Load a window with default settings.This method does a lot of magic:
	 * <ul>
	 * <li>abstract a common name from the controller class name</li>
	 * <li>Load an FXML file based on the generated name</li>
	 * <li>Set up a scene for the loaded FXML stuff</li>
	 * <li>If the controller extends {@link AbstractWindowController} the
	 * controllers extra functionality will be initialized</li>
	 * <li>add icons to the stage (based on the generated name)</li>
	 * </ul>
	 * This loaded uses the convention that all FXML, property and icon files use a
	 * name that is derived from the controller's class name. The derived name is
	 * identical to the controller's class name; on exception: if the class name
	 * ends with "controller" it will be truncated.
	 *
	 * @param <T>             the type of the controller class
	 * @param controllerClass the controller class to control the window
	 * @param stage           the stage to use for the scene
	 * @return the newly create controller instance
	 */
	protected static <T> T load(final Class<T> controllerClass, final Stage stage) {
		// Load window information
		final FXMLLoader loader = new FXMLLoader();
		Parent root;
		try (InputStream stream = getFxmlInputStream(controllerClass, getNameWithPath(controllerClass) + ".fxml")) {
			root = loader.load(stream);
		} catch (final IOException e) {
			throw new RuntimeException("Unable to load window for " + controllerClass.getName(), e);
		}

		// Create and set up the window
		final Scene scene = new Scene(root);
		stage.setScene(scene);
		final T controller = loader.getController();
		if (controller instanceof AbstractWindowController) {
			initAbstractWindowController((AbstractWindowController) controller, stage);
		}

		addIcons(controllerClass, stage, getNameWithPath(controllerClass) + "_%s.png");
		return controller;
	}

	private static <T> InputStream getFxmlInputStream(final Class<T> controllerClass, final String filename) {
		return Objects.requireNonNull(getResourceAsStream(controllerClass, filename),
				"Cannot find FXML file for " + controllerClass.getName() + ". Expected file name: " + filename);
	}

	private static void initAbstractWindowController(final AbstractWindowController atc, final Stage stage) {
		atc.setStage(stage);
		stage.setOnShowing(atc::onShowing);
		stage.setOnShown(atc::onShown);
		stage.setOnCloseRequest(atc::onCloseRequest);
		stage.setOnHiding(atc::onHiding);
		stage.setOnHidden(atc::onHidden);
	}

	/**
	 * Add icons the given window based on the given filename format
	 *
	 * @param <T>             the type of the controller's class
	 * @param controllerClass the controller class
	 * @param stage           the window to add the icons to
	 * @param filenameFormat  a format to be used to generate icon filenames. This
	 *                        will be used with
	 *                        {@link String#format(String, Object...)} to insert an
	 *                        integer value. Example: <code>icon_%s.png</code>
	 */
	public static <T> void addIcons(final Class<T> controllerClass, final Stage stage, final String filenameFormat) {
		for (final int size : ICON_SIZES) {
			addIcon(controllerClass, stage, String.format(filenameFormat, size));
		}
	}

	private static void addIcon(final Class<?> relativClass, final Stage stage, final String path) {
		try (InputStream stream = getResourceAsStream(relativClass, path)) {
			if (stream != null) {
				final Image image = new Image(stream);
				stage.getIcons().add(image);
			}
		} catch (@SuppressWarnings("unused") final IOException ignore) {
			// do nothing
		}
	}

	/**
	 * Create a child window to the current window
	 *
	 * @param <T>             The type of the window's controller
	 * @param controllerClass the controller class to attach to the window
	 * @param owner           the owner of the window to be created (for modality)
	 * @param title           the text to be shown as the window's title
	 * @param stageStyle      defines the type of window to show
	 * @param modality        defines the window's modality
	 * @return the instance of the window's controller
	 */
	public static <T> T createWindow(final Class<T> controllerClass,
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
}
