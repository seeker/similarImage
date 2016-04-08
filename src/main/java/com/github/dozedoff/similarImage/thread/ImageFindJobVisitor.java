package com.github.dozedoff.similarImage.thread;

import java.io.IOException;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.sql.SQLException;
import java.util.concurrent.ExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.dozedoff.commonj.hash.ImagePHash;
import com.github.dozedoff.similarImage.db.Persistence;
import com.github.dozedoff.similarImage.io.Statistics;

/**
 * For every file that is found, check the file extension. A valid file is
 * compared against the database to see if it needs to be hashed, if so, create
 * a {@link ImageHashJob} and add it to the queue.
 * 
 * @author Nicholas Wright
 *
 */
public class ImageFindJobVisitor extends SimpleFileVisitor<Path> {
	private static final Logger logger = LoggerFactory.getLogger(ImageFindJobVisitor.class);
	private final ExecutorService threadPool;
	private final Filter<Path> fileFilter;
	private final Persistence persistence;
	private final ImagePHash hasher;
	private final Statistics statistics;
	private int fileCount = 0;

	public ImageFindJobVisitor(Filter<Path> fileFilter, ExecutorService threadPool, Persistence persistence,
			ImagePHash hasher, Statistics statistics) {
		this.threadPool = threadPool;
		this.persistence = persistence;
		this.fileFilter = fileFilter;
		this.hasher = hasher;
		this.statistics = statistics;
	}

	@Override
	public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
		try {
			if (isAcceptedFile(file)) {
				statistics.incrementFoundFiles();
				fileCount++;

				if (!isInDatabase(file)) {
					threadPool.execute(new ImageHashJob(file, hasher, persistence, statistics));
				} else {
					statistics.incrementSkippedFiles();
					statistics.incrementProcessedFiles();
				}
			}
		} catch (SQLException e) {
			statistics.incrementProcessedFiles();
			statistics.incrementFailedFiles();
			logger.error("Database query for {} failed with: {}", file, e);
		}

		return FileVisitResult.CONTINUE;
	}

	private boolean isAcceptedFile(Path file) throws IOException {
		return fileFilter.accept(file);
	}

	private boolean isInDatabase(Path path) throws SQLException {
		return (persistence.isBadFile(path) || persistence.isPathRecorded(path));
	}

	public int getFileCount() {
		return fileCount;
	}
}
