package org.olafneumann.filesystem;

import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.util.Map;

/**
 * TODO javadoc
 *
 * @see <a href=
 *      "https://github.com/filesystem-watcher/filesystem-watcher">github</a>
 */
public enum FilesystemEventType {
	/** TODO javadoc */
	INITIAL,
	/** TODO javadoc */
	CREATED,
	/** TODO javadoc */
	DELETED,
	/** TODO javadoc */
	MODIFIED;

	private static final Map<WatchEvent.Kind<?>, FilesystemEventType> CORRESPONDING_WATCH_KINDS = Map.of(
			StandardWatchEventKinds.ENTRY_CREATE, FilesystemEventType.CREATED, StandardWatchEventKinds.ENTRY_DELETE,
			FilesystemEventType.DELETED, StandardWatchEventKinds.ENTRY_MODIFY, FilesystemEventType.MODIFIED);

	/**
	 * TODO javadoc
	 *
	 * @param kind
	 * @return
	 */
	static FilesystemEventType of(final WatchEvent.Kind<?> kind) {
		return CORRESPONDING_WATCH_KINDS.get(kind);
	}
}
