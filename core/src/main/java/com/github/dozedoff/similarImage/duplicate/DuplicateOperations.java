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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JDialog;
import javax.swing.JOptionPane;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.dozedoff.commonj.file.DirectoryVisitor;
import com.github.dozedoff.similarImage.db.FilterRecord;
import com.github.dozedoff.similarImage.db.ImageRecord;
import com.github.dozedoff.similarImage.db.Persistence;

public class DuplicateOperations {
	private static final Logger logger = LoggerFactory.getLogger(DuplicateOperations.class);
	private final Persistence persistence;

	public enum Tags {
		DNW, BLOCK
	}

	public DuplicateOperations(Persistence persistence) {
		this.persistence = persistence;
	}

	public void moveToDnw(Path path) {
		logger.info("Method not implemented");
		// TODO code me
	}

	public void deleteAll(Collection<ImageRecord> records) {
		for (ImageRecord ir : records) {
			Path path = Paths.get(ir.getPath());
			deleteFile(path);
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

			persistence.deleteRecord(ir);
			Files.delete(path);
		} catch (IOException e) {
			logger.warn("Failed to delete {} - {}", path, e.getMessage());
		} catch (SQLException e) {
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
	 */
	public void markAll(Collection<ImageRecord> records, String tag) {
		for (ImageRecord record : records) {
			try {
				persistence.addFilter(new FilterRecord(record.getpHash(), tag));
				logger.info("Adding pHash {} to filter, tag {}, source file {}", record.getpHash(), tag, record.getPath());
			} catch (SQLException e) {
				logger.warn("Failed to add tag for {}: {}", record.getPath(), e.toString());
			}
		}
	}

	public void markDnwAndDelete(Collection<ImageRecord> records) {
		for (ImageRecord ir : records) {
			long pHash = ir.getpHash();
			Path path = Paths.get(ir.getPath());

			FilterRecord fr = new FilterRecord(pHash, Tags.DNW.toString());
			try {
				persistence.addFilter(fr);
				deleteFile(path);
			} catch (SQLException e) {
				logger.warn("Failed to add filter entry for {} - {}", path, e.getMessage());
			}
		}
	}

	public void markAs(Path path, String reason) {
		// TODO do this with transaction
		try {
			ImageRecord ir = persistence.getRecord(path);
			if (ir == null) {
				logger.warn("No record found for {}", path);
				return;
			}

			long pHash = ir.getpHash();
			logger.info("Adding pHash {} to filter, reason {}", pHash, reason);
			FilterRecord fr = persistence.getFilter(pHash);

			if (fr != null) {
				fr.setReason(reason);
			} else {
				fr = new FilterRecord(pHash, reason);
			}

			persistence.addFilter(fr);
		} catch (SQLException e) {
			logger.warn("Add filter operation failed for {} - {}", path, e.getMessage());
		}
	}

	public void markDirectoryAs(Path directory, String reason) {
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
					markAs(current, reason);
					addCount++;
				}
			}

			logger.info("Added {} images from {} to filter list", addCount, directory);
		} catch (IOException e) {
			logger.error("Failed to add images to filter list, {}", e);
		}
	}

	public void markDirectoryAndChildrenAs(Path rootDirectory, String tag) {
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
			records = persistence.filterByPath(directory);
		} catch (SQLException e) {
			logger.error("Failed to get records from database: {}", e.toString());
		}

		LinkedList<ImageRecord> toPrune = new LinkedList<>();

		for (ImageRecord ir : records) {
			Path path = Paths.get(ir.getPath());

			if (!Files.exists(path)) {
				toPrune.add(ir);
			}
		}

		logger.info("Found {} non-existant records", toPrune.size());

		return toPrune;
	}

	/**
	 * Checks all known paths for the given directory and removes those that no longer exist.
	 * 
	 * @param directory
	 *            directory to scan for missing files
	 * @deprecated use call to get number to prune, then separate call to prune.
	 */
	@Deprecated
	public void pruneRecords(Path directory) {
		try {
			List<ImageRecord> toPrune = findMissingFiles(directory);

			logger.info("Found {} non-existant records", toPrune.size());

			Object options[] = { "Prune " + toPrune.size() + " records?" };
			JOptionPane pane = new JOptionPane(options, JOptionPane.PLAIN_MESSAGE, JOptionPane.OK_CANCEL_OPTION);
			JDialog dialog = pane.createDialog("Prune records");
			dialog.setVisible(true);

			if (pane.getValue() != null && (Integer) pane.getValue() == JOptionPane.OK_OPTION) {
				persistence.deleteRecord(toPrune);
			} else {
				logger.info("User aborted prune operation for {}", directory);
			}
		} catch (SQLException e) {
			logger.warn("Failed to prune records for {} - {}", directory, e.getMessage());
		}
	}

	public void ignore(long pHash) {
		try {
			persistence.addIgnore(pHash);
		} catch (SQLException e) {
			logger.warn("Failed to ignore pHash {} - {}", pHash, e.getMessage());
		}
	}

	/**
	 * Get a list of in-use filter tags
	 * 
	 * @return list of tags
	 */
	public List<String> getFilterTags() {
		return persistence.getFilterTags();
	}
}
