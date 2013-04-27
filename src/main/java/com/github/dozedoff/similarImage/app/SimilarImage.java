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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.LinkedBlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.dozedoff.commonj.file.FilenameFilterVisitor;
import com.github.dozedoff.commonj.filefilter.SimpleImageFilter;
import com.github.dozedoff.similarImage.gui.SimilarImageGUI;
import com.github.dozedoff.similarImage.hash.PhashWorker;

public class SimilarImage {
	SimilarImageGUI gui;
	Logger logger = LoggerFactory.getLogger(SimilarImage.class);
	private final int WORKER_TREADS = 4;
	
	public static void main(String[] args) {
		new SimilarImage().init();
	}
	
	public void init() {
		gui = new SimilarImageGUI(this);
	}
	
	public void compareImages(String path) {
		logger.info("Comparing images in {}", path);
		LinkedBlockingQueue<Path> imagePaths = new LinkedBlockingQueue<Path>();
		
		FilenameFilterVisitor visitor = new FilenameFilterVisitor(imagePaths, new SimpleImageFilter());
		Path directoryToSearch = Paths.get(path);
		try {
			Files.walkFileTree(directoryToSearch, visitor);
		} catch (IOException e) {
			logger.error("Failed to walk file tree", e);
			return;
		}
		
		logger.info("Found {} images", imagePaths.size());
		calculateHashes(imagePaths);
	}
	
	private void calculateHashes(LinkedBlockingQueue<Path> imagePaths) {
		Thread workers[] = new Thread[WORKER_TREADS];
		for(Thread t : workers) {
			t  = new PhashWorker(imagePaths);
			t.start();
		}
	}
}
