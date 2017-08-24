package org.scidb.modis.util;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ThreadUtil {

    public static ThreadPoolExecutor getStandardThreadPool(int capacity) {
        int numProcessors = Runtime.getRuntime().availableProcessors();
        BlockingQueue<Runnable> workQueue = new ArrayBlockingQueue<Runnable>(capacity);
        return new ThreadPoolExecutor(numProcessors, numProcessors, 1, TimeUnit.MINUTES, workQueue);
    }

    public static ThreadPoolExecutor getStandardThreadPool(int threads, int capacity) {
        BlockingQueue<Runnable> workQueue = new ArrayBlockingQueue<Runnable>(capacity);
        return new ThreadPoolExecutor(threads, threads, 1, TimeUnit.MINUTES, workQueue);
    }

    public static ThreadPoolExecutor getStandardThreadPool(int coreThreads, int maxThreads, int capacity) {
        BlockingQueue<Runnable> workQueue = new ArrayBlockingQueue<Runnable>(capacity);
        return new ThreadPoolExecutor(coreThreads, maxThreads, 1, TimeUnit.MINUTES, workQueue);
    }
}
