package com.github.dozedoff.similarImage.benchmark;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aparapi.device.Device;
import com.aparapi.internal.kernel.KernelManager;
import com.aparapi.internal.kernel.KernelManagers;
import com.github.dozedoff.similarImage.image.kernel.DCTKernel;
import com.google.common.base.Stopwatch;

public class DCTKernelBenchmark {
	private static final Logger LOGGER = LoggerFactory.getLogger(DCTKernelBenchmark.class);

	private static final int SAMPLES = 200;
	private static final int MATRIX_SIZE = 8;
	private static final int BENCHMARK_ITERATIONS = 4;

	private static double[][] samples;
	private double[][] results;

	private static DCTKernel kernel;
	private Stopwatch sw;

	private Device device;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		createSamples();
		kernel = new DCTKernel();
	}

	@Before
	public void setUp() throws Exception {
		results = new double[SAMPLES][];
		device = null;
		sw = Stopwatch.createStarted();
	}

	@After
	public void tearDown() {
		LOGGER.info(sw.toString());
	}

	private static void createSamples() {
		samples = new double[SAMPLES][];

		for (int i = 0; i < SAMPLES; i++) {
			samples[i] = generateRandomMatrix(MATRIX_SIZE);
		}
	}

	private static double[] generateRandomMatrix(int matrix_size) {
		int matrix_area = matrix_size * matrix_size;
		double[] matrix = new double[matrix_area];
		
		for(int i=0; i<matrix_area; i++) {
			matrix[i] = Math.random();
		}
		
		return matrix;
	}

	private void computeMatrix() {
		for (int i = 0; i < SAMPLES; i++) {
			results[i] = kernel.transformDCT(samples[i]);
		}
	}

	private void runBenchmark() {
		for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
			Stopwatch sw = Stopwatch.createStarted();

			computeMatrix();

			sw.stop();
			LOGGER.info("Interation: {}, time: {}", i + 1, sw);
		}
	}

	@Test
	public void cpuOpenCl() {
		device = KernelManager.instance().bestDevice();
		LOGGER.info("OpenCL CPU: {}", device);
		kernel.setDevice(device);

		runBenchmark();
	}

	@Test
	public void jtp() {
		device = KernelManagers.JTP_ONLY.bestDevice();
		LOGGER.info("JTP: {}", device);
		kernel.setDevice(device);

		runBenchmark();
	}
}
