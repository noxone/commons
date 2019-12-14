package org.olafneumann.commons.javafx;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Optional;

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
abstract class MessageBoxes {
	private static void enhance(final Stage owner, final Alert alert) {
		if (owner != null) {
			((Stage) alert.getDialogPane().getScene().getWindow()).initOwner(owner);
			((Stage) alert.getDialogPane().getScene().getWindow()).getIcons().addAll(owner.getIcons());
		}
	}

	/**
	 * TODO write javadoc
	 *
	 * @param owner
	 * @param title
	 * @param message
	 */
	public static void showMessageBoxOK(final Stage owner, final String title, final String message) {
		showMessageBoxOK(owner, title, null, message);
	}

	/**
	 * TODO write javadoc
	 *
	 * @param owner
	 * @param title
	 * @param header
	 * @param message
	 */
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

	/**
	 * TODO write javadoc
	 *
	 * @param owner
	 * @param text
	 * @return
	 */
	public static boolean showMessageBoxYesNo(final Stage owner, final String text) {
		return showMessageBoxYesNo(owner, null, text);
	}

	/**
	 * TODO write javadoc
	 *
	 * @param owner
	 * @param header
	 * @param text
	 * @return
	 */
	public static boolean showMessageBoxYesNo(final Stage owner, final String header, final String text) {
		final Alert alert = new Alert(AlertType.CONFIRMATION, text, ButtonType.NO, ButtonType.YES);
		alert.setHeaderText(header);
		enhance(owner, alert);
		return alert.showAndWait().get() == ButtonType.YES;
	}

	/**
	 * TODO write javadoc
	 *
	 * @param owner
	 * @param text
	 * @param throwable
	 */
	public static void showMessageBoxError(final Stage owner, final String text, final Throwable throwable) {
		showMessageBoxError(owner, text, throwable.getLocalizedMessage(), throwable);
	}

	/**
	 * TODO write javadoc
	 *
	 * @param owner
	 * @param text
	 * @param message
	 * @param throwable
	 */
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

	/**
	 * TODO write javadoc
	 * 
	 * @param title
	 * @param header
	 * @param input
	 * @param defaultValue
	 * @return
	 */
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
}
