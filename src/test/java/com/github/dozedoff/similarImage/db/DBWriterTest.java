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
package com.github.dozedoff.similarImage.db;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.sql.SQLException;
import java.util.LinkedList;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class DBWriterTest {
	private Persistence persistence;
	private DBWriter dBWriter;

	private final int oneSize = 10;
	private final int twoSize = 30;

	private final long SLEEP_TIME = 100L;

	private LinkedList<ImageRecord> one, two;

	@Before
	public void setUp() throws Exception {
		persistence = mock(Persistence.class);
		dBWriter = new DBWriter(persistence);

		createImageLists();
	}

	private void createImageLists() {
		one = new LinkedList<>();
		two = new LinkedList<>();

		for (int i = 0; i < oneSize; i++) {
			one.add(new ImageRecord("one" + i, i));
		}

		for (int i = 0; i < twoSize; i++) {
			two.add(new ImageRecord("two" + i, 1000 + i));
		}
	}

	@Test
	public void testAddVerifyOne() throws Exception {
		dBWriter.add(one);
		dBWriter.add(two);

		Thread.sleep(SLEEP_TIME);

		verify(persistence).batchAddRecord(one);
	}

	@Test
	public void testAddVerifyTwo() throws Exception {
		dBWriter.add(one);
		dBWriter.add(two);

		Thread.sleep(SLEEP_TIME);

		verify(persistence).batchAddRecord(two);
	}

	@Test
	public void testAddFailOnFirstAttemptVerifyOne() throws Exception {
		Mockito.doThrow(new SQLException("Simulating db error")).doNothing().when(persistence).batchAddRecord(one);

		dBWriter.add(one);
		dBWriter.add(two);

		Thread.sleep(SLEEP_TIME);

		verify(persistence, times(2)).batchAddRecord(one);
	}

	@Test
	public void testAddFailOnFirstAttemptVerifyTwo() throws Exception {
		Mockito.doThrow(new SQLException("Simulating db error")).doNothing().when(persistence).batchAddRecord(one);

		dBWriter.add(one);
		dBWriter.add(two);

		Thread.sleep(SLEEP_TIME);

		verify(persistence).batchAddRecord(two);
	}

	@Test
	public void testAddFailOnFirstTwoAttemptsVerifyOne() throws Exception {
		Mockito.doThrow(new SQLException("Simulating db error")).doThrow(new SQLException("Simulating db error")).doNothing()
				.when(persistence).batchAddRecord(one);

		dBWriter.add(one);
		dBWriter.add(two);

		Thread.sleep(SLEEP_TIME);

		verify(persistence, times(3)).batchAddRecord(one);
	}

	@Test
	public void testAddFailOnFirstTwoAttemptsVerifyTwo() throws Exception {
		Mockito.doThrow(new SQLException("Simulating db error")).doThrow(new SQLException("Simulating db error")).doNothing()
				.when(persistence).batchAddRecord(one);

		dBWriter.add(one);
		dBWriter.add(two);

		Thread.sleep(SLEEP_TIME);

		verify(persistence).batchAddRecord(two);
	}

	@Test
	public void testAddGiveUpVerifyOne() throws Exception {
		Mockito.doThrow(new SQLException("Simulating db error")).when(persistence).batchAddRecord(one);

		dBWriter.add(one);
		dBWriter.add(two);

		Thread.sleep(SLEEP_TIME);

		verify(persistence, times(4)).batchAddRecord(one);
	}

	@Test
	public void testAddGiveUpVerifyTwo() throws Exception {
		Mockito.doThrow(new SQLException("Simulating db error")).doThrow(new SQLException("Simulating db error")).doNothing()
				.when(persistence).batchAddRecord(one);

		dBWriter.add(one);
		dBWriter.add(two);

		Thread.sleep(SLEEP_TIME);

		verify(persistence).batchAddRecord(two);
	}
}
