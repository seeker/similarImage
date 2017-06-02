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
package com.github.dozedoff.similarImage.component;

import javax.inject.Singleton;

import com.github.dozedoff.similarImage.db.Database;
import com.github.dozedoff.similarImage.db.repository.FilterRepository;
import com.github.dozedoff.similarImage.db.repository.IgnoreRepository;
import com.github.dozedoff.similarImage.db.repository.ImageRepository;
import com.github.dozedoff.similarImage.db.repository.PendingHashImageRepository;
import com.github.dozedoff.similarImage.db.repository.TagRepository;
import com.github.dozedoff.similarImage.db.repository.ormlite.RepositoryFactory;
import com.github.dozedoff.similarImage.module.SQLitePersistenceModule;
import com.j256.ormlite.misc.TransactionManager;

import dagger.Component;

@Singleton
@Component(modules = { SQLitePersistenceModule.class })
public interface PersistenceComponent {
	Database getDatabase();

	RepositoryFactory getRepositoryFactory();

	// TODO remove methods below here, they are temporary for refactoring
	ImageRepository getImageRepository();
	PendingHashImageRepository getPendingHashImageRepository();
	FilterRepository getFilterRepository();
	TagRepository getTagRepository();

	IgnoreRepository getIgnoreRepository();

	TransactionManager getTransactionManager();
}
