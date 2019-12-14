package org.olafneumann.filesystem.parameters;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import javax.annotation.Nullable;

import org.olafneumann.filesystem.utilities.FilesystemUtils;

@SuppressWarnings("javadoc")
public enum PathKind implements Function<Path, PathKind.PathScenario> {
	DIRECTORY(path -> {
		final Path directory = FilesystemUtils.createDirectory(path, "directory");
		return PathScenario.of(directory);
	}),
	FILE(path -> {
		final Path file = FilesystemUtils.createFile(path, "test.txt");
		return PathScenario.of(file);
	}),
	FILE_SYMLINK(path -> {
		final Path filePath = FILE.apply(path).getSubjectPath();
		final Path linkPath = FilesystemUtils.createLink(filePath, "symlink", path);
		return PathScenario.of(linkPath, filePath, Arrays.asList(linkPath, filePath));
	}),
	DIRECTORY_SYMLINK(path -> {
		final Path directoryPath = DIRECTORY.apply(path).getSubjectPath();
		final Path linkPath = FilesystemUtils.createLink(directoryPath, "symlink", path);
		return PathScenario.of(linkPath, directoryPath, Arrays.asList(linkPath, directoryPath));
	}),
	RECURSIVE_DIRECTORY(path -> {
		final Path first = FilesystemUtils.createDirectory(path, "first");
		final Path second = FilesystemUtils.createDirectory(first, "second");
		final Path third = FilesystemUtils.createDirectory(second, "third");
		return PathScenario.of(third, Arrays.asList(first, second, third));
	}),
	RECURSIVE_FILE(path -> {
		final Path first = FilesystemUtils.createDirectory(path, "first");
		final Path second = FilesystemUtils.createDirectory(first, "second");
		final Path third = FilesystemUtils.createFile(second, "recursive.txt");
		return PathScenario.of(third, Arrays.asList(first, second, third));
	});

	private PathKind(final Function<Path, PathScenario> allPathsSupplier) {
		this.allPathsSupplier = allPathsSupplier;
	}

	private final Function<Path, PathScenario> allPathsSupplier;

	@Override
	public PathScenario apply(final @Nullable Path path) {
		return allPathsSupplier.apply(path);
	}

	public static class PathScenario {
		static PathScenario of(final Path tested) {
			return of(tested, Arrays.asList(tested));
		}

		static PathScenario of(final Path tested, final List<Path> all) {
			return of(tested, tested, all);
		}

		static PathScenario of(final Path tested, final Path subject, final List<Path> all) {
			return new PathScenario(tested, subject, all);
		}

		private final Path testedPath;

		private final Path subjectPath;

		private final List<Path> allPaths;

		private PathScenario(final Path testedPath, final Path subjectPath, final List<Path> allPaths) {
			this.testedPath = testedPath;
			this.subjectPath = subjectPath;
			this.allPaths = allPaths;
		}

		public Path getTestedPath() {
			return testedPath;
		}

		public Path getSubjectPath() {
			return subjectPath;
		}

		public List<Path> getAllPaths() {
			return allPaths;
		}
	}
}
