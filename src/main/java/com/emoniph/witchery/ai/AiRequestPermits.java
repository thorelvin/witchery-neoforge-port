package com.emoniph.witchery.ai;

import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

final class AiRequestPermits {
    private long nextGeneration = 1L;
    private Pool current = new Pool(nextGeneration, 1);

    synchronized Permit tryAcquire(int requestedLimit) {
        int limit = Math.max(1, requestedLimit);
        if (current.limit != limit) {
            current = new Pool(++nextGeneration, limit);
        }
        if (!current.semaphore.tryAcquire()) {
            return null;
        }
        current.inFlight.incrementAndGet();
        return new Permit(current);
    }

    synchronized void invalidate(int requestedLimit) {
        current = new Pool(++nextGeneration, Math.max(1, requestedLimit));
    }

    synchronized long generation() {
        return current.generation;
    }

    synchronized boolean isCurrent(long generation) {
        return current.generation == generation;
    }

    synchronized int inFlight() {
        return current.inFlight.get();
    }

    private static final class Pool {
        private final long generation;
        private final int limit;
        private final Semaphore semaphore;
        private final AtomicInteger inFlight = new AtomicInteger();

        private Pool(long generation, int limit) {
            this.generation = generation;
            this.limit = limit;
            this.semaphore = new Semaphore(limit, true);
        }
    }

    static final class Permit implements AutoCloseable {
        private final Pool pool;
        private final AtomicBoolean closed = new AtomicBoolean();

        private Permit(Pool pool) {
            this.pool = pool;
        }

        long generation() {
            return pool.generation;
        }

        @Override
        public void close() {
            if (closed.compareAndSet(false, true)) {
                pool.inFlight.decrementAndGet();
                pool.semaphore.release();
            }
        }
    }
}
