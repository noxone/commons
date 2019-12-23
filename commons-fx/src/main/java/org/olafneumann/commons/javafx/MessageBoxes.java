package org.olafneumann.commons.javafx;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Optional;

import javax.annotation.Nullable;

import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.stage.Stage;

/**
 * Utility class providing several methods to show message boxes.
 *
 * @author Olaf Neumann
 *
 */
public final class MessageBoxes {
	private static void enhance(final Optional<Stage> optionalOwner, final Alert alert) {
		optionalOwner.ifPresent(owner -> {
			((Stage) alert.getDialogPane().getScene().getWindow()).initOwner(owner);
			((Stage) alert.getDialogPane().getScene().getWindow()).getIcons().addAll(owner.getIcons());
		});
	}

	/**
	 * Display a customized {@link Alert} and wait until the user closes it. This
	 * method replaces the buttons displayed for the alert before showing the alert.
	 * 
	 * @param alert       the message box to display
	 * @param buttonTypes the type of buttons to show
	 * @return An {@link Optional} that contains the result. Refer to the
	 *         {@link Dialog} class documentation for more detail.
	 */
	public static Optional<ButtonType> showWithButtonsAndWait(final Alert alert, final ButtonType... buttonTypes) {
		alert.getButtonTypes().setAll(buttonTypes);
		return alert.showAndWait();
	}

	/**
	 * Show a simple text message box with an OK button.
	 *
	 * @param owner   the owner window of the message box
	 * @param title   the text to be shown as the title
	 * @param message the text to be shown as the main message.
	 */
	public static void showMessageBoxOK(final Optional<Stage> owner, final String title, final String message) {
		showMessageBoxOK(owner, title, Optional.empty(), message);
	}

	/**
	 * Show a simple text message box with an OK button.
	 *
	 * @param owner   the owner window of the message box
	 * @param title   the text to be shown as the title
	 * @param header  text to be shown as a header above the main message
	 * @param message the text to be shown as the main message.
	 */
	public static void showMessageBoxOK(final Optional<Stage> owner,
			final String title,
			final Optional<String> header,
			final String message) {
		createMessageBoxOK(owner, title, header, message).showAndWait();
	}

	/**
	 * Create a simple text message box with an OK button without showing it. <br/>
	 * This method is ment to create a message box that can be further modified by
	 * the requesting code.
	 *
	 * @param owner   the owner window of the message box
	 * @param title   the text to be shown as the title
	 * @param header  text to be shown as a header above the main message
	 * @param message the text to be shown as the main message.
	 * @return the message box created. This box is ready to be shown or you may
	 *         change details before shoing it.
	 */
	public static Alert createMessageBoxOK(final Optional<Stage> owner,
			final String title,
			final Optional<String> header,
			final String message) {
		final Alert alert = new Alert(AlertType.INFORMATION);
		alert.setTitle(title);
		header.ifPresent(alert::setHeaderText);
		alert.setContentText(message);
		enhance(owner, alert);
		return alert;
	}

	/**
	 * Show a message box with two buttons: YES and NO
	 *
	 * @param owner the owner window of the message box
	 * @param text  the question to be displayed to the user
	 * @return <code>true</code> if the user chose <code>YES</code>
	 */
	public static boolean showMessageBoxYesNo(final Optional<Stage> owner, final String text) {
		return showMessageBoxYesNo(owner, Optional.empty(), text);
	}

	/**
	 * Show a message box with two buttons: YES and NO
	 *
	 * @param owner  the owner window of the message box
	 * @param header text to be shown as a header above the main message
	 * @param text   the question to be displayed to the user
	 * @return <code>true</code> if the user chose <code>YES</code>
	 */
	public static boolean showMessageBoxYesNo(final Optional<Stage> owner,
			final Optional<String> header,
			final String text) {
		return createMessageBoxYesNo(owner, header, text).showAndWait().get() == ButtonType.YES;
	}

	/**
	 * Create a message box with two buttons: YES and NO without showing it. <br/>
	 * This method is ment to create a message box that can be further modified by
	 * the requesting code.
	 *
	 * @param owner  the owner window of the message box
	 * @param header text to be shown as a header above the main message
	 * @param text   the question to be displayed to the user
	 * @return the message box created. This box is ready to be shown or you may
	 *         change details before shoing it.
	 */
	public static Alert createMessageBoxYesNo(final Optional<Stage> owner,
			final Optional<String> header,
			final String text) {
		final Alert alert = new Alert(AlertType.CONFIRMATION, text, ButtonType.NO, ButtonType.YES);
		header.ifPresent(alert::setHeaderText);
		enhance(owner, alert);
		return alert;
	}

	/**
	 * Show an error message to the user.
	 *
	 * @param owner     the owner window of the message box
	 * @param text      the error message to be displayed to the user
	 * @param throwable the exception to be displayed to the user
	 */
	public static void showMessageBoxError(final Optional<Stage> owner, final String text, final Throwable throwable) {
		showMessageBoxError(owner, text, throwable.getLocalizedMessage(), throwable);
	}

	/**
	 * Show an error message to the user.
	 *
	 * @param owner     the owner window of the message box
	 * @param text      the error message to be displayed to the user
	 * @param message   an additional message describing the error
	 * @param throwable the exception to be displayed to the user
	 */
	public static void showMessageBoxError(final Optional<Stage> owner,
			final String text,
			final String message,
			final Throwable throwable) {
		createMessageBoxError(owner, text, message, throwable).showAndWait();
	}

	/**
	 * Create a message box that can display an error message to the user without
	 * showing it. <br/>
	 * This method is ment to create a message box that can be further modified by
	 * the requesting code.
	 *
	 * @param owner     the owner window of the message box
	 * @param text      the error message to be displayed to the user
	 * @param message   an additional message describing the error
	 * @param throwable the exception to be displayed to the user
	 * @return the message box created. This box is ready to be shown or you may
	 *         change details before shoing it.
	 */
	public static Alert createMessageBoxError(final Optional<Stage> owner,
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
		return alert;
	}

	/**
	 * Show a box to the user asking for string input
	 *
	 * @param title        the text to be shown as the title
	 * @param header       text to be shown as a header above the main message
	 * @param input        Text asking for input
	 * @param defaultValue the default value to be used if the user presses CANCEL
	 * @return
	 */
	public static Optional<String> showMessageBoxInput(final String title,
			final String header,
			final String input,
			@Nullable final String defaultValue) {
		final TextInputDialog dialog = new TextInputDialog(defaultValue);
		dialog.setTitle(title);
		dialog.setHeaderText(header);
		dialog.setContentText(input);
		return dialog.showAndWait();
	}

	private MessageBoxes() {
		throw new RuntimeException();
	}
}
