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
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.dozedoff.similarImage.result.Result;
import com.github.dozedoff.similarImage.result.ResultGroup;
import com.google.common.base.Stopwatch;
import com.google.common.cache.LoadingCache;

public class ResultGroupPresenter {
	private static final Logger LOGGER = LoggerFactory.getLogger(ResultGroupPresenter.class);

	private ResultGroup resultGroup;
	private OperationsMenuFactory menuFactory;
	private final SimilarImageController siController;
	private final LoadingCache<Result, BufferedImage> thumbnailCache;

	/**
	 * Create a new presenter for a group of {@link Result}s.
	 * 
	 * @param resultGroup
	 *            to present
	 * @param menuFactory
	 *            for creating context menus
	 * @param siController
	 *            the parent controller for navigation callbacks
	 * @param thumbnailCache
	 *            for caching and loading thumbnails
	 */
	public ResultGroupPresenter(ResultGroup resultGroup, OperationsMenuFactory menuFactory,
			SimilarImageController siController, LoadingCache<Result, BufferedImage> thumbnailCache) {
		this.resultGroup = resultGroup;
		this.menuFactory = menuFactory;
		this.siController = siController;
		this.thumbnailCache = thumbnailCache;
	}

	/**
	 * Bind a view to this presenter.
	 * 
	 * @param view
	 *            to bind
	 */
	public void setView(ResultGroupView view) {
		List<Result> results = resultGroup.getResults();
		Stopwatch sw = Stopwatch.createStarted();

		List<ResultView> views;

		try {
			views = createResultViews(results.parallelStream());
		} catch (OutOfMemoryError oome) {
			LOGGER.warn("JVM ran out of memory loading {}, falling back to single threaded mode", resultGroup);
			views = createResultViews(results.stream());
		}

		for (ResultView resultView : views) {
			view.addResultView(resultView);
		}

		LOGGER.info("Loaded {} results in {}", results.size(), sw);
	}
	
	private List<ResultView> createResultViews(Stream<Result> stream) {
		return stream.map(new Function<Result, ResultView>() {
			@Override
			public ResultView apply(Result t) {
				return createResultView(t);
			}
		}).collect(Collectors.toList());
	}

	private ResultView createResultView(Result result) {
		ResultPresenter resultPresenter = new ResultPresenter(result, thumbnailCache);
		return new ResultView(resultPresenter, menuFactory.createOperationsMenu(result));
	}

	/**
	 * Display the previous group group
	 */
	public void previousGroup() {
		siController.displayPreviousGroup(resultGroup);
	}

	/**
	 * Display the next group
	 */
	public void nextGroup() {
		siController.displayNextGroup(resultGroup);
	}
}
