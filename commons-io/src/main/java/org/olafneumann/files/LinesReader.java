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
import java.util.Iterator;
import java.util.LinkedHashMap;
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
	public LinesReader(BufferedReader reader) {
		this.reader = Objects.requireNonNull(reader);
	}

	public LinesReader(InputStream in, Charset charset) {
		this(new BufferedReader(new InputStreamReader(in, charset.newDecoder()), BUFFER_SIZE));
	}

	public LinesReader(Path path, Charset charset) throws IOException {
		this(Files.newBufferedReader(path, charset));
	}

	public LinesReader(File file, Charset charset) throws IOException {
		this(file.toPath(), charset);
	}

	private BufferedReader getBufferedReader() {
		return reader;
	}

	@Override
	public void close() throws IOException {
		getBufferedReader().close();
	}

	public Stream<String> lines() {
		return getBufferedReader().lines();
	}

	public Iterator<String> compoundLinesIterator(Predicate<String> appendToPreviousLine) {
		return new CompoundLinesIterator(getBufferedReader(), appendToPreviousLine);
	}

	private static class CompoundLinesIterator extends BufferingIterator<String> {
		private String currentLine = null;
		private final List<String> lines = new ArrayList<>(1000);

		private final BufferedReader reader;
		private final Predicate<String> appendToPreviousLine;

		private CompoundLinesIterator(BufferedReader reader, Predicate<String> appendToPreviousLine) {
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
				} else if (lineCount > 1) {
					return String.join(System.lineSeparator(), lines);
				} else {
					return null;
				}
			} finally {
				lines.clear();
			}
		}
	}

	public Stream<String> compoundLines(Predicate<String> appendToPreviousLine) {
		return StreamSupport.stream(//
				// Spliterators.spliteratorUnknownSize(iter, Spliterator.ORDERED |
				// Spliterator.NONNULL)//
				Spliterators.spliterator(compoundLinesIterator(appendToPreviousLine), 100,
						Spliterator.ORDERED | Spliterator.NONNULL)//
				, true);
	}

	public <G> Iterator<List<String>> groupsIterator(Predicate<String> appendToPreviousLine,
			Function<String, G> determineGroup, Function<String, LineType> determineEntryType) {
		final Iterator<String> linesIterator = compoundLinesIterator(appendToPreviousLine);
		return new GroupedIterator(linesIterator, determineGroup, determineEntryType);
	}

	private static class GroupedIterator extends BufferingIterator<List<String>> {
		private Map<Object, List<String>> groups = new LinkedHashMap<>();

		private final Iterator<String> linesIterator;
		private final Function<String, ?> determineGroup;
		private final Function<String, LineType> determineEntryType;

		private GroupedIterator(Iterator<String> linesIterator, Function<String, ?> determineGroup,
				Function<String, LineType> determineEntryType) {
			this.linesIterator = linesIterator;
			this.determineGroup = determineGroup;
			this.determineEntryType = determineEntryType;
		}

		@Override
		protected List<String> readItem() {
			List<String> list = null;
			while (linesIterator.hasNext() && list == null) {
				String line = linesIterator.next();
				Object groupId = determineGroup.apply(line);
				LineType type = determineEntryType.apply(line);

				// the group
				List<String> group = groups.get(groupId);
				if (group == null) {
					group = new LinkedList<>();
					groups.put(groupId, group);
				}

				// handling
				if (type == LineType.Normal) {
					group.add(line);
				} else if (type == LineType.Start) {
					if (group.size() == 0) {
						group.add(line);
					} else {
						group = new LinkedList<>();
						group.add(line);
						list = groups.put(groupId, group);
					}
				} else if (type == LineType.End) {
					group.add(line);
					list = groups.put(groupId, new LinkedList<>());
				} else {
					throw new RuntimeException("Unkown entry type: " + type.name());
				}
			}
			if (list == null) {
				return groups.entrySet()//
						.stream()//
						.filter(e -> !e.getValue().isEmpty())//
						.findFirst()//
						.map(e -> groups.remove(e.getKey()))//
						.orElse(null);
			}
			return list;
		}
	}

	public <G> Stream<List<String>> groups(Predicate<String> appendToPreviousLine, Function<String, G> determineGroup,
			Function<String, LineType> determineEntryType) {
		return StreamSupport.stream(
				Spliterators.spliterator(groupsIterator(appendToPreviousLine, determineGroup, determineEntryType), 100,
						Spliterator.ORDERED | Spliterator.NONNULL),
				true);
	}
}
