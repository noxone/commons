package org.olafneumann.filesystem;

import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * TODO javadoc
 *
 * @see <a href=
 *      "https://github.com/filesystem-watcher/filesystem-watcher">github</a>
 */
public class FilesystemMonitor implements FilesystemNotifier {

	private final Path watchedPath;

	private final Consumer<FilesystemEvent> watchedConsumer;

	private final FilesystemConstraints watchedConstraints;

	private final BlockingQueue<FilesystemEvent> queue = new ArrayBlockingQueue<>(100000);

	private final ExecutorService producersExecutor = Executors
			.newSingleThreadExecutor(r -> new Thread(r, "filesystem-monitor-producers" + UUID.randomUUID().toString()));

	private final ExecutorService consumersExecutor = Executors
			.newSingleThreadExecutor(r -> new Thread(r, "filesystem-monitor-consumers" + UUID.randomUUID().toString()));

	private Optional<FilesystemNotifier> watcher = Optional.empty();

	private Optional<FilesystemNotifier> reader = Optional.empty();

	private FilesystemMonitor(final Path watchedPath,
			final Consumer<FilesystemEvent> watchedConsumer,
			final FilesystemConstraints watchedConstraints) {
		this.watchedPath = watchedPath;
		this.watchedConsumer = watchedConsumer;
		this.watchedConstraints = watchedConstraints;
	}

	/** {@inheritDoc} */
	@Override
	public synchronized void startWatching() {
		watcher = Optional.of(new FilesystemWatcher(
				watchedPath,
				watchedConstraints,
				watchedConsumer,
				queue,
				producersExecutor,
				consumersExecutor));

		reader = Optional.of(new FilesystemReader(watchedPath, watchedConstraints, this::consumeEvent));

		watcher.get().startWatching();
		reader.get().startWatching();
	}

	/** {@inheritDoc} */
	@Override
	public synchronized void stopWatching() {
		if (!(reader.isPresent() && watcher.isPresent())) {
			throw new IllegalStateException("The FilesystemMonitor has not been properly initialized and started.");
		}
		reader.get().stopWatching();
		watcher.get().stopWatching();
	}

	private void consumeEvent(final FilesystemEvent event) {
		try {
			queue.put(event);
		} catch (@SuppressWarnings("unused") final InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	/**
	 * Create a new builder to eventually create a {@link FilesystemMonitor} object.
	 *
	 * @return the newly created builder
	 */
	public static Builder builder() {
		return new Builder();
	}

	/**
	 * TODO javadoc
	 */
	public static class Builder {

		private Optional<Path> watchedPath = Optional.empty();

		private Optional<Consumer<FilesystemEvent>> watchedConsumer = Optional.empty();

		private FilesystemConstraints watchedConstraints = FilesystemConstraints.DEFAULT;

		/**
		 * TODO javadoc
		 *
		 * @return
		 */
		public FilesystemMonitor build() {
			return new FilesystemMonitor(watchedPath.get(), watchedConsumer.get(), watchedConstraints);
		}

		/**
		 * @param watchedPath the watchedPath to set
		 * @return
		 */
		public Builder watchedPath(final Path watchedPath) {
			this.watchedPath = Optional.of(watchedPath);
			return this;
		}

		/**
		 * @param watchedConsumer the watchedConsumer to set
		 * @return
		 */
		public Builder watchedConsumer(final Consumer<FilesystemEvent> watchedConsumer) {
			this.watchedConsumer = Optional.of(watchedConsumer);
			return this;
		}

		/**
		 * @param watchedConstraints the watchedConstraints to set
		 * @return
		 */
		public Builder watchedConstraints(final FilesystemConstraints watchedConstraints) {
			this.watchedConstraints = Objects.requireNonNull(watchedConstraints);
			return this;
		}
	}
}
