package test.filter.impl;

import test.filter.api.Filter;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicStampedReference;

public class RateFilter implements Filter {

    private final AtomicStampedReference<Long> lastAcceptRef;
    private final int maxCapacity;
    private final double refillRate;

    public RateFilter(int ratePerUnit, TimeUnit timeUnit) {
        this.maxCapacity = ratePerUnit;
        refillRate = ratePerUnit / (double) timeUnit.toNanos(1);
        lastAcceptRef = new AtomicStampedReference<Long>(0L, Float.floatToIntBits(this.maxCapacity));
    }

    @Override
    public boolean isSignalAllowed() {
        boolean compareAndSetHappen;
        long elapsedTime;
        double allowance;
        do {
            long now = System.nanoTime();
            Long lastAcceptTime = lastAcceptRef.getReference();
            int prevStamp = lastAcceptRef.getStamp();
            elapsedTime = now - lastAcceptTime;
            allowance = Float.intBitsToFloat(prevStamp);
            allowance += elapsedTime * refillRate;
            allowance = Math.min(allowance, maxCapacity);
            if (allowance < 1.0) {
                System.out.println("Discard, elapsed time: " + elapsedTime / 1000000 + " allowance: " + allowance);
                return false;
            }
            int newStamp = Float.floatToIntBits((float) (allowance - 1.0));
            compareAndSetHappen = lastAcceptRef.compareAndSet(lastAcceptTime, now, prevStamp, newStamp);
        } while (!compareAndSetHappen);
        System.out.println("Allow, elapsed time: " + elapsedTime / 1000000 + " allowance: " + allowance);
        return true;
    }
}
