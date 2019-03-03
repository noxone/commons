package org.olafneumann.files;

/**
 * The type of line read by the {@link LinesReader}.
 *
 * @author noxone
 *
 */
public enum LineType {
	/** Defines the start of a group read by the {@link LinesReader} */
	Start,
	/** Defines a normal line read by the {@link LinesReader} */
	Normal,
	/** Defines the end of a group read by the {@link LinesReader} */
	End;
}
