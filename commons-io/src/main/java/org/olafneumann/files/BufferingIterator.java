package org.olafneumann.files;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Iterator;
import java.util.NoSuchElementException;

abstract class BufferingIterator<T> implements Iterator<T> {
	private T next;

	@Override
	public boolean hasNext() {
		if (next != null) {
			return true;
		} else {
			try {
				next = readItem();
				return (next != null);
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}
	}

	protected abstract T readItem() throws IOException;

	@Override
	public T next() {
		if (next != null || hasNext()) {
			T line = next;
			next = null;
			return line;
		} else {
			throw new NoSuchElementException();
		}
	}
}