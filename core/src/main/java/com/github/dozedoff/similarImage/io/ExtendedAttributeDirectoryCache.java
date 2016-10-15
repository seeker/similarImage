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
package com.github.dozedoff.similarImage.io;

import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

/**
 * Checks and caches extended attribute support for directories.
 * 
 * @author Nicholas Wright
 *
 */
public class ExtendedAttributeDirectoryCache implements ExtendedAttributeQuery {
	private static final int DEAFULT_EXPIRE_TIME = 1;
	private static final TimeUnit DEAFULT_EXPIRE_UNIT = TimeUnit.MINUTES;

	private final int expireTime;
	private final TimeUnit expireUnit;
	private final ExtendedAttributeQuery eaQuery;

	private LoadingCache<Path, Boolean> eaSupport;

	/**
	 * Create a new cache with the default expire time.
	 * 
	 * @param eaQuery
	 *            used to query extended attribute support
	 */
	public ExtendedAttributeDirectoryCache(ExtendedAttributeQuery eaQuery) {
		this(eaQuery, DEAFULT_EXPIRE_TIME, DEAFULT_EXPIRE_UNIT);
	}

	/**
	 * Creates a new cache with the given expire time.
	 * 
	 * @param eaQuery
	 *            used to query extended attribute support
	 * @param expireTime
	 *            until entries expire
	 * @param expireUnit
	 *            for expireTime
	 */
	public ExtendedAttributeDirectoryCache(ExtendedAttributeQuery eaQuery, int expireTime, TimeUnit expireUnit) {
		this.eaQuery = eaQuery;
		this.expireTime = expireTime;
		this.expireUnit = expireUnit;

		setupCache();
	}

	private void setupCache() {
		eaSupport = CacheBuilder.newBuilder().expireAfterAccess(expireTime, expireUnit)
				.build(new CacheLoader<Path, Boolean>() {
					@Override
					public Boolean load(Path key) throws Exception {
						return eaQuery.isEaSupported(key);
					}
				});

	}

	/**
	 * Checks for extended attribute support of the file by checking and caching the parent.
	 * 
	 * @param path
	 *            to check
	 * @return true if extended attribute is supported
	 */
	@Override
	public boolean isEaSupported(Path path) {
		Path parent = path.getParent();

		if (path.getRoot() != null && path.equals(path.getRoot())) {
			parent = path;
		}

		if (parent == null) {
			return false;
		}

		return eaSupport.getUnchecked(parent);
	}
}
