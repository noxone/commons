package org.olafneumann.filesystem;

import static org.olafneumann.filesystem.FilesystemEventType.INITIAL;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO javadoc
 *
 * @see <a href=
 *      "https://github.com/filesystem-watcher/filesystem-watcher">github</a>
 */
public class FilesystemReader implements FilesystemNotifier {
	private static final Logger LOGGER = LoggerFactory.getLogger(FilesystemReader.class);

	private final Path watchedPath;

	private final FilesystemConstraints watchedConstraints;

	private final Consumer<FilesystemEvent> watchedConsumer;

	/**
	 * TODO javadoc
	 *
	 * @param watchedPath
	 * @param watchedConstraints
	 * @param watchedConsumer
	 */
	FilesystemReader(final Path watchedPath,
			final FilesystemConstraints watchedConstraints,
			final Consumer<FilesystemEvent> watchedConsumer) {
		this.watchedPath = watchedPath;
		this.watchedConstraints = watchedConstraints;
		this.watchedConsumer = watchedConsumer;
	}

	/** {@inheritDoc} */
	@Override
	public void startWatching() {
		final ConstraintsFilteringVisitor visitor = new ConstraintsFilteringVisitor(watchedPath, watchedConstraints);
		try {
			LOGGER.info("Reading started path={}", watchedPath);
			Files.walkFileTree(watchedPath, visitor);
			LOGGER.info("Reading completed path={}", watchedPath);
		} catch (final IOException e) {
			throw new UncheckedIOException(e);
		}

		visitor.getEvents().forEach(watchedConsumer);
	}

	/** {@inheritDoc} */
	@Override
	public void stopWatching() {
		// For now we do not need to implement this. Maybe later to make this class more
		// responsive to stop.
	}

	private static final class ConstraintsFilteringVisitor extends SimpleFileVisitor<Path> {

		private final Path watchedPatch;

		private final FilesystemConstraints constraints;

		private final List<FilesystemEvent> events = new ArrayList<>();

		private ConstraintsFilteringVisitor(final Path watchedPatch, final FilesystemConstraints constraints) {
			super();
			this.watchedPatch = watchedPatch;
			this.constraints = constraints;
		}

		@Override
		public FileVisitResult preVisitDirectory(final @Nullable Path dir, final @Nullable BasicFileAttributes attrs)
				throws IOException {
			Objects.requireNonNull(dir);
			super.preVisitDirectory(dir, attrs);

			if (dir.equals(watchedPatch)) {
				return FileVisitResult.CONTINUE;
			}

			addFilesystemEvent(dir);

			if (!constraints.isRecursive()) {
				return FileVisitResult.SKIP_SUBTREE;
			}

			return FileVisitResult.CONTINUE;
		}

		@Override
		public FileVisitResult visitFile(final @Nullable Path file, final @Nullable BasicFileAttributes attrs)
				throws IOException {
			Objects.requireNonNull(file);
			super.visitFile(file, attrs);
			addFilesystemEvent(file);
			return FileVisitResult.CONTINUE;
		}

		private void addFilesystemEvent(final Path path) {
			final FilesystemEvent filesystemEvent = FilesystemEvent.of(path, INITIAL);

			if (constraints.test(filesystemEvent)) {
				events.add(filesystemEvent);
				LOGGER.info("Created event: {}", filesystemEvent);
			}
		}

		public List<FilesystemEvent> getEvents() {
			return events;
		}

		@Override
		public int hashCode() {
			return Objects.hash(constraints, events, watchedPatch);
		}

		@Override
		public boolean equals(final @Nullable Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (getClass() != obj.getClass()) {
				return false;
			}
			final ConstraintsFilteringVisitor other = (ConstraintsFilteringVisitor) obj;
			return Objects.equals(constraints, other.constraints)
					&& Objects.equals(events, other.events)
					&& Objects.equals(watchedPatch, other.watchedPatch);
		}
	}
}
