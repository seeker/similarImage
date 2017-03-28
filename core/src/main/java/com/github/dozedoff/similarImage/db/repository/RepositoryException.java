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
package com.github.dozedoff.similarImage.db.repository;

/**
 * Thrown if a repository encounters an error.
 * 
 * @author Nicholas Wright
 */
public class RepositoryException extends Exception {
	private static final long serialVersionUID = -8439120978919077820L;
	private final String message;
	private final Throwable cause;

	/**
	 * Create a new Exception with a message and the original cause.
	 * 
	 * @param message
	 *            describing this exception
	 * @param cause
	 *            that caused this exception to be thrown
	 */
	public RepositoryException(String message, Throwable cause) {
		this.message = message;
		this.cause = cause;
	}

	/**
	 * Create a new Exception with only a message, the cause is null.
	 * 
	 * @param message
	 *            describing this exception
	 */
	public RepositoryException(String message) {
		this.message = message;
		this.cause = null;
	}

	/**
	 * Get the message of this exception.
	 * 
	 * @return the message
	 */
	public final String getMessage() {
		return message;
	}

	/**
	 * Get the cause of this exception.
	 * 
	 * @return the original exception, if any or null
	 */
	public final Throwable getCause() {
		return cause;
	}
}
