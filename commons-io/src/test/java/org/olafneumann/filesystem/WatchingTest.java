package org.olafneumann.filesystem;

import static org.olafneumann.filesystem.FilesystemEventType.CREATED;
import static org.olafneumann.filesystem.FilesystemEventType.DELETED;
import static org.olafneumann.filesystem.FilesystemEventType.INITIAL;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.olafneumann.filesystem.parameters.ModificationKind;
import org.olafneumann.filesystem.parameters.PathKind;
import org.olafneumann.filesystem.utilities.AwaitilityUtils;
import org.olafneumann.filesystem.utilities.FilesystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("javadoc")
class WatchingTest {
	private static final Logger LOGGER = LoggerFactory.getLogger(WatchingTest.class);

	private static Stream<Arguments> allPathKinds() {
		return Arrays.stream(PathKind.values()).map(Arguments::of);
	}

	private static Stream<Arguments> allModificationKindsOnAllPathKinds() {
		return Arrays.stream(PathKind.values())
				.flatMap(pk -> Arrays.stream(ModificationKind.values()).map(mk -> Arguments.of(pk, mk)));
	}

	@Test
	void shouldNotEmitEventsForEmptyDirectory(@TempDir final Path temporaryFolder) throws InterruptedException {
		// given
		final List<FilesystemEvent> receivedEvents = new ArrayList<>();
		final FilesystemMonitor monitor
				= FilesystemMonitor.builder().watchedPath(temporaryFolder).watchedConsumer(receivedEvents::add).build();

		// when
		monitor.startWatching();
		// TODO Monitor should initialize recursive directory watchers if possible
		// before exiting from startWatching method.
		Thread.sleep(100);

		// then
		Assertions.assertThat(receivedEvents).isEmpty();
	}

	@ParameterizedTest
	@MethodSource("allPathKinds")
	void shouldWatchCreations(final PathKind pathKind, @TempDir final Path temporaryDirectory)
			throws InterruptedException {
		// given
		final ConcurrentHashMap<UUID, FilesystemEvent> receivedEvents = new ConcurrentHashMap<>();
		final FilesystemNotifier monitor = FilesystemMonitor.builder()
				.watchedPath(temporaryDirectory)
				.watchedConstraints(FilesystemConstraints.DEFAULT.withRecursive(true))
				.watchedConsumer(filesystemEvent -> {
					LOGGER.info("ADDED: {}", filesystemEvent);
					receivedEvents.put(UUID.randomUUID(), filesystemEvent);
				})
				.build();
		monitor.startWatching();

		// when
		final PathKind.PathScenario setup = pathKind.apply(temporaryDirectory);
		final List<Path> createdPaths = setup.getAllPaths();
		// TODO Monitor should initialize recursive directory watchers if possible
		// before exiting from startWatching method.
		Thread.sleep(100);

		// then
		AwaitilityUtils.awaitForSize(receivedEvents, createdPaths.size());
		final List<FilesystemEvent> createdEvents
				= createdPaths.stream().map(path -> new FilesystemEvent(path, CREATED)).collect(Collectors.toList());
		Assertions.assertThat(receivedEvents.values()).containsExactlyInAnyOrderElementsOf(createdEvents);
	}

	@ParameterizedTest
	@MethodSource("allPathKinds")
	void shouldWatchDeletions(final PathKind pathKind, @TempDir final Path temporaryDirectory)
			throws InterruptedException {
		// given
		final PathKind.PathScenario setup = pathKind.apply(temporaryDirectory);
		final Path subjectPath = setup.getSubjectPath();

		// when
		final List<FilesystemEvent> receivedEvents = Collections.synchronizedList(new ArrayList<>());
		final FilesystemNotifier monitor = FilesystemMonitor.builder()
				.watchedPath(temporaryDirectory)
				.watchedConstraints(FilesystemConstraints.DEFAULT.withRecursive(true))
				.watchedConsumer(receivedEvents::add)
				.build();

		// when
		monitor.startWatching();
		// TODO Monitor should initialize recursive directory watchers if possible
		// before exiting from startWatching method.
		Thread.sleep(100);

		FilesystemUtils.delete(subjectPath);

		// then
		AwaitilityUtils.awaitForSize(receivedEvents, setup.getAllPaths().size() + 1);
		final Stream<FilesystemEvent> initial
				= setup.getAllPaths().stream().map(path -> new FilesystemEvent(path, INITIAL));
		final Stream<FilesystemEvent> modified = Stream.of(new FilesystemEvent(setup.getSubjectPath(), DELETED));
		final List<FilesystemEvent> expected = Stream.concat(initial, modified).collect(Collectors.toList());
		Assertions.assertThat(receivedEvents).containsExactlyInAnyOrderElementsOf(expected);
	}

	@ParameterizedTest
	@MethodSource("allModificationKindsOnAllPathKinds")
	void shouldWatchModifications(final PathKind pathKind,
			final ModificationKind strategy,
			@TempDir final Path temporaryDirectory) throws InterruptedException {
		// given
		final PathKind.PathScenario setup = pathKind.apply(temporaryDirectory);
		final Path testedPath = setup.getTestedPath();

		// when
		final List<FilesystemEvent> receivedEvents = new CopyOnWriteArrayList<>();
		final FilesystemNotifier monitor = FilesystemMonitor.builder()
				.watchedPath(temporaryDirectory)
				.watchedConstraints(FilesystemConstraints.DEFAULT.withRecursive(true))
				.watchedConsumer(filesystemEvent -> {
					LOGGER.info("Event: {}", filesystemEvent);
					receivedEvents.add(filesystemEvent);
				})
				.build();

		// when
		monitor.startWatching();
		// TODO Monitor should initialize recursive directory watchers if possible
		// before exiting from startWatching method.
		Thread.sleep(100);
		strategy.apply(testedPath);

		// then
		AwaitilityUtils.awaitForSize(receivedEvents, setup.getAllPaths().size() + 1);
		final Stream<FilesystemEvent> initial
				= setup.getAllPaths().stream().map(path -> new FilesystemEvent(path, INITIAL));
		final Stream<FilesystemEvent> modified
				= Stream.of(new FilesystemEvent(setup.getSubjectPath(), strategy.getExpectedEvent()));
		final List<FilesystemEvent> expected = Stream.concat(initial, modified).collect(Collectors.toList());
		Assertions.assertThat(receivedEvents).containsExactlyInAnyOrderElementsOf(expected);
	}
}
