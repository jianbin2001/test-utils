package test;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;

import utils.http.HttpUtils;
import utils.test.StressBaseTest;

public class IPDetectTest extends StressBaseTest {

	public IPDetectTest(int poolSize, int queueSize) {
		super(poolSize, queueSize);
	}

	public static void main(String[] args) {
		String url = "https://www.baidu.com";
		int testNum = 100;
		int poolSize = 100;
		int queueSize = 20000;

		int pos = 0;
		if (args.length > pos) {
			url = StringUtils.trimToEmpty(args[pos]);
		}
		pos++;
		if (args.length > pos) {
			testNum = NumberUtils.toInt(args[pos], testNum);
		}
		pos++;
		if (args.length > pos) {
			poolSize = NumberUtils.toInt(args[pos], poolSize);
		}
		pos++;
		if (args.length > pos) {
			queueSize = NumberUtils.toInt(args[pos], queueSize);
		}
		pos++;

		IPDetectTest test = new IPDetectTest(poolSize, queueSize);
		long begin = System.currentTimeMillis();
		test.test(url, testNum);
		test.finish(begin);
	}

	public void test(final String url, int testNum) {
		for (int i = 1; i <= testNum; i++) {
			final int num = i;
			executor(new Runnable() {
				@Override
				public void run() {
					try {
						HttpUtils.doGet(url);
					} catch (Exception e) {
						logger.error("test error, loop={}", num, e);
					}
					if (num % 1000 == 0) {
						logger.info("loop={}", num);
					}
				}
			});
		}
	}
}
