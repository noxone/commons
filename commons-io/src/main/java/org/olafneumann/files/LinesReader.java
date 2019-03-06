package org.olafneumann.files;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * A reader that is able to collect multiple consecutive to one "item". It is
 * also able to group lines depending on user defined basis.
 *
 * @author noxone
 *
 */
public class LinesReader implements AutoCloseable {
	private static final int BUFFER_SIZE = 1 * 1024 * 1024;// 1 MB

	/**
	 * The underlying character-input stream.
	 */
	protected BufferedReader reader;

	/**
	 * Creates a new reader.
	 *
	 * @param reader a {@link BufferedReader} object providing the underlying
	 *               stream.
	 * @throws NullPointerException if <code>reader</code> is <code>null</code>
	 */
	public LinesReader(final BufferedReader reader) {
		this.reader = Objects.requireNonNull(reader);
	}

	/**
	 * Creates a new reader.
	 *
	 * @param in      {@link InputStream} object providing the underlying stream
	 * @param charset the charset to use for decoding
	 */
	public LinesReader(final InputStream in, final Charset charset) {
		this(new BufferedReader(new InputStreamReader(in, charset.newDecoder()), BUFFER_SIZE));
	}

	/**
	 * Creates a new reader.
	 *
	 * @param path    the file to read
	 * @param charset the charset to use for decoding
	 * @throws IOException if an I/O error occurs opening the file
	 */
	public LinesReader(final Path path, final Charset charset) throws IOException {
		this(Files.newBufferedReader(path, charset));
	}

	/**
	 * @param file    the file to read
	 * @param charset the charset to use for decoding
	 * @throws IOException if an I/O error occurs opening the file
	 */
	public LinesReader(final File file, final Charset charset) throws IOException {
		this(file.toPath(), charset);
	}

	private BufferedReader getBufferedReader() {
		return reader;
	}

	/** {@inheritDoc} */
	@Override
	public void close() throws IOException {
		getBufferedReader().close();
	}

	/**
	 * Stream the lines of the underlying reader
	 *
	 * @return a {@link Stream} of lines from the underlying reader
	 */
	public Stream<String> lines() {
		return getBufferedReader().lines();
	}

	/**
	 * Iterate over the lines of the underlying reader concatenated by a user
	 * defined {@link Predicate}.
	 *
	 * @param appendToPreviousLine
	 * @return an {@link Iterator} of lines from the underlying reader concatenated
	 *         by the denoted {@link Predicate}
	 */
	public Iterator<String> compoundLinesIterator(final Predicate<String> appendToPreviousLine) {
		return new CompoundLinesIterator(getBufferedReader(), appendToPreviousLine);
	}

	private static class CompoundLinesIterator extends AbstractIterator<String> {
		private String currentLine = null;

		private final List<String> lines = new ArrayList<>(1000);

		private final BufferedReader reader;

		private final Predicate<String> appendToPreviousLine;

		private CompoundLinesIterator(final BufferedReader reader, final Predicate<String> appendToPreviousLine) {
			this.reader = reader;
			this.appendToPreviousLine = appendToPreviousLine;
		}

		@Override
		protected String readItem() throws IOException {
			if (currentLine != null) {
				lines.add(currentLine);
				// currentLine will automatically be set to null below if the end of the stream
				// is reached
			} else {
				final String readLine = reader.readLine();
				if (readLine != null) {
					lines.add(readLine);
				} else {
					return null;
				}
			}
			String line;
			while ((line = reader.readLine()) != null //
					&& appendToPreviousLine.test(line)) {
				lines.add(line);
			}
			currentLine = line;

			// return the correct lines
			try {
				final int lineCount = lines.size();
				if (lineCount == 1) {
					return lines.get(0);
				}
				return String.join(System.lineSeparator(), lines);
			} finally {
				lines.clear();
			}
		}
	}

