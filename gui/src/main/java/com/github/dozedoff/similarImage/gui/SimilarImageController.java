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
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.dozedoff.commonj.filefilter.SimpleImageFilter;
import com.github.dozedoff.commonj.hash.ImagePHash;
import com.github.dozedoff.similarImage.db.FilterRecord;
import com.github.dozedoff.similarImage.db.ImageRecord;
import com.github.dozedoff.similarImage.db.Persistence;
import com.github.dozedoff.similarImage.db.Tag;
import com.github.dozedoff.similarImage.db.Thumbnail;
import com.github.dozedoff.similarImage.db.repository.FilterRepository;
import com.github.dozedoff.similarImage.db.repository.RepositoryException;
import com.github.dozedoff.similarImage.db.repository.TagRepository;
import com.github.dozedoff.similarImage.db.repository.ormlite.OrmliteFilterRepository;
import com.github.dozedoff.similarImage.db.repository.ormlite.OrmliteTagRepository;
import com.github.dozedoff.similarImage.duplicate.ImageInfo;
import com.github.dozedoff.similarImage.event.GuiEventBus;
import com.github.dozedoff.similarImage.event.GuiGroupEvent;
import com.github.dozedoff.similarImage.handler.DatabaseHandler;
import com.github.dozedoff.similarImage.handler.ExtendedAttributeHandler;
import com.github.dozedoff.similarImage.handler.HashHandler;
import com.github.dozedoff.similarImage.handler.HashNames;
import com.github.dozedoff.similarImage.handler.HashingHandler;
import com.github.dozedoff.similarImage.io.ExtendedAttribute;
import com.github.dozedoff.similarImage.io.HashAttribute;
import com.github.dozedoff.similarImage.io.Statistics;
import com.github.dozedoff.similarImage.thread.FilterSorter;
import com.github.dozedoff.similarImage.thread.ImageFindJob;
import com.github.dozedoff.similarImage.thread.ImageFindJobVisitor;
import com.github.dozedoff.similarImage.thread.ImageSorter;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.google.common.eventbus.Subscribe;
import com.j256.ormlite.dao.DaoManager;

public class SimilarImageController {
	private final Logger logger = LoggerFactory.getLogger(SimilarImageController.class);

	private static final String GUI_MSG_SORTING = "Sorting...";

	private final int THUMBNAIL_DIMENSION = 500;

	private final Persistence persistence;
	private FilterRepository filterRepository;
	private TagRepository tagRepository;
	private Multimap<Long, ImageRecord> results;
	private DisplayGroupView displayGroup;
	private SimilarImageView gui;
	private final ExecutorService threadPool;
	private final Statistics statistics;
	private final LinkedList<Thread> tasks = new LinkedList<>();

	/**
	 * Performs actions initiated by the user
	 * 
	 * @param persistence
	 *            database access
	 * @param displayGroup
	 *            view for displaying images for groups
	 * 
	 * @param threadPool
	 *            for performing tasks
	 * @param statistics
	 *            tracking stats
	 */
	public SimilarImageController(Persistence persistence, DisplayGroupView displayGroup, ExecutorService threadPool,
			Statistics statistics) {
		this(persistence, null, null, displayGroup, threadPool, statistics);

		try {
			this.filterRepository = new OrmliteFilterRepository(DaoManager.createDao(persistence.getCs(), FilterRecord.class),
					DaoManager.createDao(persistence.getCs(), Thumbnail.class));

			this.tagRepository = new OrmliteTagRepository(DaoManager.createDao(persistence.getCs(), Tag.class));
		} catch (SQLException | RepositoryException e) {
			logger.error("Failed to setup repository");
		}
	}

	/**
	 * Performs actions initiated by the user
	 * 
	 * @param persistence
	 *            legacy DAO god class
	 * @param filterRepository
	 *            filter datasource access
	 * @param tagRepository
	 *            tag datasource access
	 * @param displayGroup
	 *            view for displaying images for groups
	 * 
	 * @param threadPool
	 *            for performing tasks
	 * @param statistics
	 *            tracking stats
	 */
	public SimilarImageController(Persistence persistence, FilterRepository filterRepository, TagRepository tagRepository,
			DisplayGroupView displayGroup, ExecutorService threadPool, Statistics statistics) {
		this.persistence = persistence;
		this.filterRepository = filterRepository;
		this.tagRepository = tagRepository;
		results = MultimapBuilder.hashKeys().hashSetValues().build();
		this.displayGroup = displayGroup;
		this.threadPool = threadPool;
		this.statistics = statistics;
		GuiEventBus.getInstance().register(this);
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
		return new HashSet<ImageRecord>(results.get(group));
	}

