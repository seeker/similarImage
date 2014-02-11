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
import com.github.dozedoff.similarImage.gui.SimilarImageView;
import com.github.dozedoff.similarImage.hash.PhashWorker;
import com.github.dozedoff.similarImage.io.ImageProducer;

public class ImageIndexer extends Thread {
	private final Logger logger = LoggerFactory.getLogger(ImageIndexer.class);

	private String path;
	private SimilarImageView gui;
	private ImageProducer producer;
	private PhashWorker phw;

	public ImageIndexer(String path, SimilarImageView gui, ImageProducer producer, PhashWorker phw) {
		super("ImageIndexer");
		this.path = path;
		this.gui = gui;
		this.producer = producer;
		this.phw = phw;
	}

	@Override
	public void run() {
		gui.setStatus("Running...");
		logger.info("Hashing images in {}", path);
		LinkedList<Path> imagePaths = new LinkedList<Path>();

		gui.setStatus("Looking for images...");
		findImages(path, imagePaths);
		gui.setStatus("Hashing images...");
		calculateHashes(imagePaths);
		gui.setStatus("Done");
	}

	public void killAll() {
		producer.clear();
		phw.clear();

		logger.info("Cleared all queues");
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

		logger.info("Adding paths to ImageProducer");
		producer.addToLoad(imagePaths);

		try {
			Thread.sleep(1000);
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		sw.stop();
		logger.info("Took {} to process {} images", sw.getTime(), imagePaths.size());
	}
}
