package org.olafneumann.filesystem;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.WatchEvent;
import java.util.Objects;

import javax.annotation.Nullable;

/**
 * TODO javadoc
 *
 * @see <a href=
 *      "https://github.com/filesystem-watcher/filesystem-watcher">github</a>
 */
public class FilesystemEvent {
	private final Path path;

	private final FilesystemEventType eventType;

	/**
	 * TODO javadoc
	 *
	 * @param event
	 * @param path
	 * @return
	 */
	static FilesystemEvent of(final WatchEvent<?> event, final Path path) {
		final Path watchedElement = (Path) event.context();
		final Path totalPath = Paths.get(path.toString(), watchedElement.toString());
		return new FilesystemEvent(totalPath, FilesystemEventType.of(event.kind()));
	}

	/**
	 * TODO javadoc
	 *
	 * @param path
	 * @param eventType
	 * @return
	 */
	static FilesystemEvent of(final Path path, final FilesystemEventType eventType) {
		return new FilesystemEvent(path, eventType);
	}

	private FilesystemEvent(final Path path, final FilesystemEventType eventType) {
		this.path = path;
		this.eventType = eventType;
	}

	/**
	 * @return the path
	 */
	public Path getPath() {
		return path;
	}

	/**
	 * @return the eventType
	 */
	public FilesystemEventType getEventType() {
		return eventType;
	}

	/** {@inheritDoc} */
	@Override
	public String toString() {
		return String.format("FilesystemEvent [path=%s, eventType=%s]", path, eventType);
	}

	/** {@inheritDoc} */
	@Override
	public int hashCode() {
		return Objects.hash(eventType, path);
	}

	/** {@inheritDoc} */
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
		final FilesystemEvent other = (FilesystemEvent) obj;
		return eventType == other.eventType && Objects.equals(path, other.path);
	}
}
