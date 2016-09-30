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
package com.github.dozedoff.similarImage.db.repository.ormlite;

import java.sql.SQLException;
import java.util.List;

import com.github.dozedoff.similarImage.db.Tag;
import com.github.dozedoff.similarImage.db.repository.RepositoryException;
import com.github.dozedoff.similarImage.db.repository.TagRepository;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.PreparedQuery;
import com.j256.ormlite.stmt.SelectArg;

/**
 * {@link TagRepository} using ORMlite DAO for database access.
 * 
 * @author Nicholas Wright
 *
 */
public class OrmliteTagRepository implements TagRepository {
	private Dao<Tag, Long> tagDao;

	private PreparedQuery<Tag> nameQuery;

	private SelectArg nameArg;

	/**
	 * Create a Repository using ORMlite DAO to access the database.
	 * 
	 * @param tagDao
	 *            DAO for the {@link Tag} table.
	 * @throws RepositoryException
	 *             if an error occurs during prepared statement construction
	 */
	public OrmliteTagRepository(Dao<Tag, Long> tagDao) throws RepositoryException {
		this.tagDao = tagDao;

		try {
			prepareStatements();
		} catch (SQLException e) {
			throw new RepositoryException("Failed to create prepared statements", e);
		}
	}

	private void prepareStatements() throws SQLException {
		nameArg = new SelectArg();
		nameQuery = tagDao.queryBuilder().where().eq(Tag.NAME_FIELD_NAME, nameArg).prepare();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Tag getByName(String name) throws RepositoryException {
		try {
			nameArg.setValue(name);
			return tagDao.queryForFirst(nameQuery);
		} catch (SQLException e) {
			throw new RepositoryException("Failed to query by name", e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void store(Tag tag) throws RepositoryException {
		try {
			tagDao.createOrUpdate(tag);
		} catch (SQLException e) {
			throw new RepositoryException("Failed to store", e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void remove(Tag tag) throws RepositoryException {
		try {
			tagDao.delete(tag);
		} catch (SQLException e) {
			throw new RepositoryException("Failed to delete", e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<Tag> getAll() throws RepositoryException {
		try {
			return tagDao.queryForAll();
		} catch (SQLException e) {
			throw new RepositoryException("Failed to query all", e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<Tag> getWithContext() throws RepositoryException {
		try {
			return tagDao.queryForMatching(new Tag(null, true));
		} catch (SQLException e) {
			throw new RepositoryException("Failed to query by context flag", e);
		}
	}
}
