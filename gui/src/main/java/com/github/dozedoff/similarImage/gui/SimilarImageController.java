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

import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.swing.DefaultListModel;
import javax.swing.SwingUtilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.dozedoff.commonj.filefilter.SimpleImageFilter;
import com.github.dozedoff.similarImage.component.ApplicationScope;
import com.github.dozedoff.similarImage.db.ImageRecord;
import com.github.dozedoff.similarImage.db.Tag;
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
import com.github.dozedoff.similarImage.thread.GroupListPopulator;
import com.github.dozedoff.similarImage.thread.ImageFindJob;
import com.github.dozedoff.similarImage.thread.ImageFindJobVisitor;
import com.github.dozedoff.similarImage.thread.pipeline.ImageQueryPipeline;
import com.github.dozedoff.similarImage.thread.pipeline.ImageQueryPipelineBuilder;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Multimap;
import com.google.common.eventbus.Subscribe;

@ApplicationScope
public class SimilarImageController {
	private final Logger logger = LoggerFactory.getLogger(SimilarImageController.class);

	private static final String GUI_MSG_SORTING = "Sorting...";
	private static final int MAXIMUM_GROUP_SIZE = 50;

	private GroupList groupList;
	private SimilarImageView gui;
	private final Statistics statistics;
	private final LinkedList<Thread> tasks = new LinkedList<>();
	private boolean includeIgnoredImages;

	private final HandlerListFactory handlerCollectionFactory;
	private final OperationsMenuFactory omf;
	private final DefaultListModel<ResultGroup> groupListModel;
	private final LoadingCache<Result, BufferedImage> thumbnailCache;
	private final ImageQueryPipelineBuilder imagePipelineBuilder;

	/**
	 * Performs actions initiated by the user
	 * 
	 * @param pipelineBuilder
	 *            builder for image queries
	 * @param handlerCollectionFactory
	 *            factory for creating handler collection used in processing images
	 * @param opsMenuFactory
	 *            factory to create menus with operations that can be performed on images
	 * @param statistics
	 *            program statistics tracking
	 */
	@Inject
	public SimilarImageController(ImageQueryPipelineBuilder pipelineBuilder, HandlerListFactory handlerCollectionFactory,
			OperationsMenuFactory opsMenuFactory, Statistics statistics) {
		groupList = new GroupList();
		this.statistics = statistics;
		this.handlerCollectionFactory = handlerCollectionFactory;
		this.omf = opsMenuFactory;
		GuiEventBus.getInstance().register(this);
		groupListModel = new DefaultListModel<ResultGroup>();
		this.thumbnailCache = CacheBuilder.newBuilder().softValues().build(new ThumbnailCacheLoader());
		this.imagePipelineBuilder = pipelineBuilder;
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
		gui.setListModel(groupListModel);
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

	/**
	 * Display the {@link ResultGroup}, generating all necessary UI elements. Will ask the user if a group should be
	 * loaded if the image count exceeds a set threshold.
	 * 
	 * @param group
	 *            to display
	 */
	public void displayGroup(ResultGroup group) {
		List<Result> grouplist = group.getResults();

		if (grouplist.size() > MAXIMUM_GROUP_SIZE) {
			if (!gui.okToDisplayLargeGroup(grouplist.size())) {
				return;
			}
		}

		logger.info("Loading {} thumbnails for group {}", grouplist.size(), group);

		ResultGroupPresenter rgp = new ResultGroupPresenter(group, omf, this, thumbnailCache);
		gui.displayResultGroup(group.toString(), rgp);
	}

	/**
	 * Display the next group in the list.
	 * 
	 * @param currentGroup
	 *            the current displayed group, used as a reference
	 */
	public void displayNextGroup(ResultGroup currentGroup) {
		displayGroup(groupList.nextGroup(currentGroup));
	}

	/**
	 * Display the previous group in the list.
	 * 
	 * @param currentGroup
	 *            the current displayed group, used as a reference
	 */
	public void displayPreviousGroup(ResultGroup currentGroup) {
		displayGroup(groupList.previousGroup(currentGroup));
	}

	private void updateGUI() {
		setGUIStatus("" + groupList.groupCount() + " Groups");
		groupList.setMappedListModel(groupListModel);
		SwingUtilities.invokeLater(new GroupListPopulator(groupList, groupListModel));
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

	/**
	 * For every image, find other images that have a matching hash.
	 * 
	 * @param hammingDistance
	 *            distance to consider a hash a match
	 * @param path
	 *            limit images to this path, if empty or null, all images are considered
	 */
	public void sortDuplicates(int hammingDistance, String path) {
		setGUIStatus(GUI_MSG_SORTING);
		ImageQueryPipeline pipeline = imagePipelineBuilder.distance(hammingDistance).groupAll()
				.removeSingleImageGroups().removeDuplicateGroups().build();
		Thread t = createPipelineThread(pipeline, checkPath(path));
		startTask(t);
	}

	/**
	 * Match images against tagged hashes.
	 * 
	 * @param hammingDistance
	 *            distance to consider a hash a match
	 * @param tag
	 *            tag to match a against.
	 * @param path
	 *            limit images to this path, if empty or null, all images are considered
	 */
	public void sortFilter(int hammingDistance, Tag tag, String path) {

		ImageQueryPipeline pipeline = imagePipelineBuilder.distance(hammingDistance).groupByTag(tag).build();
		Thread t = createPipelineThread(pipeline, checkPath(path));

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

	/**
	 * This event is triggered whenever the group list should be repopulated.
	 * 
	 * @param event
	 *            data associated with this event
	 */
	@Subscribe
	public void updateGroup(GuiGroupEvent event) {
		setResults(event.getGroups());
	}

	/**
	 * Set if ignored images should be included in the results.
	 * 
	 * @param includeIgnoredImages
	 *            set to true if ignored images should be used.
	 */
	public void setIncludeIgnoredImages(boolean includeIgnoredImages) {
		this.includeIgnoredImages = includeIgnoredImages;
	}

	private Thread createPipelineThread(ImageQueryPipeline pipeline, Path scope) {
		return new Thread() {
			@Override
			public void run() {
				setResults(pipeline.apply(scope));
			}
		};
	}

	private Path checkPath(String path) {
		String checkedPath = path;
		if (path == null) {
			checkedPath = "";
		}

		return Paths.get(checkedPath);
	}
}
