package org.olafneumann.filesystem.utilities;

import java.util.Collection;
import java.util.Map;

import org.awaitility.Awaitility;
import org.awaitility.Duration;

@SuppressWarnings("javadoc")
public class AwaitilityUtils {

	private static final Duration DEFAULT_WAIT_TIME = Duration.FIVE_HUNDRED_MILLISECONDS;

	public static void awaitForSize(final Collection<?> collection, final int expectedSize) {
		Awaitility.await().atMost(DEFAULT_WAIT_TIME).until(() -> collection.size() == expectedSize);
	}

	public static void awaitForSize(final Map<?, ?> map, final int expectedSize) {
		Awaitility.await().atMost(DEFAULT_WAIT_TIME).until(() -> map.size() == expectedSize);
	}
}
