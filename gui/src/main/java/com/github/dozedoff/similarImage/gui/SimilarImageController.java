/*  Copyright (C) 2016  Nicholas Wright
    
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
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.dozedoff.commonj.filefilter.SimpleImageFilter;
import com.github.dozedoff.similarImage.component.ApplicationScope;
import com.github.dozedoff.similarImage.db.ImageRecord;
import com.github.dozedoff.similarImage.db.Tag;
import com.github.dozedoff.similarImage.duplicate.DuplicateOperations;
import com.github.dozedoff.similarImage.duplicate.ImageInfo;
import com.github.dozedoff.similarImage.event.GuiEventBus;
import com.github.dozedoff.similarImage.event.GuiGroupEvent;
import com.github.dozedoff.similarImage.handler.HandlerListFactory;
import com.github.dozedoff.similarImage.handler.HashHandler;
import com.github.dozedoff.similarImage.handler.HashNames;
import com.github.dozedoff.similarImage.io.HashAttribute;
import com.github.dozedoff.similarImage.io.Statistics;
import com.github.dozedoff.similarImage.result.GroupList;
import com.github.dozedoff.similarImage.result.Result;
import com.github.dozedoff.similarImage.result.ResultGroup;
import com.github.dozedoff.similarImage.thread.ImageFindJob;
import com.github.dozedoff.similarImage.thread.ImageFindJobVisitor;
import com.github.dozedoff.similarImage.thread.SorterFactory;
import com.google.common.collect.Multimap;
import com.google.common.eventbus.Subscribe;

@ApplicationScope
public class SimilarImageController {
	private final Logger logger = LoggerFactory.getLogger(SimilarImageController.class);

	private static final String GUI_MSG_SORTING = "Sorting...";

	private final int THUMBNAIL_DIMENSION = 500;

	private GroupList groupList;
	private DisplayGroupView displayGroup;
	private SimilarImageView gui;
	private final Statistics statistics;
	private final LinkedList<Thread> tasks = new LinkedList<>();

	private final DuplicateOperations dupOps;
	private final SorterFactory sorterFactory;
	private final HandlerListFactory handlerCollectionFactory;
	private final UserTagSettingController utsc;

	/**
	 * Performs actions initiated by the user
	 * 
	 * @param displayGroup
	 *            view for displaying images for groups
	 * @param statistics
	 *            tracking stats
	 */
	@Inject
	public SimilarImageController(SorterFactory sorterFactory, HandlerListFactory handlerCollectionFactory,
			DuplicateOperations dupOps, DisplayGroupView displayGroup, Statistics statistics,
			UserTagSettingController utsc) {

		groupList = new GroupList();
		this.displayGroup = displayGroup;
		this.statistics = statistics;
		this.sorterFactory = sorterFactory;
		this.handlerCollectionFactory = handlerCollectionFactory;
		this.dupOps = dupOps;
		this.utsc = utsc;
		GuiEventBus.getInstance().register(this);
	}

	private void setGroupListToResult(Multimap<Long, ImageRecord> results) {
		Set<Long> keys = results.keySet();
		List<ResultGroup> groups = keys.stream().map(key -> new ResultGroup(groupList, key, results.get(key)))
				.collect(Collectors.toList());

		groupList.populateList(groups);
	}

	/**
	 * Set the user interface this controller should interact with, and register it with the statistics tracker.
	 * 
	 * @param gui
	 *            the view to use
	 */
	public final void setGui(SimilarImageView gui) {
		if (this.gui != null) {
			statistics.removeStatisticsListener(this.gui);
		}

		this.gui = gui;
		statistics.addStatisticsListener(gui);
	}

	public void ignoreImage(ImageRecord toIgnore) {
		throw new RuntimeException("Not implemented yet");
	}

	/**
	 * Get the images associated with this group.
	 * 
	 * @param group
	 *            to query
	 * @return images matched to this group
	 */
	public Set<ImageRecord> getGroup(long group) {
		// TODO pass group directly (or even grouplist?)
		List<Result> results = groupList.getGroup(group).getResults();
		return new HashSet<ImageRecord>(resultsToImageRecords(results));
	}

	private List<ImageRecord> resultsToImageRecords(List<Result> results) {
		return results.stream().map(result -> result.getImageRecord()).collect(Collectors.toList());
	}

	/**
	 * Update the search results and refresh the GUI.
	 * 
	 * @param results
	 *            to use in GUI selections
	 */
	public synchronized void setResults(Multimap<Long, ImageRecord> results) {
		setGroupListToResult(results);
		updateGUI();
	}

	public void displayGroup(ResultGroup group) {
		int maxGroupSize = 30;

		List<Result> grouplist = group.getResults();
		LinkedList<View> images = new LinkedList<View>();
		Dimension imageDim = new Dimension(THUMBNAIL_DIMENSION, THUMBNAIL_DIMENSION);

		if (grouplist.size() > maxGroupSize) {
			if (!gui.okToDisplayLargeGroup(grouplist.size())) {
				return;
			}
		}

		logger.info("Loading {} thumbnails for group {}", grouplist.size(), group);

		for (Result rec : grouplist) {
			ImageRecord ir = rec.getImageRecord();
			Path path = Paths.get(ir.getPath());

			if (Files.exists(path)) {
				ImageInfo info = new ImageInfo(path, ir.getpHash());
				OperationsMenu opMenu;

				opMenu = new OperationsMenu(rec, dupOps, utsc);
				DuplicateEntryController entry = new DuplicateEntryController(info);
				new DuplicateEntryView(entry, opMenu);
				images.add(entry);

			} else {
				logger.warn("Image {} not found, skipping...", path);
			}
		}

		displayGroup.displayImages(group.toString(), images);
	}

	private void updateGUI() {
		setGUIStatus("" + groupList.groupCount() + " Groups");
		gui.populateGroupList(groupList);
	}

	private void setGUIStatus(String message) {
		guiSetCheck();
		gui.setStatus(message);
	}

	private void guiSetCheck() {
		if (gui == null) {
			throw new RuntimeException("GUI was not set for the controller! Use setGui()!");
		}
	}

	private void startTask(Thread thread) {
		thread.start();
		tasks.add(thread);
	}

	public void indexImages(String path) {
		HashAttribute hashAttribute = new HashAttribute(HashNames.DEFAULT_DCT_HASH_2);

		List<HashHandler> handlers;

		try {
			handlers = handlerCollectionFactory.withExtendedAttributeSupport(hashAttribute);

			ImageFindJobVisitor visitor = new ImageFindJobVisitor(new SimpleImageFilter(), handlers, statistics);

			// TODO use a priority queue to let FindJobs run first
			Thread t = new Thread(new ImageFindJob(path, visitor));
			t.setName("Image Find Job");
			startTask(t);

		} catch (Exception e) {
			logger.error("Failed to setup broker connection: {}", e.toString());
		}
	}

	public void sortDuplicates(int hammingDistance, String path) {
		setGUIStatus(GUI_MSG_SORTING);
		Thread t = sorterFactory.newImageSorter(hammingDistance, path);
		startTask(t);
	}

	public void sortFilter(int hammingDistance, Tag tag, String path) {
		Thread t;
		if (path.isEmpty()) {
			t = sorterFactory.newFilterSorterAllImages(hammingDistance, tag);
		} else {
			t = sorterFactory.newFilterSorterRestrictByPath(hammingDistance, tag, Paths.get(path));
		}

		startTask(t);
	}

	/**
	 * Stop all running Jobs.
	 */
	public void stopWorkers() {
		logger.info("Stopping running jobs...");

		for (Thread t : tasks) {
			t.interrupt();
		}

		tasks.clear();

		logger.info("Clearing queue...");
		// TODO clear message queues
	}

	/**
	 * Get the approximate number of queued tasks.
	 * 
	 * @return number of queued tasks
	 */
	public int getNumberOfQueuedTasks() {
		// TODO remove me
		return 0;
	}

	@Subscribe
	public void updateGroup(GuiGroupEvent event) {
		setResults(event.getGroups());
	}
}