	/**
	 * Stream the lines of the underlying reader concatenated by a user defined
	 * {@link Predicate}.
	 *
	 * @param appendToPreviousLine
	 * @return a {@link Stream} of lines from the underlying reader concatenated by
	 *         the denoted {@link Predicate}
	 */
	public Stream<String> compoundLines(final Predicate<String> appendToPreviousLine) {
		return StreamSupport.stream(//
				// Spliterators.spliteratorUnknownSize(iter, Spliterator.ORDERED |
				// Spliterator.NONNULL)//
				Spliterators.spliterator(compoundLinesIterator(appendToPreviousLine), 100,
						Spliterator.ORDERED | Spliterator.NONNULL)//
				, true);
	}

	/**
	 * Iterate over the concatenated lines of the underlying reader grouped by a
	 * user defined {@link Predicate}.
	 *
	 * @param                      <G> the type of group identifier
	 * @param appendToPreviousLine whether of not to concatenate the tested line
	 *                             with the previous one
	 * @param determineGroup       determine the group id of the tested line
	 * @param determineEntryType   determine the type of the tested line
	 * @return an {@link Iterator} of the concatenated lines of the underlying
	 *         reader grouped by a user defined {@link Predicate}
	 */
	public <G> Iterator<List<String>> groupsIterator(final Predicate<String> appendToPreviousLine,
			final Function<String, G> determineGroup,
			final Function<String, LineType> determineEntryType) {
		final Iterator<String> linesIterator = compoundLinesIterator(appendToPreviousLine);
		return new GroupedIterator(linesIterator, determineGroup, determineEntryType);
	}

	private static class GroupedIterator extends AbstractIterator<List<String>> {
		private Map<Object, List<String>> groups = new HashMap<>();

		private final Iterator<String> linesIterator;

		private final Function<String, ?> determineGroup;

		private final Function<String, LineType> determineEntryType;

		private GroupedIterator(final Iterator<String> linesIterator,
				final Function<String, ?> determineGroup,
				final Function<String, LineType> determineEntryType) {
			this.linesIterator = linesIterator;
			this.determineGroup = determineGroup;
			this.determineEntryType = determineEntryType;
		}

		@Override
		protected List<String> readItem() {
			while (linesIterator.hasNext()) {
				final String line = linesIterator.next();
				final Object groupId = determineGroup.apply(line);
				final LineType lineType = determineEntryType.apply(line);

				List<String> groupOfCurrentLine = groups
						.computeIfAbsent(groupId, key -> new LinkedList<>());

				if (lineType == LineType.End) {
					groupOfCurrentLine.add(line);
					return groups.remove(groupId);
				}
				if (lineType == LineType.Start
						&& !groupOfCurrentLine.isEmpty()) {
					groupOfCurrentLine = new LinkedList<>();
					groupOfCurrentLine.add(line);
					return groups.put(groupId, groupOfCurrentLine);
				}

				groupOfCurrentLine.add(line);
			}

			return groups//
					.keySet().stream().findFirst().map(groups::remove)
					.orElse(null);
		}
	}

	/**
	 * Stream the concatenated lines of the underlying reader grouped by a user
	 * defined {@link Predicate}.
	 *
	 * @param                      <G> the type of group identifier
	 * @param appendToPreviousLine whether of not to concatenate the tested line
	 *                             with the previous one
	 * @param determineGroup       determine the group id of the tested line
	 * @param determineEntryType   determine the type of the tested line
	 * @return a {@link Stream} of the concatenated lines of the underlying reader
	 *         grouped by a user defined {@link Predicate}
	 */
	public <G> Stream<List<String>> groups(final Predicate<String> appendToPreviousLine,
			final Function<String, G> determineGroup,
			final Function<String, LineType> determineEntryType) {
		return StreamSupport.stream(
				Spliterators.spliterator(groupsIterator(appendToPreviousLine, determineGroup, determineEntryType), 100,
						Spliterator.ORDERED | Spliterator.NONNULL),
				true);
	}
}
