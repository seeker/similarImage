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

import java.awt.image.BufferedImage;
import java.nio.file.Paths;

import org.imgscalr.Scalr;
import org.imgscalr.Scalr.Method;

import com.github.dozedoff.similarImage.result.Result;
import com.github.dozedoff.similarImage.util.ImageUtil;
import com.google.common.cache.CacheLoader;

/**
 * Thumbnail loader for the thumbnail cache.
 * 
 * @author Nicholas Wright
 *
 */
public class ThumbnailCacheLoader extends CacheLoader<Result, BufferedImage> {
	private static final int THUMBNAIL_SIZE = 500;

	/**
	 * Loads a thumbnail for the {@link Result}.
	 * 
	 * @param key
	 *            the result for which the thumbnail should be loaded.
	 * @return the loaded thumbnail for the given {@link Result}
	 * @throws Exception
	 *             if the thumbnail loading failed
	 */

	@Override
	public BufferedImage load(Result key) throws Exception {
		BufferedImage bi = ImageUtil.loadImage(Paths.get(key.getImageRecord().getPath()));
		return Scalr.resize(bi, Method.AUTOMATIC, THUMBNAIL_SIZE);
	}
}
