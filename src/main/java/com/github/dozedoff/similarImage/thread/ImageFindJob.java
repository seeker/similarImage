package com.github.dozedoff.similarImage.thread;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.dozedoff.commonj.filefilter.SimpleImageFilter;
import com.github.dozedoff.similarImage.db.Persistence;

/**
 * Scan the given directory and all sub-directories for image files and check
 * them against the database. Found files that have not been hashed will be
 * added as {@link ImageLoadJob}.
 * 
 * @author Nicholas Wright
 *
 */
public class ImageFindJob implements Runnable {
	private final Logger logger = LoggerFactory.getLogger(ImageFindJob.class);
	private final String searchPath;
	private final ExecutorService threadPool;
	private final Persistence persistence;

	public ImageFindJob(String searchPath, ExecutorService threadPool, Persistence persistence) {
		this.searchPath = searchPath;
		this.threadPool = threadPool;
		this.persistence = persistence;
	}

	@Override
	public void run() {
		logger.info("Scanning {} for images...", searchPath);
		LoadJobVisitor visitor = new LoadJobVisitor(new SimpleImageFilter(), threadPool, persistence);

		try {
			Files.walkFileTree(Paths.get(searchPath), visitor);
		} catch (IOException e) {
			logger.error("Failed to walk file tree", e);
		}

		logger.info("Finished scanning for images in {}", searchPath);
	}
}
