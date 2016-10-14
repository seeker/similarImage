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

import java.util.LinkedList;
import java.util.List;

import org.apache.activemq.artemis.api.core.ActiveMQException;

import com.github.dozedoff.similarImage.db.repository.ImageRepository;
import com.github.dozedoff.similarImage.io.HashAttribute;
import com.github.dozedoff.similarImage.io.Statistics;
import com.github.dozedoff.similarImage.messaging.ArtemisSession;

public class HandlerListFactory {
	private final ImageRepository imageRepository;
	private final Statistics statistics;
	private final ArtemisSession session;

	public HandlerListFactory(ImageRepository imageRepository, Statistics statistics, ArtemisSession as) {
		this.imageRepository = imageRepository;
		this.statistics = statistics;
		this.session = as;
	}

	public List<HashHandler> withExtendedAttributeSupport(HashAttribute hashAttribute) throws ActiveMQException {
		List<HashHandler> handlers = new LinkedList<HashHandler>();

		handlers.add(new DatabaseHandler(imageRepository, statistics));
		handlers.add(new ExtendedAttributeHandler(hashAttribute, imageRepository));
		handlers.add(new ArtemisHashProducer(session.getSession(), ArtemisSession.ADDRESS_HASH_QUEUE));

		return handlers;
	}

	public List<HashHandler> noExtendedAttributeSupport(HashAttribute hashAttribute) throws ActiveMQException {
		List<HashHandler> handlers = new LinkedList<HashHandler>();

		handlers.add(new DatabaseHandler(imageRepository, statistics));
		handlers.add(new ArtemisHashProducer(session.getSession(), ArtemisSession.ADDRESS_HASH_QUEUE));

		return handlers;
	}
}
