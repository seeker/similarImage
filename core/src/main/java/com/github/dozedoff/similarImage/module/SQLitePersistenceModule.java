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
package com.github.dozedoff.similarImage.module;

import java.nio.file.Path;
import java.nio.file.Paths;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.github.dozedoff.similarImage.db.Database;
import com.github.dozedoff.similarImage.db.SQLiteDatabase;
import com.github.dozedoff.similarImage.db.repository.FilterRepository;
import com.github.dozedoff.similarImage.db.repository.IgnoreRepository;
import com.github.dozedoff.similarImage.db.repository.ImageRepository;
import com.github.dozedoff.similarImage.db.repository.PendingHashImageRepository;
import com.github.dozedoff.similarImage.db.repository.Repository;
import com.github.dozedoff.similarImage.db.repository.RepositoryException;
import com.github.dozedoff.similarImage.db.repository.TagRepository;
import com.github.dozedoff.similarImage.db.repository.ormlite.OrmliteRepositoryFactory;
import com.github.dozedoff.similarImage.db.repository.ormlite.RepositoryFactory;
import com.j256.ormlite.misc.TransactionManager;
import com.j256.ormlite.support.ConnectionSource;

import dagger.Module;
import dagger.Provides;

@Module
public class SQLitePersistenceModule {
	private final static String DEFAULT_DB_PATH = "similarImage.db";

	private final Path databasePath;

	@Inject
	public SQLitePersistenceModule() {
		this.databasePath = Paths.get(DEFAULT_DB_PATH);
	}

	public SQLitePersistenceModule(Path databasePath) {
		this.databasePath = databasePath;
	}

	private RuntimeException runtimeException(Class<? extends Repository> repository, Throwable e) {
		return new RuntimeException("Failed to create repository " + repository.getSimpleName(), e);
	}

	@Singleton
	@Provides
	public Database provideDatabase() {
		return new SQLiteDatabase(databasePath);
	}

	@Singleton
	@Provides
	public ConnectionSource provideConnectionSource(Database database) {
		return database.getCs();
	}

	@Singleton
	@Provides
	public RepositoryFactory provideRepositoryFactory(Database database) {
		return new OrmliteRepositoryFactory(database);
	}

	@Singleton
	@Provides
	public ImageRepository provideImageRepository(RepositoryFactory repositoryFactory) {
		try {
			return repositoryFactory.buildImageRepository();
		} catch (RepositoryException e) {
			throw runtimeException(ImageRepository.class, e);
		}
	}

	@Singleton
	@Provides
	public PendingHashImageRepository providePendingHashImageRepository(RepositoryFactory repositoryFactory) {
		try {
			return repositoryFactory.buildPendingHashImageRepository();
		} catch (RepositoryException e) {
			throw runtimeException(PendingHashImageRepository.class, e);
		}
	}

	@Singleton
	@Provides
	public FilterRepository provideFilterRepository(RepositoryFactory repositoryFactory) {
		try {
			return repositoryFactory.buildFilterRepository();
		} catch (RepositoryException e) {
			throw runtimeException(FilterRepository.class, e);
		}
	}

	@Singleton
	@Provides
	public TagRepository provideTagRepository(RepositoryFactory repositoryFactory) {
		try {
			return repositoryFactory.buildTagRepository();
		} catch (RepositoryException e) {
			throw runtimeException(TagRepository.class, e);
		}
	}

	@Singleton
	@Provides
	public IgnoreRepository provideIgnoreRepository(RepositoryFactory repositoryFactory) {
		try {
			return repositoryFactory.buildIgnoreRepository();
		} catch (RepositoryException e) {
			throw runtimeException(IgnoreRepository.class, e);
		}
	}

	@Singleton
	@Provides
	public TransactionManager provideTransactionManager(ConnectionSource cs) {
		return new TransactionManager(cs);
	}
}
