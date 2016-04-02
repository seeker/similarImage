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

import com.github.dozedoff.similarImage.db.Persistence;

/**
 * For every file that is found, check the file extension. A valid file is
 * compared against the database to see if it needs to be hashed, if so, create
 * a {@link ImageLoadJob}.
 * 
 * @author Nicholas Wright
 *
 */
public class LoadJobVisitor extends SimpleFileVisitor<Path> {
	private static final Logger logger = LoggerFactory.getLogger(LoadJobVisitor.class);
	private final ExecutorService threadPool;
	private final Filter<Path> fileFilter;
	private final Persistence persistence;

	public LoadJobVisitor(Filter<Path> fileFilter, ExecutorService threadPool, Persistence persistence) {
		this.threadPool = threadPool;
		this.persistence = persistence;
		this.fileFilter = fileFilter;
	}

	@Override
	public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
		try {
			if (isAcceptedFile(file) && !isInDatabase(file)) {
				threadPool.execute(new ImageLoadJob(file));
			}
		} catch (SQLException e) {
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
}
