package org.olafneumann.commons.javafx;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Properties;

import javafx.application.Application;
import javafx.application.HostServices;
import javafx.fxml.FXML;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.WindowEvent;

/**
 * A simple controller providing default functionality for JavaFX windows.
 *
 * @author Olaf Neumann
 *
 */
public abstract class AbstractWindowController {
	private Stage stage;

	private HostServices hostServices;

	/**
	 * Set the {@link Application}'s {@link HostServices} so the window has
	 * immediate access to it.
	 *
	 * @param hostServices the host services to set.
	 */
	final void setHostServices(final HostServices hostServices) {
		this.hostServices = hostServices;
	}

	/**
	 * Set the stage for current window. This method should only be called once when
	 * initializing the controller.
	 *
	 * @param stage the stage to set
	 */
	final void setStage(final Stage stage) {
		this.stage = stage;
	}

	/**
	 * Return the stage used to show the current window.
	 *
	 * @return the stage
	 */
	protected Stage getStage() {
		return stage;
	}

	/**
	 * Get the current {@link Application}'s {@link HostServices}.
	 *
	 * @return the current {@link HostServices}
	 */
	protected HostServices getHostServices() {
		return hostServices;
	}

	/**
	 * Opens a file denoted be the given URI
	 *
	 * @param uri the file to open
	 */
	protected void openFile(final URI uri) {
		getHostServices().showDocument(uri.toString());
	}

	/**
	 * Create a child window to the current window
	 *
	 * @param <T>             The type of the window's controller
	 * @param controllerClass the controller class to attach to the window
	 * @param title           the text to be shown as the window's title
	 * @param stageStyle      defines the type of window to show
	 * @param modality        defines the window's modality
	 * @return the instance of the window's controller
	 */
	protected <T> T createWindow(final Class<T> controllerClass,
			final String title,
			final StageStyle stageStyle,
			final Modality modality) {
		return FXMLWindowLoader.createWindow(controllerClass, getStage(), title, stageStyle, modality);
	}

	/**
	 * Returns a property object that is extracted from a property file named ofter
	 * the controller class.
	 *
	 * @return the properties loaded.
	 */
	protected Properties getProperties() {
		final Properties properties = new Properties();
		try (InputStream stream
				= getClass().getResourceAsStream(FXMLWindowLoader.getName(getClass()) + ".properties")) {
			properties.load(stream);
		} catch (@SuppressWarnings("unused") final IOException ignore) {
			// do nothing
		}
		return properties;
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

	/**
	 * Close the current window.
	 */
	protected void close() {
		stage.close();
	}

	/**
	 * Show the current window. This method returns immediately so that your calling
	 * code can continue working on other stuff.
	 */
	protected void show() {
		getStage().show();
	}

	/**
	 * Show the current window and block until the window is hidden again.
	 */
	protected void showAndWait() {
		getStage().showAndWait();
	}

	/**
	 * The method will be called just before the window will be shown.<br>
	 * The default implementation does nothing but subclasses may override to add
	 * special event handling.
	 *
	 * @param event the event that triggered this method
	 */
	protected void onShowing(@SuppressWarnings("unused") final WindowEvent event) {
		// empty to be overwritten by sub classes
	}

	/**
	 * The method will be called right after the window has been shown.<br>
	 * The default implementation does nothing but subclasses may override to add
	 * special event handling.
	 *
	 * @param event the event that triggered this method
	 */
	protected void onShown(@SuppressWarnings("unused") final WindowEvent event) {
		// empty to be overwritten by sub classes
	}

	/**
	 * The method will be called when the window is requested to be closed.<br>
	 * The default implementation does nothing but subclasses may override to add
	 * special event handling.
	 *
	 * @param event the event that triggered this method
	 */
	protected void onCloseRequest(@SuppressWarnings("unused") final WindowEvent event) {
		// empty to be overwritten by sub classes
	}

	/**
	 * The method will be called just before the window is about to be hidden.<br>
	 * The default implementation does nothing but subclasses may override to add
	 * special event handling.
	 *
	 * @param event the event that triggered this method
	 */
	protected void onHiding(@SuppressWarnings("unused") final WindowEvent event) {
		// empty to be overwritten by sub classes
	}

	/**
	 * The method will be called after the window has been hidden.<br>
	 * The default implementation does nothing but subclasses may override to add
	 * special event handling.
	 *
	 * @param event the event that triggered this method
	 */
	protected void onHidden(@SuppressWarnings("unused") final WindowEvent event) {
		// empty to be overwritten by sub classes
	}
}
