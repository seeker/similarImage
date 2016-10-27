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

import java.nio.file.Path;

import com.github.dozedoff.similarImage.messaging.MessageFactory;
import com.github.dozedoff.similarImage.messaging.MessageFactory.MessageProperty;
import com.github.dozedoff.similarImage.messaging.MessageFactory.TaskType;
import com.github.dozedoff.similarImage.messaging.StorageNode;

/**
 * Creates Messages from the files and sends them to the queue.
 * 
 * @author Nicholas Wright
 *
 */
public class ArtemisHashProducer implements HashHandler {
	/**
	 * @deprecated Use {@link MessageProperty}
	 */
	@Deprecated
	public static final String MESSAGE_TASK_PROPERTY = "task";
	/**
	 * @deprecated Use {@link MessageProperty}
	 */
	@Deprecated
	public static final String MESSAGE_PATH_PROPERTY = "path";
	/**
	 * @deprecated Use {@link MessageProperty}
	 */
	@Deprecated
	public static final String MESSAGE_HASH_PROPERTY = MessageFactory.HASH_PROPERTY_NAME;
	/**
	 * @deprecated Use {@link TaskType}
	 */
	@Deprecated
	public static final String MESSAGE_TASK_VALUE_HASH = "hash";
	/**
	 * @deprecated Use {@link TaskType}
	 */
	@Deprecated
	public static final String MESSAGE_TASK_VALUE_CORRUPT = "corr";

	private final StorageNode storageNode;

	/**
	 * Create a new handler using the given {@link StorageNode}.
	 * 
	 * @param storageNode
	 *            used for filesystem access
	 * 
	 * @throws Exception
	 *             if pending image query failed
	 */
	public ArtemisHashProducer(StorageNode storageNode) throws Exception {
		this.storageNode = storageNode;
	}

	/**
	 * Read the file, create a message and send it.
	 * 
	 * @param file
	 *            to read and send
	 * @return true if the file was read and sent successfully
	 */
	@Override
	public boolean handle(Path file) {
		return storageNode.processFile(file);
	}
}
