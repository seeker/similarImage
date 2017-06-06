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

import java.util.concurrent.TimeUnit;

import com.github.dozedoff.similarImage.db.repository.FilterRepository;
import com.github.dozedoff.similarImage.db.repository.ImageRepository;
import com.github.dozedoff.similarImage.duplicate.DuplicateOperations;
import com.github.dozedoff.similarImage.gui.OperationsMenuFactory;
import com.github.dozedoff.similarImage.gui.UserTagSettingController;
import com.github.dozedoff.similarImage.io.ExtendedAttribute;
import com.github.dozedoff.similarImage.io.ExtendedAttributeDirectoryCache;
import com.github.dozedoff.similarImage.io.ExtendedAttributeQuery;
import com.github.dozedoff.similarImage.thread.pipeline.ImageQueryPipelineBuilder;

import dagger.Module;
import dagger.Provides;

@Module
public class GuiModule {

	@Provides
	public ExtendedAttributeQuery provideExtendedAttributeQuery() {
		return new ExtendedAttributeDirectoryCache(new ExtendedAttribute(), 1, TimeUnit.MINUTES);
	}

	@Provides
	public OperationsMenuFactory provideOperationsMenuFactory(DuplicateOperations dupOps,
			UserTagSettingController utsc) {
		return new OperationsMenuFactory(dupOps, utsc);
	}

	@Provides
	public ImageQueryPipelineBuilder provideImageQueryPipelineBuilder(ImageRepository imageRepository,
			FilterRepository filterRepository) {
		return ImageQueryPipelineBuilder.newBuilder(imageRepository, filterRepository);
	}
}
