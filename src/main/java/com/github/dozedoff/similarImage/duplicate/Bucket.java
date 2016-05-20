/*  Copyright (C) 2014  Nicholas Wright
    
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
package com.github.dozedoff.similarImage.duplicate;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

@Deprecated
public final class Bucket<I, T> {
	private final I id;
	private final LinkedList<T> bucket = new LinkedList<T>();

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

	public int getSize() {
		return bucket.size();
	}

	public boolean isEmpty() {
		return bucket.isEmpty();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Bucket other = (Bucket) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		return true;
	}
}
