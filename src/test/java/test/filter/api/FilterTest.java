package test.filter.api;

import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * This is a small app that demonstrates how a {@link Filter} can be used.
 *
 * If you want to score some extra points you can implement JUnit tests for your implementation.
 */
public class FilterTest {
    private static final int numberOfSignalsPerProducer = 100;
    private static final int numberOfSignalsProducers = 3;

    private static class RandomFilter implements Filter {
        private final int limit;
        private int counter;
        private long time;
        private Lock lock;

        /** @param N maximum number of signals per last 100 seconds */
        private RandomFilter (int N) {
            this.limit = N;
            this.counter = 0;
            this.time = System.currentTimeMillis();
            this.lock = new ReentrantLock();
        }

        @Override
        public boolean isSignalAllowed() {
            long elapsedTime = System.currentTimeMillis() - time;
            if (elapsedTime > 1000) {
                lock.lock();
                if (System.currentTimeMillis() - time > 1000) {
                    counter = 1;
                    time = System.currentTimeMillis();
                    lock.unlock();
                    return true;
                } else if (counter < limit) {
                    counter++;
                    lock.unlock();
                    return true;
                } else {
                    lock.unlock();
                    return false;
                }
            } else if (counter < limit) {
                lock.lock();
                if (counter < limit) {
                    counter++;
                    lock.unlock();
                    return true;
                } else {
                    lock.unlock();
                    return false;
                }
            } else {
                return false;
            }
        }
    }

    private static class TestProducer extends Thread {
        private final Filter filter;
        private final AtomicInteger totalPassed;

        private TestProducer(Filter filter, AtomicInteger totalPassed) {
            this.filter = filter;
            this.totalPassed = totalPassed;
        }

        @Override
        public void run() {
            Random rnd = new Random ();
            try {
                for (int j = 0; j < numberOfSignalsPerProducer; j++) {
                    if (filter.isSignalAllowed())
                        totalPassed.incrementAndGet();
                    Thread.sleep(rnd.nextInt(100));
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main (String ... args) throws InterruptedException {
        final int N = 100;
        Filter filter = new RandomFilter(N);

        AtomicInteger totalPassed = new AtomicInteger();
        Thread [] producers = new Thread[numberOfSignalsProducers];
        for (int i=0; i < producers.length; i++)
            producers[i] = new TestProducer(filter, totalPassed);

        for (Thread producer : producers)
            producer.start();

        for (Thread producer : producers)
            producer.join();

        System.out.println("Filter allowed " + totalPassed + " signals out of " + (numberOfSignalsPerProducer * numberOfSignalsProducers));
    }

}