	/**
	 * Update the search results and refresh the GUI.
	 * 
	 * @param results
	 *            to use in GUI selections
	 */
	public synchronized void setResults(Multimap<Long, ImageRecord> results) {
		this.results = results;
		updateGUI();
	}

	public void displayGroup(long group) {
		int maxGroupSize = 30;

		Set<ImageRecord> grouplist = getGroup(group);
		LinkedList<View> images = new LinkedList<View>();
		Dimension imageDim = new Dimension(THUMBNAIL_DIMENSION, THUMBNAIL_DIMENSION);

		if (grouplist.size() > maxGroupSize) {
			if (!gui.okToDisplayLargeGroup(grouplist.size())) {
				return;
			}
		}

		logger.info("Loading {} thumbnails for group {}", grouplist.size(), group);

		for (ImageRecord rec : grouplist) {
			Path path = Paths.get(rec.getPath());

			if (Files.exists(path)) {
				ImageInfo info = new ImageInfo(path, rec.getpHash());
				OperationsMenu opMenu;
				try {
					opMenu = new OperationsMenu(info, persistence,
							new UserTagSettingController(DaoManager.createDao(persistence.getCs(), Tag.class)));
					DuplicateEntryController entry = new DuplicateEntryController(info, imageDim);
					new DuplicateEntryView(entry, opMenu);
					images.add(entry);
				} catch (SQLException e) {
					logger.warn("Failed to create Operations menu for {}: {}", info.getPath(), e.toString());
				}

			} else {
				logger.warn("Image {} not found, skipping...", path);
			}
		}

		displayGroup.displayImages(group, images);
	}

	private void updateGUI() {
		setGUIStatus("" + results.keySet().size() + " Groups");
		gui.populateGroupList(results.keySet());
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

		List<HashHandler> handlers = new ArrayList<HashHandler>();

		handlers.add(new DatabaseHandler(persistence, statistics));

		if (ExtendedAttribute.supportsExtendedAttributes(Paths.get(path))) {
			handlers.add(new ExtendedAttributeHandler(hashAttribute, persistence));
			handlers.add(new HashingHandler(threadPool, new ImagePHash(), persistence, statistics, hashAttribute));
			logger.info("Extended attributes are supported for {}", path);
		} else {
			logger.info("Extended attributes are NOT supported for {}, disabling...", path);
			handlers.add(new HashingHandler(threadPool, new ImagePHash(), persistence, statistics, null));
		}

		ImageFindJobVisitor visitor = new ImageFindJobVisitor(new SimpleImageFilter(), handlers, statistics);

		// TODO use a priority queue to let FindJobs run first
		Thread t = new Thread(new ImageFindJob(path, visitor));
		t.setName("Image Find Job");
		startTask(t);
	}

	public void sortDuplicates(int hammingDistance, String path) {
		setGUIStatus(GUI_MSG_SORTING);
		Thread t = new ImageSorter(hammingDistance, path, persistence);
		startTask(t);
	}

	public void sortFilter(int hammingDistance, Tag tag, String path) {
		Thread t;
		if (path.isEmpty()) {
			t = new FilterSorter(hammingDistance, tag, persistence, filterRepository, tagRepository);
		} else {
			t = new FilterSorter(hammingDistance, tag, persistence, filterRepository, tagRepository, Paths.get(path));
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
		((ThreadPoolExecutor) threadPool).getQueue().clear();
	}

	/**
	 * Get the approximate number of queued tasks.
	 * 
	 * @return number of queued tasks
	 */
	public int getNumberOfQueuedTasks() {
		return ((ThreadPoolExecutor) threadPool).getQueue().size();
	}

	@Subscribe
	public void updateGroup(GuiGroupEvent event) {
		setResults(event.getGroups());
	}
}
