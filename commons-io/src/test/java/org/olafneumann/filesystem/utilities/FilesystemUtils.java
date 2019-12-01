package org.olafneumann.filesystem.utilities;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;
import java.util.function.Supplier;

@SuppressWarnings("javadoc")
public class FilesystemUtils {

	private final Path temporaryFolder;

	public FilesystemUtils(final Path temporaryFolder) {
		this.temporaryFolder = temporaryFolder;
	}

	public static Path delete(final Path path) {
		TryIO.with(() -> Files.deleteIfExists(path));
		return path;
	}

	public static Path createDirectory(final Path path, final String name) {
		return TryIO.with(() -> Files.createDirectory(path.resolve(name)));
	}

	public static Path createFile(final Path path, final String name) {
		return TryIO.with(() -> Files.createFile(path.resolve(name)));
	}

	public static Path createLink(final Path linkTarget, final String linkName, final Path linkPlace) {
		return TryIO.with(() -> Files.createSymbolicLink(linkPlace.resolve(linkName), linkTarget));
	}

	public static Set<PosixFilePermission> getPosixFilePermissions(final Path path) {
		return TryIO.with(() -> Files.getPosixFilePermissions(path));
	}

	public static Path setPosixFilePermissions(final Path path, final Set<PosixFilePermission> permissions) {
		return TryIO.with(() -> Files.setPosixFilePermissions(path, permissions));
	}

	static class TryIO {

		@FunctionalInterface
		interface ThrowingSupplier<T> {
			T apply() throws IOException;
		}

		static <T> T with(final ThrowingSupplier<T> throwingSupplier) {
			final Supplier<T> supplier = () -> {
				try {
					return throwingSupplier.apply();
				} catch (final IOException e) {
					throw new UncheckedIOException(e);
				}
			};
			return supplier.get();
		}
	}
}
