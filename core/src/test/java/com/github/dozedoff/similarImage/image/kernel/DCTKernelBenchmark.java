package com.github.dozedoff.similarImage.image.kernel;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.CoreMatchers.not;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.aparapi.device.Device;
import com.aparapi.device.JavaDevice;
import com.aparapi.device.OpenCLDevice;
import com.aparapi.device.OpenCLDevice.DeviceSelector;
import com.aparapi.internal.kernel.KernelManager;
import com.aparapi.internal.kernel.KernelManagers;
import com.github.dozedoff.commonj.time.StopWatch;
import com.google.common.base.Stopwatch;

public class DCTKernelBenchmark {
	private static final int SAMPLE_SIZE = 1000;
	private static double[][] samples;
	private static double[][] resultCpu;
	private static double[][] resultJtp;
	private static double[][] resultGpu;
	
	private static DCTKernel kernel;
	
	
	private static double[] randomMatrixGen() {
		int size = 64;
		double[] m = new double[size];
		
		for (int i=0; i < size; i++) {
			m[i] = Math.random();
		}
		
		return m;
	}
	
	@BeforeClass
	public static void setUpClass() {
		samples = new double[SAMPLE_SIZE][];

		resultCpu = new double[SAMPLE_SIZE][];
		resultJtp = new double[SAMPLE_SIZE][];
		resultGpu = new double[SAMPLE_SIZE][];
		
		for (int i = 0; i < SAMPLE_SIZE; i++) {
			samples[i] = randomMatrixGen();
		}
		
		kernel = new DCTKernel();
	}
	
	private void runBenchMark(Device device, double result[][]) {
		kernel.setDevice(device);
		System.out.println("Starting benchmark using device: "+device.getType());
		Stopwatch sw = Stopwatch.createStarted();
		
		for (int i = 0; i < SAMPLE_SIZE; i++) {
			result[i] = kernel.transformDCT(samples[i]);
		}
		
		sw.stop();
		
		System.out.println("Time for " + device.getType() + ":" + sw.toString());
	}
	
	@Test
	public void benchmark() {
		List<OpenCLDevice> gpuDevices = OpenCLDevice.listDevices(Device.TYPE.GPU);
		List<OpenCLDevice> cpuDevices = OpenCLDevice.listDevices(Device.TYPE.CPU);
		
		Device gpu = null;
		Device cpu = null;
		
		for(OpenCLDevice d : gpuDevices) {
			String deviceName = d.getOpenCLPlatform().getVendor().toLowerCase();
			
			if(deviceName.contains("nvidia") || deviceName.contains("amd")) {
				gpu = d;
				break;
			}
		}
		
		for(OpenCLDevice d : cpuDevices) {
			String deviceName = d.getOpenCLPlatform().getVendor().toLowerCase();
			
			if(deviceName.contains("intel") || deviceName.contains("amd") || deviceName.contains("pocl")) {
				cpu = d;
				break;
			}
		}
		
		assertThat(gpu, is(notNullValue()));
		assertThat(cpu, is(notNullValue()));
		
		System.out.println("Running JTP benchmark");
		runBenchMark(JavaDevice.THREAD_POOL, resultJtp);
		System.out.println("Running GPU benchmark");
		runBenchMark(gpu, resultGpu);
		System.out.println("Running CPU benchmark");
		runBenchMark(cpu, resultCpu);
	}
}
