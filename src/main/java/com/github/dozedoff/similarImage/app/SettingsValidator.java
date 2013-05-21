/*  Copyright (C) 2013  Nicholas Wright
    
    This file is part of similarImage - A similar image finder using pHash
    
    mmut is free software: you can redistribute it and/or modify
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
package com.github.dozedoff.similarImage.app;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.dozedoff.commonj.settings.AbstractSettings;
import com.github.dozedoff.commonj.settings.ISettingsValidator;
import com.github.dozedoff.similarImage.app.Settings.Parameters;

public class SettingsValidator implements ISettingsValidator {
	private static final Logger logger = LoggerFactory.getLogger(SettingsValidator.class);
	private boolean allOk = true;
	
	@Override
	public boolean validate(AbstractSettings settings) {
		if (!(settings instanceof Settings)) {
			return false;
		}

		Settings set = (Settings) settings;

		checkRange(Parameters.data_loader_priority, set.getDataLoaderPriority(), Thread.MIN_PRIORITY, Thread.MAX_PRIORITY);
		checkGreaterZero(Parameters.data_loaders, set.getDataLoaders());
		checkGreaterZero(Parameters.loader_out_queue_size, set.getLoaderOutQueueSize());
		checkGreaterZero(Parameters.phash_workers, set.getpHashWorkers());
		checkGreaterZero(Parameters.thumbnail_dimension, set.getThumbnailDimension());

		return allOk;
	}
	
	private void checkGreaterZero(Parameters param, int value) {
		if (!(value > 0)) {
			setOk(false);
			logger.warn("Value for {} must be greater than 0, currently set to {}", param.toString(), value);
		}
	}
	
	private void checkRange(Parameters param, int value, int min, int max) {
		if(! ((value >= min) && (value <= max))){
			setOk(false);
			Object loggerData[] = {param.toString(), };
			logger.warn("Value for {} must be between {} and {}, currently set to {}", loggerData);
		}
	}

	private void setOk(boolean ok) {
		allOk &= ok;
	}
}
