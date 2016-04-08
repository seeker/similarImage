package com.github.dozedoff.similarImage.thread;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.DirectoryStream.Filter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.concurrent.ExecutorService;

import org.junit.Before;
import org.junit.Test;

import com.github.dozedoff.commonj.hash.ImagePHash;
import com.github.dozedoff.similarImage.db.Persistence;
import com.github.dozedoff.similarImage.io.Statistics;

public class LoadJobVisitorTest {
	private static final int DEFAULT_TIMEOUT = 2000;

	@SuppressWarnings("unchecked")
	private Filter<Path> fileFilter = mock(Filter.class);

	private Persistence persistence = mock(Persistence.class);

	private ExecutorService threadPool = mock(ExecutorService.class);
	private ImagePHash imagePHash = mock(ImagePHash.class);
	private Statistics statistics = mock(Statistics.class);

	private LoadJobVisitor loadJobVisitor;

	private Path testPath;

	public void createLoadJobVisitor() throws Exception {
		when(fileFilter.accept(any(Path.class))).thenReturn(true);
		when(persistence.isBadFile(any(Path.class))).thenReturn(false);
		when(persistence.isPathRecorded(any(Path.class))).thenReturn(false);

		loadJobVisitor = new LoadJobVisitor(fileFilter, threadPool, persistence, imagePHash, statistics);
	}

	@Before
	public void setUp() throws Exception {
		createLoadJobVisitor();
		testPath = Paths.get("fooBar");
	}

	@Test
	public void testVisitFileValidUnknown() throws Exception {
		loadJobVisitor.visitFile(testPath, null);

		verify(threadPool, timeout(DEFAULT_TIMEOUT)).execute(any(ImageHashJob.class));
	}

	@Test
	public void testVisitFileInvalid() throws Exception {
		when(fileFilter.accept(any(Path.class))).thenReturn(false);

		loadJobVisitor.visitFile(testPath, null);

		verify(threadPool, never()).execute(any(ImageHashJob.class));
	}

	@Test
	public void testVisitFileKnownBad() throws Exception {
		when(persistence.isBadFile(any(Path.class))).thenReturn(true);

		loadJobVisitor.visitFile(testPath, null);

		verify(threadPool, never()).execute(any(ImageHashJob.class));
	}

	@Test
	public void testVisitFileKnownHash() throws Exception {
		when(persistence.isPathRecorded(any(Path.class))).thenReturn(true);

		loadJobVisitor.visitFile(testPath, null);

		verify(threadPool, never()).execute(any(ImageHashJob.class));
	}

	@Test
	public void testVisitFileDBerror() throws Exception {
		when(persistence.isPathRecorded(any(Path.class))).thenThrow(new SQLException("Testing"));

		loadJobVisitor.visitFile(testPath, null);

		verify(threadPool, never()).execute(any(ImageHashJob.class));
	}
}
