package org.olafneumann.files;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * An abstract implementation that only requires the reading of items. This
 * implementation handles all the {@link Iterator} stuff.
 *
 * @author noxone
 *
 * @param <T> the type of items to iterate
 */
abstract class AbstractIterator<T> implements Iterator<T> {
	private T next;

	/** {@inheritDoc} */
	@Override
	public boolean hasNext() {
		if (next != null) {
			return true;
		}
		try {
			next = readItem();
			return next != null;
		} catch (final IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	/**
	 * Reads the next item from to be returned to the caller.
	 *
	 * @return the next item or <code>null</code> if the end of input has been
	 *         reached
	 * @throws IOException if something on the input fails
	 */
	protected abstract T readItem() throws IOException;

	/** {@inheritDoc} */
	@Override
	public T next() {
		if (next != null || hasNext()) {
			final T line = next;
			next = null;
			return line;
		}
		throw new NoSuchElementException();
	}
}
