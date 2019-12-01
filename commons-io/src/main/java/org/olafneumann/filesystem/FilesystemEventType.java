package org.olafneumann.filesystem;

import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.util.Collections;
import java.util.LinkedHashMap;
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

	private static final Map<WatchEvent.Kind<?>, FilesystemEventType> CORRESPONDING_WATCH_KINDS;
	static {
		final Map<WatchEvent.Kind<?>, FilesystemEventType> kinds = new LinkedHashMap<>();
		kinds.put(StandardWatchEventKinds.ENTRY_CREATE, FilesystemEventType.CREATED);
		kinds.put(StandardWatchEventKinds.ENTRY_DELETE, FilesystemEventType.DELETED);
		kinds.put(StandardWatchEventKinds.ENTRY_MODIFY, FilesystemEventType.MODIFIED);
		CORRESPONDING_WATCH_KINDS = Collections.unmodifiableMap(kinds);
	}

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
