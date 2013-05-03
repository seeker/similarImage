/*  Copyright (C) 2013  Nicholas Wright
    
    This file is part of similarImage - A similar image finder using pHash
    
    mmut is free software: you can redistribute it and/or modify
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
package com.github.dozedoff.similarImage.app;

import java.awt.Dimension;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;

import javax.swing.JComponent;
import javax.swing.JLabel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sun.awt.image.ImageFormatException;

import com.github.dozedoff.commonj.file.FilenameFilterVisitor;
import com.github.dozedoff.commonj.filefilter.SimpleImageFilter;
import com.github.dozedoff.commonj.image.SubsamplingImageLoader;
import com.github.dozedoff.similarImage.db.ImageRecord;
import com.github.dozedoff.similarImage.db.Persistence;
import com.github.dozedoff.similarImage.duplicate.SortSimilar;
import com.github.dozedoff.similarImage.gui.DisplayGroup;
import com.github.dozedoff.similarImage.gui.IGUIevent;
import com.github.dozedoff.similarImage.gui.SimilarImageGUI;
import com.github.dozedoff.similarImage.hash.PhashWorker;
import com.j256.ormlite.dao.CloseableWrappedIterable;

@SuppressWarnings("restriction")
public class SimilarImage implements IGUIevent{
	SimilarImageGUI gui;
	DisplayGroup displayGroup;
	
	Logger logger = LoggerFactory.getLogger(SimilarImage.class);
	private final int WORKER_TREADS = 4;
	private final int THUMBNAIL_DIMENSION = 500;
	private PhashWorker workers[] = new PhashWorker[WORKER_TREADS];
	private SortSimilar sorter = new SortSimilar();
	
	public static void main(String[] args) {
		new SimilarImage().init();
	}
	
	public void init() {
		gui = new SimilarImageGUI(this);
		displayGroup = new DisplayGroup();
	}
	
	public void indexImages(String path) {
		Thread t = new ImageIndexer(path);
		t.start();
	}
	
	public void sortDuplicates(int hammingDistance) {
		Thread t = new ImageSorter(hammingDistance);
		t.start();
	}
	
	private void findImages(String path, LinkedBlockingQueue<Path> imagePaths) {
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
	
	private void calculateHashes(LinkedBlockingQueue<Path> imagePaths) {
		logger.info("Creating and starting workers...");
		for(int i=0; i < WORKER_TREADS; i++) {
			workers[i] = new PhashWorker(imagePaths, this);
			workers[i].start();
		}
		
		for(int i=0; i < WORKER_TREADS; i++) {
			try {
				workers[i].join();
			} catch (InterruptedException e) {
				logger.info("Interrupted waiting for {}", workers[i].getName());
			}
		}
	}
	
	public void stopWorkers() {
		logger.info("Stopping all workers...");
		for(PhashWorker phw : workers) {
			if(phw != null) {
				logger.info("Stopping {}...", phw.getName());
				phw.stopWorker();
			}
		}
		
		gui.clearProgress();
	}
	
	public void displayGroup(long group) {
		Set<ImageRecord> grouplist = sorter.getGroup(group);
		HashMap<Path, JComponent> images = new HashMap<Path, JComponent>();
		Dimension imageDim = new Dimension(THUMBNAIL_DIMENSION, THUMBNAIL_DIMENSION);
		
		logger.info("Loading {} thumbnails for group {}", grouplist.size(), group);
		
		for(ImageRecord rec : grouplist) {
			Path path = Paths.get(rec.getPath());
			try {
				JLabel image = SubsamplingImageLoader.loadAsLabel(path, imageDim);
				images.put(path,image);
			} catch (ImageFormatException e) {
				logger.warn("Unable to process image {}", path, e);
			} catch (IOException e) {
				logger.warn("Unable to load file {}", path);
			}
		}
		
		displayGroup.displayImages(group, images);
	}
	
	class ImageIndexer extends Thread {
		String path;
		
		public ImageIndexer(String path) {
			this.path = path;
		}
		
		@Override
		public void run() {
			gui.clearProgress();
			gui.setStatus("Running...");
			logger.info("Hashing images in {}", path);
			LinkedBlockingQueue<Path> imagePaths = new LinkedBlockingQueue<Path>();
			
			gui.setStatus("Looking for images...");
			findImages(path, imagePaths);
			gui.setStatus("Hashing images...");
			calculateHashes(imagePaths);
			gui.setStatus("Done");
		}
	}
	
	class ImageSorter extends Thread {
		int hammingDistance = 0;
		
		public ImageSorter(int hammingDistance) {
			super();
			this.hammingDistance = hammingDistance;
		}

		@Override
		public void run() {
			sorter.clear();
			gui.setStatus("Sorting...");
			if (hammingDistance == 0) {
				CloseableWrappedIterable<ImageRecord> records = Persistence.getInstance().getImageRecordIterator();
				sorter.sortExactMatch(records);
			} else {
				sorter.sortHammingDistance(hammingDistance);
			}
			gui.setStatus("" + sorter.getNumberOfDuplicateGroups() + " Groups");
			List<Long> groups = sorter.getDuplicateGroups();
			gui.populateGroupList(groups);
		}
	}

	@Override
	public void progressUpdate(int update) {
		gui.addDelta(update);
	}
}
