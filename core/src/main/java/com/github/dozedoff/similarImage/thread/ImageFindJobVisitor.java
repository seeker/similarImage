package com.github.dozedoff.similarImage.thread;

import java.io.IOException;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.dozedoff.similarImage.handler.HashHandler;
import com.github.dozedoff.similarImage.io.Statistics;

/**
 * For every file that is found, check the file extension. A valid file is passed to the handlers for processing.
 * 
 * @author Nicholas Wright
 *
 */
public class ImageFindJobVisitor extends SimpleFileVisitor<Path> {
	private static final Logger LOGGER = LoggerFactory.getLogger(ImageFindJobVisitor.class);
	private final Filter<Path> fileFilter;
	private final Statistics statistics;
	private int fileCount;
	private Collection<HashHandler> handlers;

	/**
	 * Creates a visitor that will pass the accepted files to the handlers.
	 * 
	 * @param fileFilter
	 *            filter which specifies the accepted files
	 * @param handlers
	 *            for processing the files
	 * @param statistics
	 *            for tracking stats about files
	 */
	public ImageFindJobVisitor(Filter<Path> fileFilter, Collection<HashHandler> handlers, Statistics statistics) {
		this.fileFilter = fileFilter;
		this.statistics = statistics;
		this.handlers = handlers;
	}


	/**
	 * Visit a file and if it is accepted, pass it to the handlers.
	 * 
	 * @param {@inheritDoc}
	 * @return {@inheritDoc}
	 * @throws {@inheritDoc}
	 */
	@Override
	public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
		if (isAcceptedFile(file)) {
			statistics.incrementFoundFiles();
			fileCount++;

			boolean isHandled = false;

			for (HashHandler handler : handlers) {
				if (handler.handle(file)) {
					isHandled = true;
					break;
				}
			}

			statistics.incrementProcessedFiles();

			if (!isHandled) {
				statistics.incrementFailedFiles();
				LOGGER.error("No handler was able to process {}", file);
			}
		}

		return FileVisitResult.CONTINUE;
	}

	private boolean isAcceptedFile(Path file) throws IOException {
		return fileFilter.accept(file);
	}

	/**
	 * 
	 * @deprecated Use {@link Statistics#getFoundFiles()} instead.
	 */
	@Deprecated
	public int getFileCount() {
		return fileCount;
	}
}
