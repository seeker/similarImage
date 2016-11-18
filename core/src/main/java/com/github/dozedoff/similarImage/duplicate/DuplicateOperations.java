/*  Copyright (C) 2016  Nicholas Wright
    
    This file is part of similarImage - A similar image finder using pHash
    
    similarImage is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.github.dozedoff.similarImage.duplicate;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.imageio.ImageIO;

import org.imgscalr.Scalr;
import org.imgscalr.Scalr.Method;
import org.imgscalr.Scalr.Mode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.dozedoff.commonj.file.DirectoryVisitor;
import com.github.dozedoff.similarImage.db.FilterRecord;
import com.github.dozedoff.similarImage.db.ImageRecord;
import com.github.dozedoff.similarImage.db.Tag;
import com.github.dozedoff.similarImage.db.Thumbnail;
import com.github.dozedoff.similarImage.db.repository.FilterRepository;
import com.github.dozedoff.similarImage.db.repository.ImageRepository;
import com.github.dozedoff.similarImage.db.repository.RepositoryException;
import com.github.dozedoff.similarImage.db.repository.TagRepository;
import com.github.dozedoff.similarImage.util.ImageUtil;

public class DuplicateOperations {
	private static final Logger logger = LoggerFactory.getLogger(DuplicateOperations.class);

	private static final String FILTER_ADD_FAILED_MESSAGE = "Add filter operation failed for {} - {}";
	private static final int THUMBNAIL_SIZE = Thumbnail.THUMBNAIL_SIZE;
	private static final String MESSAGE_DIGEST_ALGORITHM = "SHA-256";
	
	private final FilterRepository filterRepository;
	private final TagRepository tagRepository;
	private final ImageRepository imageRepository;
	private final FileSystem fileSystem;

	public enum Tags {
		DNW, BLOCK
	}

	/**
	 * Create with the given classes to access the data. Use the default {@link FileSystem}.
	 * 
	 * @param filterRepository
	 *            Data access for {@link FilterRecord}
	 * @param tagRepository
	 *            Data access for {@link Tag}
	 * @param imageRepository
	 *            Data access for {@link ImageRecord}
	 * 
	 */
	public DuplicateOperations(FilterRepository filterRepository, TagRepository tagRepository, ImageRepository imageRepository) {
		this(FileSystems.getDefault(), filterRepository, tagRepository, imageRepository);
	}

	/**
	 * Create with the given classes to access the data.
	 * 
	 * @param fileSystem
	 *            to use for accessing files
	 * @param filterRepository
	 *            Data access for {@link FilterRecord}
	 * @param tagRepository
	 *            Data access for {@link Tag}
	 * @param imageRepository
	 *            Data access for {@link ImageRecord}
	 * 
	 */
	public DuplicateOperations(FileSystem fileSystem, FilterRepository filterRepository, TagRepository tagRepository,
			ImageRepository imageRepository) {
		this.filterRepository = filterRepository;
		this.tagRepository = tagRepository;
		this.imageRepository = imageRepository;
		this.fileSystem = fileSystem;
	}

	public void moveToDnw(Path path) {
		logger.info("Method not implemented");
		// TODO code me
	}

	public void deleteAll(Collection<ImageRecord> records) {
		for (ImageRecord ir : records) {
			Path path = fileSystem.getPath(ir.getPath());
			deleteFile(path);
		}
	}

	/**
	 * Removed the images from the database.
	 * 
	 * @param records
	 *            to remove
	 */
	public void remove(Collection<ImageRecord> records) {
		try {
			imageRepository.remove(records);
		} catch (RepositoryException e) {
			logger.error("Failed to remove images: {}", e.toString());
		}
	}

	// TODO directly delete via Imagerecord
	public void deleteFile(Path path) {
		try {
			if (path == null) {
				logger.error("Path was null, skipping...");
				return;
			}

			if (isDirectory(path)) {
				logger.info("Path is a directory, skipping...");
				return;
			}

			logger.info("Deleting file {}", path);

			ImageRecord ir = new ImageRecord(path.toString(), 0);

			imageRepository.remove(ir);
			Files.delete(path);
		} catch (IOException e) {
			logger.warn("Failed to delete {} - {}", path, e.getMessage());
		} catch (RepositoryException e) {
			logger.warn("Failed to remove {} from database - {}", path, e.getMessage());
		}
	}

	/**
	 * Add filter records with the given tag for all records.
	 * 
	 * @param records
	 *            to add filter records for
	 * @param tag
	 *            tag to use for filter records
	 * @deprecated Use {@link DuplicateOperations#markAll(Collection, Tag)} instead.
	 */
	@Deprecated
	public void markAll(Collection<ImageRecord> records, String tag) {
		for (ImageRecord record : records) {
			try {
				markAs(record, getTag(tag));
				logger.info("Adding pHash {} to filter, tag {}, source file {}", record.getpHash(), tag, record.getPath());
			} catch (RepositoryException e) {
				logger.warn("Failed to add tag for {}: {}", record.getPath(), e.toString());
			}
		}
	}

	/**
	 * Add filter records with the given tag for all records.
	 * 
	 * @param records
	 *            to add filter records for
	 * @param tag
	 *            tag to use for filter records
	 */
	public void markAll(Collection<ImageRecord> records, Tag tag) {
		for (ImageRecord record : records) {
			try {
				markAs(record, tag);
				logger.info("Adding pHash {} to filter, tag {}, source file {}", record.getpHash(), tag,
						record.getPath());
			} catch (RepositoryException e) {
				logger.warn("Failed to add tag for {}: {}", record.getPath(), e.toString());
			}
		}
	}

	/**
	 * Add {@link FilterRecord} for the images and delete the files.
	 * 
	 * @param records
	 *            to filter and delete
	 */
	public void markDnwAndDelete(Collection<ImageRecord> records) {
		for (ImageRecord ir : records) {
			Path path = fileSystem.getPath(ir.getPath());

			try {
				markAs(ir, getTag(Tags.DNW.toString()));
				deleteFile(path);
			} catch (RepositoryException e) {
				logger.warn("Failed to add filter entry for {} - {}", path, e.getMessage());
			}
		}
	}

	/**
	 * Add a {@link FilterRecord} for the given path.
	 * 
	 * @param path
	 *            to tag
	 * @param reason
	 *            reason/tag to use
	 * @deprecated Do not use plain Strings for tags, use the {@link Tag} class instead
	 */
	@Deprecated
	public void markAs(Path path, String reason) {
		try {
			ImageRecord ir = imageRepository.getByPath(path);

			if (ir == null) {
				logger.warn("No record found for {}", path);
				return;
			}

			Tag tag = getTag(reason);

			markAs(ir, tag);
		} catch (RepositoryException e) {
			logger.warn(FILTER_ADD_FAILED_MESSAGE, path, e.getMessage());
		}
	}

	/**
	 * Add a {@link FilterRecord} for the given path.
	 * 
	 * @param path
	 *            to tag
	 * @param tag
	 *            tag to use
	 */
	public void markAs(Path path, Tag tag) {
		try {
			ImageRecord ir = imageRepository.getByPath(path);

			if (ir == null) {
				logger.warn("No record found for {}", path);
				return;
			}

			markAs(ir, tag);
		} catch (RepositoryException e) {
			logger.warn(FILTER_ADD_FAILED_MESSAGE, path, e.getMessage());
		}
	}

	/**
	 * @deprecated Do not use String tags, use the {@link Tag} class instead
	 */
	@Deprecated
	private Tag getTag(String reason) throws RepositoryException {
		Tag tag = tagRepository.getByName(reason);

		if (tag == null) {
			logger.info("Tag {} does not exist, creating...", reason);
			tag = new Tag(reason);
			tagRepository.store(tag);
		}

		return tag;
	}

	/**
	 * Add a {@link FilterRecord} for the given {@link ImageRecord} with the specified reason.
	 * 
	 * @param image
	 *            to add a filter for
	 * @param tag
	 *            for the filter
	 * @throws RepositoryException
	 *             if there is an error accessing the datasource
	 */
	public void markAs(ImageRecord image, Tag tag) throws RepositoryException {
			long pHash = image.getpHash();
			logger.info("Adding pHash {} to filter, reason {}", pHash, tag);

		Thumbnail thumb = createThumbnail(fileSystem.getPath(image.getPath()));

			filterRepository.store(new FilterRecord(pHash, tag, thumb));
	}

	private Thumbnail createThumbnail(Path path) {
		Thumbnail thumb = null;

		try {
			if (Files.size(path) == 0) {
				logger.warn("Image is empty, skipping thumbnail generation");
				return thumb;
			}

			InputStream is = Files.newInputStream(path);
			MessageDigest md = MessageDigest.getInstance(MESSAGE_DIGEST_ALGORITHM);
			DigestInputStream dis = new DigestInputStream(is, md);
			
			BufferedImage img = ImageIO.read(dis);
			BufferedImage resized = Scalr.resize(img, Method.QUALITY, Mode.AUTOMATIC, THUMBNAIL_SIZE);

			thumb = new Thumbnail(md.digest(), ImageUtil.imageToBytes(resized));
		} catch (IOException | NoSuchAlgorithmException e) {
			logger.warn("Failed to load thumbnail image for {}, {}", path, e.toString());
		}

		return thumb;
	}

	/**
	 * Create {@link FilterRecord} with the given tag for all files in the directory. Sub-Directories will <b>not</b> be
	 * searched.
	 * 
	 * @param directory
	 *            to search for files
	 * @param reason
	 *            tag to use for the {@link FilterRecord}
	 * @deprecated Do not use string tags, use {@link Tag} instead.
	 */
	@Deprecated
	public void markDirectoryAs(Path directory, String reason) {
		try {
			markDirectoryAs(directory, getTag(reason));
		} catch (RepositoryException e) {
			logger.error("Failed to add images to filter list, {}", e);
		}
	}

	/**
	 * Create {@link FilterRecord} with the given {@link Tag} for all files in the directory. Sub-Directories will
	 * <b>not</b> be searched.
	 * 
	 * @param directory
	 *            to search for files
	 * @param tag
	 *            tag to use for the {@link FilterRecord}
	 */
	public void markDirectoryAs(Path directory, Tag tag) {
		if (!isDirectory(directory)) {
			logger.warn("Directory {} not valid, aborting.", directory);
			return;
		}

		try {
			int addCount = 0;
			Iterator<Path> iter = Files.newDirectoryStream(directory).iterator();

			while (iter.hasNext()) {
				Path current = iter.next();
				if (Files.isRegularFile(current)) {
					markAs(current, tag);
					addCount++;
				}
			}

			logger.info("Added {} images from {} to filter list", addCount, directory);
		} catch (IOException e) {
			logger.error("Failed to add images to filter list, {}", e);
		}
	}

	/**
	 * Create {@link FilterRecord} with the given {@link Tag} for all files in the directory and Sub-Directories.
	 * 
	 * @param rootDirectory
	 *            to search for files and folders
	 * @param tag
	 *            tag to use for the {@link FilterRecord}
	 * @deprecated Do not use string tags, use {@link Tag} instead.
	 */
	@Deprecated
	public void markDirectoryAndChildrenAs(Path rootDirectory, String tag) {
		try {
			markDirectoryAndChildrenAs(rootDirectory, getTag(tag));
		} catch (RepositoryException e) {
			logger.error("Failed to mark directory {}", rootDirectory);
		}
	}

	/**
	 * Create {@link FilterRecord} with the given {@link Tag} for all files in the directory and Sub-Directories.
	 * 
	 * @param rootDirectory
	 *            to search for files and folders
	 * @param tag
	 *            tag to use for the {@link FilterRecord}
	 */
	public void markDirectoryAndChildrenAs(Path rootDirectory, Tag tag) {
		LinkedList<Path> directories = new LinkedList<>();
		DirectoryVisitor dv = new DirectoryVisitor(directories);

		try {
			Files.walkFileTree(rootDirectory, dv);
		} catch (IOException e) {
			logger.error("Failed to walk directory {}: {}", rootDirectory, e.toString());
		}

		for (Path dir : directories) {
			markDirectoryAs(dir, tag);
		}
	}

	private boolean isDirectory(Path directory) {
		return directory != null && Files.exists(directory) && Files.isDirectory(directory);
	}

	/**
	 * Returns a list of {@link ImageRecord} that are in the database, but the corresponding file does not exist. <b>Caution:</b> may return
	 * false positives if a source is offline (like a network share or external disk)
	 * 
	 * @param directory
	 *            to scan for missing files
	 * @return a list f missing files
	 */
	public List<ImageRecord> findMissingFiles(Path directory) {
		if (!isDirectory(directory)) {
			logger.error("Directory is null or missing, aborting");
			return Collections.emptyList();
		}

		if (!Files.exists(directory)) {
			logger.warn("Directory {} does not exist.", directory);
		}

		List<ImageRecord> records = Collections.emptyList();

		try {
			records = imageRepository.startsWithPath(directory);
		} catch (RepositoryException e) {
			logger.error("Failed to get records from database: {}", e.toString());
		}

		LinkedList<ImageRecord> toPrune = new LinkedList<>();

		for (ImageRecord ir : records) {
			Path path = fileSystem.getPath(ir.getPath());

			if (!Files.exists(path)) {
				toPrune.add(ir);
			}
		}

		logger.info("Found {} non-existant records", toPrune.size());

		return toPrune;
	}

	public void ignore(long pHash) {
			throw new RuntimeException("Not implemented");
	}

	/**
	 * Get a list of in-use filter tags
	 * 
	 * @return list of tags
	 */
	public List<Tag> getFilterTags() {
		try {
			return tagRepository.getAll();
		} catch (RepositoryException e) {
			logger.error("Failed to load tags: {}", e.toString());
			return Collections.emptyList();
		}
	}
}
