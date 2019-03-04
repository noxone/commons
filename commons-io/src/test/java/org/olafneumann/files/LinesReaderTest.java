package org.olafneumann.files;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

@SuppressWarnings("javadoc")
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class LinesReaderTest {
	private static final String FIVE_LINES_FILE = "fiveLines.txt";

	private static final String TEST1_LOG_FILE = "test1.log";

	private static final String TEST2_LOG_FILE = "test2.log";

	private static LinesReader createLinesReaderFromResource(final String resourceName) throws IOException {
		Path path;
		try {
			path = Paths.get(LinesReaderTest.class.getResource(resourceName).toURI());
		} catch (final URISyntaxException e) {
			throw new RuntimeException("Invalid resource name", e);
		}
		return new LinesReader(path, StandardCharsets.UTF_8);
	}

	private static BufferedReader createBufferedReaderFromResource(final String resourceName) throws IOException {
		Path path;
		try {
			path = Paths.get(LinesReaderTest.class.getResource(resourceName).toURI());
		} catch (final URISyntaxException e) {
			throw new RuntimeException("Invalid resource name", e);
		}
		return Files.newBufferedReader(path);
	}

	private static boolean startsWithWhitespace(final String line) {
		if (line.length() > 0) {
			return line.charAt(0) <= ' ';
		}
		return false;
	}

	private static boolean startsWithOpeningBracket(final String line) {
		return !line.isEmpty() && line.charAt(0) == '[';
	}

	private static boolean isPartOfStackTrace(final String line) {
		return line.length() > 0 //
				&& (Character.isWhitespace(line.charAt(0))//
						|| line.startsWith("Caused by: "));
	}

	private static String getThreadName(final String line) {
		// final int start = line.indexOf("[") + 1;
		// final int stop = line.indexOf("]");
		final int endClock = line.indexOf("]");
		final int start = line.indexOf(":", endClock) + 2;
		final int stop = line.indexOf(":", start);
		if (start >= 0 && stop > start) {
			return line.substring(start, stop);
		}
		return null;
	}

	@BeforeClass
	public static void debugPoint() {
		System.out.println("BeforeClass");
	}

	@AfterClass
	public static void afterClass() {
		System.out.println("AfterClass");
	}

	@Test
	public void readFiveCompoundLines() throws IOException {
		List<String> lines;
		try (LinesReader reader = createLinesReaderFromResource(FIVE_LINES_FILE)) {
			lines = reader.compoundLines(LinesReaderTest::startsWithWhitespace).collect(toList());
		}

		assertThat(lines.size()).isEqualTo(5);
		final String thirdLine
				= "and the LinesReader" + System.lineSeparator() + " should" + System.lineSeparator() + " 	read";
		assertThat(lines.get(2)).isEqualTo(thirdLine);
	}

	@Test
	public void collectCompoundLines() throws IOException {
		List<String> lines;
		try (LinesReader reader = createLinesReaderFromResource(TEST1_LOG_FILE)) {
			lines = reader.compoundLines(LinesReaderTest::isPartOfStackTrace).collect(toList());
		}

		assertThat(lines.size()).isEqualTo(641);
	}

	@Test
	public void countGroupsWithMax30_713790() throws IOException {
		final AtomicInteger ai = new AtomicInteger(0);
		long count;
		try (LinesReader reader = createLinesReaderFromResource(TEST2_LOG_FILE)) {
			count = reader.groups(LinesReaderTest::startsWithWhitespace, LinesReaderTest::getThreadName, x -> {
				if (ai.incrementAndGet() == 30) {
					ai.set(0);
					return LineType.End;
				}
				return LineType.Middle;
			}).count();
		}

		assertThat(count).isEqualTo(21320);
	}

	@Test
	public void speed_countLines_buffered() throws IOException {
		long count;
		try (BufferedReader reader = createBufferedReaderFromResource(TEST2_LOG_FILE)) {
			count = reader.lines().count();
		}
		assertThat(count).isEqualTo(20501941L);
	}

	@Test
	public void speed_countLines_lines_713790() throws IOException {
		long count;
		try (LinesReader reader = createLinesReaderFromResource(TEST2_LOG_FILE)) {
			count = reader.lines().count();
		}
		assertThat(count).isEqualTo(20501941L);
	}

	@Test
	public void speed_countCompoundLines_713790() throws IOException {
		long count;
		try (LinesReader reader = createLinesReaderFromResource(TEST2_LOG_FILE)) {
			count = reader.compoundLines(line -> !startsWithOpeningBracket(line)).count();
		}

		assertThat(count).isEqualTo(20493279L);
	}

	@Test
	public void speed_countGroupsWithBootstraps_713790() throws IOException {
		long count;
		try (LinesReader reader = createLinesReaderFromResource(TEST2_LOG_FILE)) {
			count = reader//
					.groups(line -> !startsWithOpeningBracket(line), //
							LinesReaderTest::getThreadName, //
							line -> line.endsWith(": Request done") ? LineType.End : LineType.Middle)
					.count();
		}

		assertThat(count).isEqualTo(643870L);
	}
}
