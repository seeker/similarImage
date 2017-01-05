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
package com.github.dozedoff.similarImage.handler;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javax.inject.Inject;

import com.github.dozedoff.similarImage.db.repository.ImageRepository;
import com.github.dozedoff.similarImage.io.ExtendedAttribute;
import com.github.dozedoff.similarImage.io.ExtendedAttributeDirectoryCache;
import com.github.dozedoff.similarImage.io.ExtendedAttributeQuery;
import com.github.dozedoff.similarImage.io.HashAttribute;
import com.github.dozedoff.similarImage.io.Statistics;
import com.github.dozedoff.similarImage.messaging.ArtemisSession;
import com.github.dozedoff.similarImage.messaging.StorageNode;

public class HandlerListFactory {
	private final ImageRepository imageRepository;
	private final Statistics statistics;
	private final ArtemisSession session;
	private final ExtendedAttributeQuery eaQuery;

	@Inject
	public HandlerListFactory(ImageRepository imageRepository, Statistics statistics, ArtemisSession as, ExtendedAttributeQuery eaQuery) {
		this.imageRepository = imageRepository;
		this.statistics = statistics;
		this.session = as;
		this.eaQuery = eaQuery;
	}

	public List<HashHandler> withExtendedAttributeSupport(HashAttribute hashAttribute) throws Exception {
		List<HashHandler> handlers = new LinkedList<HashHandler>();

		handlers.add(new DatabaseHandler(imageRepository, statistics));
		handlers.add(new ExtendedAttributeHandler(hashAttribute, imageRepository, eaQuery));

		StorageNode sn = new StorageNode(session.getSession(), new ExtendedAttributeDirectoryCache(new ExtendedAttribute()),
				new HashAttribute(HashNames.DEFAULT_DCT_HASH_2), Collections.emptyList());

		try {
			handlers.add(new ArtemisHashProducer(sn));
		} catch (Exception e) {
			throw new RuntimeException("Failed to setup hash producer");
		}

		return handlers;
	}
}
