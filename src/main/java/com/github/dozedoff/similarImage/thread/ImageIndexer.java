/*  Copyright (C) 2014  Nicholas Wright
    
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
package com.github.dozedoff.similarImage.thread;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.dozedoff.commonj.file.FilenameFilterVisitor;
import com.github.dozedoff.commonj.filefilter.SimpleImageFilter;
import com.github.dozedoff.commonj.time.StopWatch;
import com.github.dozedoff.similarImage.db.DBWriter;
import com.github.dozedoff.similarImage.gui.SimilarImageGUI;
import com.github.dozedoff.similarImage.hash.PhashWorker;
import com.github.dozedoff.similarImage.io.ImageProducer;

public class ImageIndexer extends Thread {
	private final Logger logger = LoggerFactory.getLogger(ImageIndexer.class);

	private final int WORKER_THREADS = 6;

	private String path;
	private SimilarImageGUI gui;
	private ImageProducer producer;
	private PhashWorker workers[] = new PhashWorker[WORKER_THREADS];
	private DBWriter dbWriter;

	public ImageIndexer(String path, SimilarImageGUI gui, ImageProducer producer, DBWriter dbWriter) {
		super("ImageIndexer");
		this.path = path;
		this.gui = gui;
		this.producer = producer;
		this.dbWriter = dbWriter;
	}

	@Override
	public void run() {
		producer.clear();
		gui.setStatus("Running...");
		logger.info("Hashing images in {}", path);
		LinkedList<Path> imagePaths = new LinkedList<Path>();

		gui.setStatus("Looking for images...");
		findImages(path, imagePaths);
		gui.setStatus("Hashing images...");
		calculateHashes(imagePaths);
		gui.setStatus("Done");
	}

	// TODO replace this with interrupt
	public void killAll() {
		for (PhashWorker phw : workers) {
			if (phw != null) {
				logger.info("Stopping {}...", phw.getName());
				phw.stopWorker();
			}
		}
	}

	private void findImages(String path, LinkedList<Path> imagePaths) {
		FilenameFilterVisitor visitor = new FilenameFilterVisitor(imagePaths, new SimpleImageFilter());
		Path directoryToSearch = Paths.get(path);
		try {
			Files.walkFileTree(directoryToSearch, visitor);
		} catch (IOException e) {
			logger.error("Failed to walk file tree", e);
			return;
		}

		logger.info("Found {} images", imagePaths.size());
		gui.setTotalFiles(imagePaths.size());
	}

	private void calculateHashes(List<Path> imagePaths) {
		StopWatch sw = new StopWatch();

		sw.start();
		logger.info("Creating and starting workers...");
		for (int i = 0; i < WORKER_THREADS; i++) {
			workers[i] = new PhashWorker(producer, dbWriter);
			workers[i].start();
		}

		logger.info("Adding paths to ImageProducer");
		producer.addToLoad(imagePaths);

		for (int i = 0; i < WORKER_THREADS; i++) {
			try {
				workers[i].join();
			} catch (InterruptedException e) {
				logger.info("Interrupted waiting for {}", workers[i].getName());
			}
		}

		sw.stop();
		logger.info("Took {} to process {} images", sw.getTime(), imagePaths.size());
	}
}
