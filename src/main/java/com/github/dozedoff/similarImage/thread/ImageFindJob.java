package com.github.dozedoff.similarImage.thread;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Scan the given directory and all sub-directories for image files and check
 * them against the database. Found files that have not been hashed will be
 * added as {@link ImageHashJob}.
 * 
 * @author Nicholas Wright
 *
 */
public class ImageFindJob implements Runnable {
	private final Logger logger = LoggerFactory.getLogger(ImageFindJob.class);
	private final String searchPath;
	private final SimpleFileVisitor<Path> visitor;

	public ImageFindJob(String searchPath, SimpleFileVisitor<Path> visitor) {
		this.searchPath = searchPath;
		this.visitor = visitor;
	}

	@Override
	public void run() {
		logger.info("Scanning {} for images...", searchPath);

		try {
			Files.walkFileTree(Paths.get(searchPath), visitor);
		} catch (IOException e) {
			logger.error("Failed to walk file tree", e);
		}

		logger.info("Finished scanning for images in {}", searchPath);
	}
}
