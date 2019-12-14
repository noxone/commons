package org.olafneumann.filesystem.parameters;

import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;
import java.util.function.Function;

import javax.annotation.Nullable;

import org.olafneumann.filesystem.FilesystemEventType;
import org.olafneumann.filesystem.utilities.FilesystemUtils;

@SuppressWarnings("javadoc")
public enum ModificationKind implements Function<Path, Path> {
	ADD_POSIX_PERMISSION(ModificationKind::addPermission, FilesystemEventType.MODIFIED),
	REMOVE_POSIX_PERMISSION(ModificationKind::removePermission, FilesystemEventType.MODIFIED),
	SET_SAME_PERMISSIONS(ModificationKind::setSamePermissions, FilesystemEventType.MODIFIED);

	private final Function<Path, Path> fileModifier;

	private final FilesystemEventType expectedEvent;

	private ModificationKind(final Function<Path, Path> fileModifier, final FilesystemEventType expectedEvent) {
		this.fileModifier = fileModifier;
		this.expectedEvent = expectedEvent;
	}

	public FilesystemEventType getExpectedEvent() {
		return expectedEvent;
	}

	@Override
	public Path apply(final @Nullable Path path) {
		return fileModifier.apply(path);
	}

	private static Path addPermission(final Path path) {
		final Set<PosixFilePermission> permissions = FilesystemUtils.getPosixFilePermissions(path);
		permissions.add(PosixFilePermission.OTHERS_EXECUTE);
		return FilesystemUtils.setPosixFilePermissions(path, permissions);
	}

	private static Path removePermission(final Path path) {
		final Set<PosixFilePermission> permissions = FilesystemUtils.getPosixFilePermissions(path);
		final PosixFilePermission toRemove = permissions.stream()
				.filter(permission -> !permission.toString().startsWith("OWNER"))
				.findAny()
				.orElseThrow(() -> new IllegalArgumentException(
						String.format("Cannot remove POSIX attribute, because given path=%s have all them set to false",
								path)));
		permissions.remove(toRemove);
		return FilesystemUtils.setPosixFilePermissions(path, permissions);
	}

	private static Path setSamePermissions(final Path path) {
		final Set<PosixFilePermission> permissions = FilesystemUtils.getPosixFilePermissions(path);
		return FilesystemUtils.setPosixFilePermissions(path, permissions);
	}

}
