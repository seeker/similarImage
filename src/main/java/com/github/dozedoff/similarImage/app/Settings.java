/*  Copyright (C) 2013  Nicholas Wright
    
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
package com.github.dozedoff.similarImage.app;

import com.github.dozedoff.commonj.settings.AbstractSettings;
import com.github.dozedoff.commonj.settings.ISettingsValidator;
import com.github.dozedoff.commonj.string.Convert;

public class Settings extends AbstractSettings {
	private static Settings instance = null;

	public enum Parameters {
		phash_workers, data_loaders, data_loader_priority, thumbnail_dimension, loader_out_queue_size, hide_ignored_images
	}

	private final int DEFAULT_PHASH_WORKERS = 2;
	private final int DEFAULT_DATA_LOADERS = 1;
	private final int DATA_LOADER_PRIORITY = 2;

	private final int THUMBNAIL_DIMENSION = 500;
	private final int LOADER_OUT_QUEUE_SIZE = 400;
	private final boolean HIDE_IGNORED_IMAGES = true;

	private Settings() {
		super(new SettingsValidator());
	}

	public synchronized static Settings getInstance() {
		if (instance == null) {
			instance = new Settings();
		}

		return instance;
	}

	public Settings(ISettingsValidator validator) {
		super(validator);
	}

	public int getpHashWorkers() {
		return readAndConvertProperty(Parameters.phash_workers, DEFAULT_PHASH_WORKERS);
	}

	public int getDataLoaders() {
		return readAndConvertProperty(Parameters.data_loaders, DEFAULT_DATA_LOADERS);
	}

	public int getDataLoaderPriority() {
		return readAndConvertProperty(Parameters.data_loader_priority, DATA_LOADER_PRIORITY);
	}

	public int getThumbnailDimension() {
		return readAndConvertProperty(Parameters.thumbnail_dimension, THUMBNAIL_DIMENSION);
	}

	public int getLoaderOutQueueSize() {
		return readAndConvertProperty(Parameters.loader_out_queue_size, LOADER_OUT_QUEUE_SIZE);
	}

	public boolean getHideIgnoredImages() {
		return readAndConvertBoolean(Parameters.hide_ignored_images, HIDE_IGNORED_IMAGES);
	}

	public void setHideIgnoredImages(Boolean hideIgnored) {
		properties.setProperty(Parameters.hide_ignored_images.toString(), hideIgnored.toString());
	}

	private int readAndConvertProperty(Enum<?> parameter, int defaultValue) {
		String value = properties.getProperty(parameter.toString());
		int intValue = Convert.stringToInt(value, defaultValue);
		return intValue;
	}

	private boolean readAndConvertBoolean(Enum<?> parameter, Boolean defaultValue) {
		String value = properties.getProperty(parameter.toString(), defaultValue.toString());
		boolean boolValue = Convert.stringToBoolean(value, defaultValue);
		return boolValue;
	}
}