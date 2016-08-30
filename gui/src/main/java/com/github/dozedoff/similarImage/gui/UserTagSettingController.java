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

import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.dozedoff.similarImage.db.CustomUserTag;
import com.j256.ormlite.dao.Dao;

/**
 * Performs actions triggered by the GUI.
 * 
 * @author Nicholas Wright
 *
 */
public class UserTagSettingController {
	private static final Logger LOGGER = LoggerFactory.getLogger(UserTagSettingController.class);
	private final Dao<CustomUserTag, Long> dao;

	/**
	 * Create a controller using the given DAO.
	 * 
	 * @param dao
	 *            to use
	 */
	public UserTagSettingController(Dao<CustomUserTag, Long> dao) {
		this.dao = dao;
	}

	/**
	 * Get all user defined tags.
	 * 
	 * @return a collection of tags
	 */
	public Collection<CustomUserTag> getAllUserTags() {
		try {
			return dao.queryForAll();
		} catch (SQLException e) {
			LOGGER.error("Failed to get user tags: {}", e.toString());
			return Collections.emptyList();
		}
	}

	/**
	 * Remove the tag from the database
	 * 
	 * @param tag
	 *            to remove
	 */
	public void removeTag(CustomUserTag tag) {
		try {
			dao.delete(tag);
		} catch (SQLException e) {
			LOGGER.error("Failed to delete user tag {}: {}", tag, e.toString());
		}
	}

	/**
	 * Add the given tag to the database.
	 * 
	 * @param tag
	 *            to add
	 * @return created data object
	 */
	public CustomUserTag addTag(String tag) {
		CustomUserTag dbTag = new CustomUserTag(tag);

		try {
			dao.create(dbTag);
		} catch (SQLException e) {
			LOGGER.error("Failed to persist tag {}: {}", tag, e.toString());
		}

		return dbTag;
	}
}
