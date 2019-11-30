package org.olafneumann.filesystem;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;
import static org.olafneumann.filesystem.FilesystemEventType.CREATED;
import static org.olafneumann.filesystem.FilesystemEventType.DELETED;
import static org.olafneumann.filesystem.FilesystemEventType.INITIAL;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.Watchable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO javadoc
 *
 * @see <a href=
 *      "https://github.com/filesystem-watcher/filesystem-watcher">github</a>
 */
public class FilesystemWatcher implements FilesystemNotifier {
	private static final Logger LOGGER = LoggerFactory.getLogger(FilesystemWatcher.class);

	private final WatchableUtility watchableUtility = new WatchableUtility();

	private final Path watchedPath;

	private final FilesystemConstraints watchedConstraints;

	private final Consumer<FilesystemEvent> watchedConsumer;

	private final BlockingQueue<FilesystemEvent> blockingQueue;

	private final ExecutorService producersExecutor;

	private final ExecutorService consumersExecutor;

	private final Map<Path, WatchKey> watchedKeys = new ConcurrentHashMap<>();

	/**
	 * TODO javadoc
	 *
	 * @param watchedPath
	 * @param watchedConstraints
	 * @param watchedConsumer
	 * @param blockingQueue
	 * @param producersExecutor
	 * @param consumersExecutor
	 */
	FilesystemWatcher(final Path watchedPath,
			final FilesystemConstraints watchedConstraints,
			final Consumer<FilesystemEvent> watchedConsumer,
			final BlockingQueue<FilesystemEvent> blockingQueue,
			final ExecutorService producersExecutor,
			final ExecutorService consumersExecutor) {
		this.watchedPath = watchedPath;
		this.watchedConstraints = watchedConstraints;
		this.watchedConsumer = watchedConsumer;
		this.blockingQueue = blockingQueue;
		this.producersExecutor = producersExecutor;
		this.consumersExecutor = consumersExecutor;
	}

	/** TODO javadoc */
	@Override
	public void startWatching() {
		startWatching(watchedPath);
		consumersExecutor.submit(this::consumeEvents);
		producersExecutor.submit(this::produceEvents);
	}

	/** TODO javadoc */
	@Override
	public void stopWatching() {
		try {
			watchableUtility.closeWatchService();
		} finally {
			blockingQueue.clear();
			producersExecutor.shutdownNow();
			consumersExecutor.shutdownNow();
		}
	}

	private void startWatching(final Path path) {
		final WatchKey key = watchableUtility.registerWatchable(path);
		watchedKeys.putIfAbsent(path, key);
		LOGGER.info("Watching started: {}", path);
	}

	private void stopWatching(final Path path) {
		final WatchKey key = watchedKeys.get(path);
		key.cancel();
		watchedKeys.remove(path);
		LOGGER.info("Watching stopped: {}", path);
	}

	private void produceEvents() {
		while (!Thread.currentThread().isInterrupted()) {
			try {
				final WatchKey watchedKey = watchableUtility.getWatchService().take();

				LOGGER.info("Watched key: {}", watchedKey.watchable());

				for (final WatchEvent<?> watchEvent : watchedKey.pollEvents()) {
					if (Thread.currentThread().isInterrupted()) {
						return;
					}

					LOGGER.info("Watched event: {} {}", watchEvent.kind(), watchEvent.context());

					if (watchEvent.kind() == OVERFLOW) {
						LOGGER.error(
								"OVERFLOW watchEvent occurred {} times. Operating system queue was overflowed. File events could be lost.",
								watchEvent.count());
						continue;
					}

					final Path watchedDirectory = (Path) watchedKey.watchable();
					final FilesystemEvent filesystemEvent = FilesystemEvent.of(watchEvent, watchedDirectory);

					if (!watchedConstraints.test(filesystemEvent)) {
						continue;
					}

					blockingQueue.put(filesystemEvent);
					watchedKey.reset();
				}
			} catch (@SuppressWarnings("unused") final InterruptedException e) {
				Thread.currentThread().interrupt();
			} catch (final Exception e) {
				LOGGER.error("", e);
			}
		}
	}

	private void consumeEvents() {
		while (!Thread.currentThread().isInterrupted()) {
			try {
				final FilesystemEvent event = blockingQueue.take();

				final Path path = event.getPath();
				if (Files.isDirectory(path)) {
					if (List.of(CREATED, INITIAL).contains(event.getEventType())) {
						startWatching(path);
						// TODO refactor
						if (CREATED == event.getEventType()) {
							new FilesystemReader(path, watchedConstraints, filesystemEvent -> {
								if (!watchedKeys.containsKey(filesystemEvent.getPath())) {
									watchedConsumer.accept(FilesystemEvent.of(filesystemEvent.getPath(), CREATED));
								}
							}).startWatching();
						}
					} else if (DELETED == event.getEventType()) {
						stopWatching(path);
					}
				}
				LOGGER.info("Consumed event: " + event);

				watchedConsumer.accept(event);
			} catch (@SuppressWarnings("unused") final InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}
	}

	/**
	 * @return the watchableUtility
	 */
	public WatchableUtility getWatchableUtility() {
		return watchableUtility;
	}

	private static class WatchableUtility {
		private static final WatchEvent.Kind<?>[] ALL_EVENT_KINDS
				= new WatchEvent.Kind[] { ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY, OVERFLOW };

		private final WatchService watchService = createWatchService();

		private WatchService createWatchService() {
			try (FileSystem fileSystem = FileSystems.getDefault()) {
				return fileSystem.newWatchService();
			} catch (final IOException e) {
				throw new UncheckedIOException(e);
			}
		}

		private WatchKey registerWatchable(final Watchable watchable) {
			try {
				return watchable.register(watchService, ALL_EVENT_KINDS);
			} catch (final IOException e) {
				throw new UncheckedIOException(e);
			}
		}

		void closeWatchService() {
			try {
				watchService.close();
			} catch (final IOException e) {
				throw new UncheckedIOException(e);
			}
		}

		/**
		 * @return the watchService
		 */
		public WatchService getWatchService() {
			return watchService;
		}
	}
}
