package org.olafneumann.files;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

@SuppressWarnings("javadoc")
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

	private static boolean startsWithWhitespace(final String line) {
		if (line.length() > 0) {
			final char c = line.charAt(0);
			return c == ' ' || c == '\t' || c == '\r' || c == '\n' || c == '\f';
		}
		return false;
	}

	private static boolean isPartOfStackTrace(final String line) {
		return line.length() > 0 //
				&& (Character.isWhitespace(line.charAt(0))//
						|| line.startsWith("Caused by: "));
	}

	private static String getThreadName(final String line) {
		final int start = line.indexOf("[") + 1;
		final int stop = line.indexOf("]");
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
				return LineType.Normal;
			}).count();
		}

		assertThat(count).isEqualTo(21320);
	}

	@Test
	public void speed_countLines_713790() throws IOException {
		long count;
		try (LinesReader reader = createLinesReaderFromResource(TEST2_LOG_FILE)) {
			count = reader.lines().count();
		}
		assertThat(count).isEqualTo(713790);
	}

	@Test
	public void speed_countCompoundLines_713790() throws IOException {
		long count;
		try (LinesReader reader = createLinesReaderFromResource(TEST2_LOG_FILE)) {
			count = reader.compoundLines(LinesReaderTest::isPartOfStackTrace).count();
		}

		assertThat(count).isEqualTo(634590);
	}

	@Test
	public void speed_countGroupsWithBootstraps_713790() throws IOException {
		long count;
		try (LinesReader reader = createLinesReaderFromResource(TEST2_LOG_FILE)) {
			count = reader
					.groups(LinesReaderTest::startsWithWhitespace, LinesReaderTest::getThreadName,
							line -> line.endsWith("Bootstrapping Oversigt") ? LineType.Start : LineType.Normal)
					.count();
		}

		assertThat(count).isEqualTo(11915);
	}
}
