package org.olafneumann.commons.javafx;

import java.io.IOException;
import java.io.InputStream;

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
		// Prepare our knowledge about the window
		final String name = getName(controllerClass);
		final String path = controllerClass.getPackage().getName() + "/";
		final String filename = path + name + ".fxml";

		// Load window information
		final FXMLLoader loader = new FXMLLoader();
		Parent root;
		try (InputStream stream = getResourceAsStream(controllerClass, filename)) {
			if (stream == null) {
				throw new RuntimeException(
						"Cannot find FXML file for " + controllerClass.getName() + ". Expected file name: " + filename);
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
			atc.setStage(stage);
			stage.setOnShowing(atc::onShowing);
			stage.setOnShown(atc::onShown);
			stage.setOnCloseRequest(atc::onCloseRequest);
			stage.setOnHiding(atc::onHiding);
			stage.setOnHidden(atc::onHidden);
		}

		// Maybe make the window beautiful
		for (final int size : ICON_SIZES) {
			addIcon(controllerClass, stage, path + name + "_" + size + ".png");
		}
		return controller;
	}

	private static InputStream getResourceAsStream(final Class<?> relativClass, final String path) {
		return relativClass.getClassLoader().getResourceAsStream(path);
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
