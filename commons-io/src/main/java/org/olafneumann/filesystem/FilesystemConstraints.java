package org.olafneumann.filesystem;

import static org.olafneumann.filesystem.FilesystemEventType.DELETED;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

/**
 * TODO javadoc
 *
 * @see <a href=
 *      "https://github.com/filesystem-watcher/filesystem-watcher">github</a>
 */
/**
 * @author micro
 *
 */
public class FilesystemConstraints implements Predicate<FilesystemEvent> {
	/** TODO javadoc */
	public enum FileType {
		/** TODO javadoc */
		REGULAR(BasicFileAttributes::isRegularFile),
		/** TODO javadoc */
		DIRECTORY(BasicFileAttributes::isDirectory),
		/** TODO javadoc */
		LINK(BasicFileAttributes::isSymbolicLink),
		/** TODO javadoc */
		OTHER(BasicFileAttributes::isOther);

		private final Predicate<BasicFileAttributes> predicate;

		private FileType(final Predicate<BasicFileAttributes> predicate) {
			this.predicate = predicate;
		}
	}

	/** TODO javadoc */
	public static final FilesystemConstraints DEFAULT = new FilesystemConstraints(
			Collections.emptyList(),
			Collections.emptyList(),
			Collections.emptyList(),
			false);

	private List<String> filenameSubstrings;

	private List<Pattern> filenamePatterns;

	private List<FileType> fileTypes;

	private boolean isRecursive;

	/**
	 * TODO javadoc
	 *
	 * @param filenameSubstrings
	 * @param filenamePatterns
	 * @param fileTypes
	 * @param isRecursive
	 */
	public FilesystemConstraints(final List<String> filenameSubstrings,
			final List<Pattern> filenamePatterns,
			final List<FileType> fileTypes,
			final boolean isRecursive) {
		this.filenameSubstrings = filenameSubstrings;
		this.filenamePatterns = filenamePatterns;
		this.fileTypes = fileTypes;
		this.isRecursive = isRecursive;
	}

	/** {@inheritDoc} */
	@Override
	public boolean test(final @Nullable FilesystemEvent event) {
		Objects.requireNonNull(event);
		final Path path = event.getPath();
		if (!fileTypes.isEmpty() && event.getEventType() != DELETED) {
			final BasicFileAttributes fileAttributes = readAttributes(path);
			if (fileTypes.stream().noneMatch(fileType -> fileType.predicate.test(fileAttributes))) {
				return false;
			}
		}

		final String filename = path.getFileName().toString();
		if (!filenameSubstrings.isEmpty()) {
			if (filenameSubstrings.stream().noneMatch(filename::contains)) {
				return false;
			}
		}

		if (!filenamePatterns.isEmpty()) {
			if (filenamePatterns.stream().noneMatch(pattern -> pattern.matcher(filename).matches())) {
				return false;
			}
		}
		return true;
	}

	private BasicFileAttributes readAttributes(final Path path) {
		try {
			return Files.readAttributes(path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
		} catch (final IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	/**
	 * @return the isRecursive
	 */
	public boolean isRecursive() {
		return isRecursive;
	}

	/**
	 * TODO javaodoc
	 * 
	 * @param filenamePatterns
	 * @return
	 */
	FilesystemConstraints withFilenamePatterns(final List<Pattern> filenamePatterns) {
		this.filenamePatterns = filenamePatterns;
		return this;
	}

	/**
	 * TODO javaodoc
	 * 
	 * @param filenameSubstrings
	 * @return
	 */
	FilesystemConstraints withFilenameSubstrings(final List<String> filenameSubstrings) {
		this.filenameSubstrings = filenameSubstrings;
		return this;
	}

	/**
	 * TODO javaodoc
	 * 
	 * @param fileTypes
	 * @return
	 */
	FilesystemConstraints withFileTypes(final List<FileType> fileTypes) {
		this.fileTypes = fileTypes;
		return this;
	}

	/**
	 * TODO javaodoc
	 * 
	 * @param isRecursive
	 * @return
	 */
	FilesystemConstraints withRecursive(final boolean isRecursive) {
		this.isRecursive = isRecursive;
		return this;
	}
}
