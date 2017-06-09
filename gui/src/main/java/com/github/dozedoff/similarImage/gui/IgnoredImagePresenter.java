/*  Copyright (C) 2017  Nicholas Wright
    
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

import java.util.List;
import java.util.function.Consumer;

import javax.swing.DefaultListModel;
import javax.swing.SwingUtilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.dozedoff.similarImage.db.IgnoreRecord;
import com.github.dozedoff.similarImage.db.repository.IgnoreRepository;
import com.github.dozedoff.similarImage.db.repository.RepositoryException;

/**
 * Presenter for the settings window to manage ignored images.
 * 
 * @author Nicholas Wright
 *
 */
public class IgnoredImagePresenter {
	private static final Logger LOGGER = LoggerFactory.getLogger(IgnoredImagePresenter.class);

	private IgnoredImageView view;
	private final DefaultListModel<IgnoreRecord> model;
	private final IgnoreRepository ignoreRepository;

	/**
	 * Create a new presenter with the given repository.
	 * 
	 * @param ignoreRepository
	 *            to use for ignored image queries and to remove ignored images
	 */
	public IgnoredImagePresenter(IgnoreRepository ignoreRepository) {
		this.ignoreRepository = ignoreRepository;
		this.model = new DefaultListModel<IgnoreRecord>();
	}

	/**
	 * Set the view this presenter will use.
	 * 
	 * @param view
	 *            to use
	 */
	public void setView(IgnoredImageView view) {
		this.view = view;
		refreshList();
	}

	/**
	 * Get the backing model this presenter is using.
	 * 
	 * @return the backing model
	 */
	public DefaultListModel<IgnoreRecord> getModel() {
		return model;
	}

	/**
	 * Reload the ignored image list from the repository
	 */
	public void refreshList() {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				model.clear();

				try {
					ignoreRepository.getAll().forEach(new Consumer<IgnoreRecord>() {
						@Override
						public void accept(IgnoreRecord t) {
							model.addElement(t);
						}
					});
				} catch (RepositoryException e) {
					LOGGER.error("Failed to load ignored image list: {} cause:{}", e.getCause());
				}

				view.pack();
			}
		});
	}

	/**
	 * Remove the ignored images from the repository.
	 * 
	 * @param toRemove
	 *            ignored images to remove
	 */
	public void removeIgnoredImages(List<IgnoreRecord> toRemove) {
		LOGGER.info("Removing {} ignored images", toRemove.size());
		toRemove.forEach(this::removeIgnoredImage);
	}

	private void removeIgnoredImage(IgnoreRecord ignore) {
		try {
			LOGGER.debug("Removing ignored image {}", ignore);
			this.ignoreRepository.remove(ignore);
		} catch (RepositoryException e) {
			LOGGER.warn("Failed to remove {}: {}, cause: {}", ignore, e.toString(), e.getCause());
		}
	}
}
