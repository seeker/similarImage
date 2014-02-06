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
package com.github.dozedoff.similarImage.gui;

import java.awt.Dimension;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.Set;

import javax.swing.JDialog;
import javax.swing.JOptionPane;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.dozedoff.similarImage.db.DBWriter;
import com.github.dozedoff.similarImage.db.ImageRecord;
import com.github.dozedoff.similarImage.db.Persistence;
import com.github.dozedoff.similarImage.duplicate.DuplicateOperations;
import com.github.dozedoff.similarImage.duplicate.ImageInfo;
import com.github.dozedoff.similarImage.duplicate.SortSimilar;
import com.github.dozedoff.similarImage.io.ImageProducer;
import com.github.dozedoff.similarImage.thread.FilterSorter;
import com.github.dozedoff.similarImage.thread.ImageIndexer;
import com.github.dozedoff.similarImage.thread.ImageSorter;

public class SimilarImageController {
	private final Logger logger = LoggerFactory.getLogger(SimilarImageController.class);

	private final int THUMBNAIL_DIMENSION = 500;
	private final int LOADER_THREADS = 2;
	private final int LOADER_PRIORITY = 2;
	private final int PRODUCER_QUEUE_SIZE = 400;

	private final Persistence persistence;

	private SortSimilar sorter;
	private DisplayGroupView displayGroup;
	private SimilarImageView gui;
	private ImageProducer producer;

	private ImageIndexer indexer;

	public SimilarImageController(Persistence persistence) {
		this.persistence = persistence;
		setupProducer();

		sorter = new SortSimilar(persistence);
		displayGroup = new DisplayGroupView();
		gui = new SimilarImageView(this, new DuplicateOperations(persistence), producer.getTotalProgress(), producer.getBufferLevel());
	}

	private void setupProducer() {
		producer = new ImageProducer(PRODUCER_QUEUE_SIZE, persistence);
		producer.setThreadPriority(LOADER_PRIORITY);
		producer.startLoader(LOADER_THREADS);
	}

	public void ignoreImage(ImageRecord toIgnore) {
		sorter.ignore(toIgnore);
	}

	public Set<ImageRecord> getGroup(long group) {
		return sorter.getGroup(group);
	}

	public void displayGroup(long group) {
		int maxGroupSize = 30;

		Set<ImageRecord> grouplist = getGroup(group);
		LinkedList<View> images = new LinkedList<View>();
		Dimension imageDim = new Dimension(THUMBNAIL_DIMENSION, THUMBNAIL_DIMENSION);

		if (grouplist.size() > maxGroupSize) {
			Object[] message = { "Group size is " + grouplist.size() + "\nContinue loading?" };
			JOptionPane pane = new JOptionPane(message, JOptionPane.PLAIN_MESSAGE, JOptionPane.OK_CANCEL_OPTION);
			JDialog getTopicDialog = pane.createDialog(null, "Continue?");
			getTopicDialog.setVisible(true);

			if (pane.getValue() != null && (Integer) pane.getValue() == JOptionPane.CANCEL_OPTION) {
				return;
			}
		}

		logger.info("Loading {} thumbnails for group {}", grouplist.size(), group);

		for (ImageRecord rec : grouplist) {
			Path path = Paths.get(rec.getPath());

			if (Files.exists(path)) {
				ImageInfo info = new ImageInfo(path, persistence);
				DuplicateEntryController entry = new DuplicateEntryController(this, info, persistence, imageDim);
				images.add(entry);
			} else {
				logger.warn("Image {} not found, skipping...", path);
			}
		}

		displayGroup.displayImages(group, images);
	}

	public void indexImages(String path) {
		DBWriter dbWriter = new DBWriter(persistence);
		Thread t = new ImageIndexer(path, gui, producer, dbWriter);
		t.start();
	}

	public void sortDuplicates(int hammingDistance, String path) {
		Thread t = new ImageSorter(hammingDistance, path, gui, sorter, persistence);
		t.start();
	}

	public void sortFilter(int hammingDistance, String reason) {
		Thread t = new FilterSorter(hammingDistance, reason, gui, sorter, persistence);
		t.start();
	}

	public void stopWorkers() {
		logger.info("Stopping all workers...");
		producer.clear();
		// FIXME stop workers

		if (indexer != null && indexer.isAlive()) {
			indexer.killAll();
		}
	}
}
