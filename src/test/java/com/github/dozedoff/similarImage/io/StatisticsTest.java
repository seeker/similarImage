package com.github.dozedoff.similarImage.io;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

import org.junit.Before;
import org.junit.Test;

import com.github.dozedoff.similarImage.io.Statistics.StatisticsEvent;

public class StatisticsTest {

	private StatisticsChangedListener listener = mock(StatisticsChangedListener.class);
	private Statistics cut;

	@Before
	public void setUp() throws Exception {
		cut = new Statistics();
		cut.addStatisticsListener(listener);
	}

	@Test
	public void testIncrementFailedFilesCounter() throws Exception {
		cut.incrementFailedFiles();

		assertThat(cut.getFailedFiles(), is(1));
	}

	@Test
	public void testIncrementFailedFilesEvent() throws Exception {
		cut.incrementFailedFiles();

		verify(listener).statisticsChangedEvent(eq(StatisticsEvent.FAILED_FILES), eq(1));
	}

	@Test
	public void testIncrementFoundFilesCounter() throws Exception {
		cut.incrementFoundFiles();
		
		assertThat(cut.getFoundFiles(), is(1));
	}

	@Test
	public void testIncrementFoundFilesEvent() throws Exception {
		cut.incrementFoundFiles();

		verify(listener).statisticsChangedEvent(eq(StatisticsEvent.FOUND_FILES), eq(1));
	}

	@Test
	public void testIncrementProcessedFilesCounter() throws Exception {
		cut.incrementProcessedFiles();

		assertThat(cut.getProcessedFiles(), is(1));
	}

	@Test
	public void testIncrementProcessedFilesFiles() throws Exception {
		cut.incrementProcessedFiles();

		verify(listener).statisticsChangedEvent(eq(StatisticsEvent.PROCESSED_FILES), eq(1));
	}

	@Test
	public void testIncrementSkippedFilesCounter() throws Exception {
		cut.incrementSkippedFiles();

		assertThat(cut.getSkippedFiles(), is(1));
	}

	@Test
	public void testIncrementSkippedFilesEvent() throws Exception {
		cut.incrementSkippedFiles();

		verify(listener).statisticsChangedEvent(eq(StatisticsEvent.SKIPPED_FILES), eq(1));
	}

	@Test
	public void testResetFailed() throws Exception {
		cut.incrementFailedFiles();
		assertThat(cut.getFailedFiles(), is(1)); // Guard

		cut.reset();

		assertThat(cut.getFailedFiles(), is(0));
	}

	@Test
	public void testResetFound() throws Exception {
		cut.incrementFoundFiles();
		assertThat(cut.getFoundFiles(), is(1)); // Guard

		cut.reset();

		assertThat(cut.getFoundFiles(), is(0));
	}

	@Test
	public void testResetProcessed() throws Exception {
		cut.incrementProcessedFiles();
		assertThat(cut.getProcessedFiles(), is(1)); // Guard

		cut.reset();

		assertThat(cut.getProcessedFiles(), is(0));
	}

	@Test
	public void testResetSkipped() throws Exception {
		cut.incrementSkippedFiles();
		assertThat(cut.getSkippedFiles(), is(1)); // Guard

		cut.reset();
		assertThat(cut.getSkippedFiles(), is(0));
	}

	@Test
	public void testRemoveStatisticsListener() throws Exception {
		cut.removeStatisticsListener(listener);
		
		cut.incrementFailedFiles();
		cut.incrementFoundFiles();
		cut.incrementProcessedFiles();
		cut.incrementSkippedFiles();

		verifyZeroInteractions(listener);
	}
}
