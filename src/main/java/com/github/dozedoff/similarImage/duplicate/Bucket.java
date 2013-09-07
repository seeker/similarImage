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
package com.github.dozedoff.similarImage.duplicate;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

public class Bucket<I, T> {
	private I id;
	private LinkedList<T> bucket = new LinkedList<T>();

	public Bucket(I id) {
		this.id = id;
	}

	public Bucket(I id, T entry) {
		this.id = id;
		bucket.add(entry);
	}

	public Bucket(I id, Collection<T> collection) {
		this.id = id;
		bucket.addAll(collection);
	}

	public I getId() {
		return id;
	}

	public List<T> getBucket() {
		return (List<T>) bucket;
	}

	public void add(T item) {
		bucket.add(item);
	}
}
