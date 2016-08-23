package utils.test;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor.DiscardPolicy;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StressBaseTest {
	protected Logger logger = LoggerFactory.getLogger(getClass());
	private ThreadPoolExecutor executor = null;
	private int queueSize = 0;
	private AtomicLong testNum = new AtomicLong();

	public StressBaseTest(int poolSize, int queueSize) {
		this.queueSize = queueSize;
		executor = new ThreadPoolExecutor(poolSize, poolSize, 1, TimeUnit.DAYS, new LinkedBlockingQueue<Runnable>(), new ThreadFactory() {
			private AtomicInteger num = new AtomicInteger();

			@Override
			public Thread newThread(Runnable r) {
				Thread t = new Thread(r);
				t.setName("test-" + num.incrementAndGet());
				t.setDaemon(true);
				t.setPriority(Thread.NORM_PRIORITY);

				return t;
			}
		}, new DiscardPolicy());
	}

	public void executor(Runnable r) {
		while (executor.getQueue().size() > queueSize) {
			try {
				Thread.sleep(10);
			} catch (Exception e) {
			}
		}

		testNum.incrementAndGet();
		executor.execute(r);
	}

	public void finish(long begin) {
		try {
			executor.shutdown();
			executor.awaitTermination(1, TimeUnit.DAYS);
		} catch (Exception e) {
		}

		long cost = System.currentTimeMillis() - begin;
		long tps = testNum.get() * 1000 / cost;

		logger.info("测试结束, 总数{}, 耗时{}ms, {}tps", testNum.get(), cost, tps);
	}
}
