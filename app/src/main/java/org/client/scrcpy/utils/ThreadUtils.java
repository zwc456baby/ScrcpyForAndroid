package org.client.scrcpy.utils;

import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.os.SystemClock;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by zhouwenchao on 2017-04-27.
 */
public final class ThreadUtils {
    private static final ExecutorService threadCache = Executors.newCachedThreadPool();
    //    private static final ScheduledExecutorService timerTask = Executors.newSingleThreadScheduledExecutor();
    private static final Handler mHandler = new Handler(Looper.getMainLooper());
    private static final List<RunnableIm> RUNNABLE_LIST = new ArrayList<>();
    private static final Object listLock = new Object();
    private static boolean timerExit = true;
    private static boolean RUNNING = true;
    // 后台线程（注意：不要死锁了，否则导致其它页面卡死）
    private static volatile Handler asyncHandler;

    static {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                if (!threadCache.isShutdown()) {
                    threadCache.shutdown();
                }
            }
        });
        Thread mainThread = new Thread(() -> {
            Looper.prepare();
            asyncHandler = new Handler(Looper.myLooper());
            // 线程优先级
            Process.setThreadPriority(Process.THREAD_PRIORITY_MORE_FAVORABLE);
            while (RUNNING) {
                try {
                    Looper.loop();
                } catch (Exception e) {
                }
            }
            asyncHandler = null;
        });
        mainThread.start();
    }

    /**
     * 异步执行一个任务, 所有任务都在同一个线程中执行
     * <p>
     * 优点：为了进行程序优化，许多任务都采用异步执行
     * 但是异步执行时，不同的线程可能导致数据、状态等不同
     * 如果采用单个线程，将能够有效避免这些问题
     * <p>
     * 经过实际测试，采用协程开启任务和采用异步 Handler 的方式
     * Handler 更有优势，如果是单个任务，建议采用 Handler
     * <p>
     * GlobalScop 开启一个协程约 1ms ，Handler post 任务耗时<0.1ms
     *
     * @param r 任务对象
     */
    public static void workPost(Runnable r) {
        //noinspection StatementWithEmptyBody
        while (asyncHandler == null) {  // 等待初始化
        }
        asyncHandler.post(r);
    }

    public static void workPostDelay(Runnable r, long time) {
        //noinspection StatementWithEmptyBody
        while (asyncHandler == null) {  // 等待初始化
        }
        asyncHandler.postDelayed(r, time);
    }

    public static void removeWork(Runnable r) {
        //noinspection StatementWithEmptyBody
        while (asyncHandler == null) {  // 等待初始化
        }
        asyncHandler.removeCallbacks(r);
    }

    public static void postDelayed(Runnable r, long time) {
        mHandler.postDelayed(r, time);
    }

    public static void post(Runnable r) {  //主线程
        mHandler.post(r);
    }

    public static void removeCallbacks(Runnable r) {
        mHandler.removeCallbacks(r);
    }

    public static void sleep(long time) {
        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private ThreadUtils() {
    }

    public static void execute(Runnable runnable) {
        if (runnable != null) {
            threadCache.execute(runnable);
        }
    }

    public static void executeDelayed(Runnable runnable, long time) {
        if (time <= 0) {
            execute(runnable);
            return;
        }
        RunnableIm im = RunnableIm.obtain(time, runnable, SystemClock.elapsedRealtime());
        synchronized (listLock) {
            RUNNABLE_LIST.add(im);
            if (timerExit) {
                execute(TIMER_RUNNABLE);
                timerExit = false;
            } else {
                try {
                    listLock.notifyAll();
                } catch (Exception ignored) {
                }
            }
        }
    }

    public static void removeExecute(Runnable runnable) {
        if (runnable == null) {
            return;
        }
        synchronized (listLock) {
            for (int index = 0; index < RUNNABLE_LIST.size(); index++) {
                RunnableIm imTmp = RUNNABLE_LIST.get(index);
                if (runnable.equals(imTmp.getRunnable())) {
                    RUNNABLE_LIST.remove(index);
                    imTmp.recycle();
                    index--;  //移除后下标减 1
                }
            }
        }
    }

    public static boolean hasRunnable(Runnable runnable) {
        if (runnable == null) {
            return false;
        }
        synchronized (listLock) {
            for (int index = 0; index < RUNNABLE_LIST.size(); index++) {
                RunnableIm imTmp = RUNNABLE_LIST.get(index);
                if (runnable.equals(imTmp.getRunnable())) {
                    return true;
                }
            }
        }
        return false;
    }

    private static final TimerRunnable TIMER_RUNNABLE = new TimerRunnable();

    /**
     * 用来运行单个任务的线程
     * 注意不要阻塞此线程的执行，否则可能导致卡死等问题
     */
    public static final class SignelThread extends Thread {

        // 后台线程（注意：不要死锁了，否则导致其它页面卡死）
        private final Object WORK_LOCK = new Object();
        private RunnableIm workLink;
        private boolean exit = false;

        @Override
        public void run() {
            while (!exit) {
                Runnable runnable = null;
                synchronized (WORK_LOCK) {
                    RunnableIm runnableIm = workLink;
                    // 如果队列为空，则一直等待，直到唤醒
                    if (runnableIm == null) {
                        try {
                            WORK_LOCK.wait();
                        } catch (InterruptedException ignore) {
                        }
                        continue;
                    }
                    long waitTime = runnableIm.getTime() - (System.currentTimeMillis() - runnableIm.getSysTime());
                    if (waitTime <= 0) {
                        runnable = runnableIm.runnable;
                        workLink = runnableIm.nextRunnableIm;
                        runnableIm.recycle();
                    } else {
                        try {
                            WORK_LOCK.wait(waitTime);
                        } catch (InterruptedException ignore) {
                        }
                    }
                }
                // 如果放到同步锁的外面，执行速度会慢一些
                // 因为会被同步锁阻塞到 add 和 remove
                if (runnable != null) {
                    try { // 执行线程方法
                        runnable.run();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            synchronized (WORK_LOCK) {
                workLink = null;
            }
        }

        public void exitThread() {
            exit = true;
            synchronized (WORK_LOCK) {
                WORK_LOCK.notify();
            }
        }

        public void workPost(Runnable runnable) {
            if (exit) return;
            if (runnable != null) {
                workPostDelay(runnable, 0);
            }
        }

        public void workPostDelay(Runnable runnable, long delay) {
            if (exit) return;
            if (runnable == null) {
                return;
            }
            synchronized (WORK_LOCK) {
                long currentSystemTime = System.currentTimeMillis();
                RunnableIm runnableIm = RunnableIm.obtain(delay, runnable, currentSystemTime);
                // 添加到队列中
                if (workLink == null) {
                    workLink = runnableIm;
                    // 改变了队列头部需要重新释放锁
                    WORK_LOCK.notify();
                    return;
                }
                long curWaitTime = runnableIm.time;
                RunnableIm linkRunnableIm = workLink;
                long curLinkWaitTime = linkRunnableIm.getTime() - (currentSystemTime - linkRunnableIm.getSysTime());
                // 如果队列头部的执行时间，比需要添加的线程执行要快
                // 则循环查找到队列的末尾，插入到队列中部
                // 如果新加入的执行速度更快，则直接添加到头部
                if (curLinkWaitTime <= curWaitTime) {
                    while (linkRunnableIm.nextRunnableIm != null) {
                        RunnableIm tmpIm = linkRunnableIm.nextRunnableIm;
                        curLinkWaitTime = tmpIm.getTime() - (currentSystemTime - tmpIm.getSysTime());
                        // 查找链表下一个，如果下一个节点的执行时机更晚，则跳出链表查找
                        // 然后将新的插入到当前节点
                        if (curLinkWaitTime > curWaitTime) {
                            break;
                        } else {
                            linkRunnableIm = tmpIm;
                        }
                    }
                    RunnableIm tmpRunnableIm = linkRunnableIm.nextRunnableIm;
                    // 插入到下一个队列中
                    linkRunnableIm.nextRunnableIm = runnableIm;
                    runnableIm.nextRunnableIm = tmpRunnableIm;
                } else {
                    workLink = runnableIm;
                    runnableIm.nextRunnableIm = linkRunnableIm;
                    // 改变了队列头部需要重新释放锁
                    // 其他时刻，也就是插入到队列中间无需释放锁
                    if (linkRunnableIm != workLink) {
                        WORK_LOCK.notify();
                    }
                }
            }
        }

        public boolean removeWork(Runnable runnable) {
            if (exit) return false;
            if (runnable == null) {
                return false;
            }
            synchronized (WORK_LOCK) {
                if (workLink == null) {
                    return false;
                }
                RunnableIm linkRunnableIm = workLink;
                if (linkRunnableIm.runnable == runnable) {
                    // 指向下一个
                    workLink = linkRunnableIm.nextRunnableIm;
                    // 改变了队列头部需要重新释放锁
                    WORK_LOCK.notify();
                    return true;
                }
                while (linkRunnableIm.nextRunnableIm != null) {
                    RunnableIm tmpRunableIm = linkRunnableIm.nextRunnableIm;
                    // 从队列中移除掉
                    if (tmpRunableIm.runnable == runnable) {
                        linkRunnableIm.nextRunnableIm = tmpRunableIm.nextRunnableIm;
                        return true;
                    } else {
                        linkRunnableIm = tmpRunableIm;
                    }
                }
                return false;
            }
        }
    }

    /**
     * 使用该线程实现定时器功能
     */
    private static final class TimerRunnable implements Runnable {

        @Override
        public void run() {
            boolean waitQuere = false;
            while (!threadCache.isShutdown()) {
                synchronized (listLock) {
                    if (RUNNABLE_LIST.size() == 0) {
                        //等待 60s，如果调度队列还是 为 0 ，则退出调度线程
                        if (waitQuere) {
                            timerExit = true;
                            return;
                        } else {
                            waitQuere = true;
                            ThreadWait(60000);
                        }
                        continue;
                    }
                    waitQuere = false;
                    int minImIndex = getMinTimeImIndex(SystemClock.elapsedRealtime());
                    if (minImIndex == -1) {
                        continue;
                    }
                    RunnableIm runnableIm = RUNNABLE_LIST.get(minImIndex);
                    long sleepTime = runnableIm.getTime() - (SystemClock.elapsedRealtime() - runnableIm.getSysTime());
                    if (sleepTime <= 0) {
                        RUNNABLE_LIST.remove(minImIndex);
                        execute(runnableIm.getRunnable());
                        runnableIm.recycle();  //标志为销毁，在下次运行时执行
                        continue;
                    }
                    ThreadWait(sleepTime);//在线程等待指定时间后，执行 任务
                }
            }
        }

        private void ThreadWait(long time) {
            try {
                listLock.wait(time);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 获取线程列表中最小的等待时间
     *
     * @return 存储线程的类
     */
    private static int getMinTimeImIndex(long time) {
        int minIndex = -1;
        RunnableIm imMin = null;
        for (int index = 0; index < RUNNABLE_LIST.size(); index++) {
            if (minIndex == -1) {  //将下标指向 0
                minIndex = index;
                imMin = RUNNABLE_LIST.get(index);
            } else {
                //如果遍历的时候找到一个更小的，则重新指向这个最小的
                RunnableIm imTmp = RUNNABLE_LIST.get(index);
                if ((imTmp.getTime() - (time - imTmp.getSysTime())) < (imMin.getTime() - (time - imMin.getSysTime()))) {
                    minIndex = index;
                    imMin = imTmp;
                }
            }
        }
        return minIndex;
    }


    private static class RunnableIm {

        long time;
        Runnable runnable;
        long sysTime;
        RunnableIm nextRunnableIm;

        private RunnableIm next;

        private static final Object sPoolSync = new Object();
        private static RunnableIm sPool;
        private static int sPoolSize = 0;
        private static final int MAX_POOL_SIZE = 32;

        static RunnableIm obtain() {
            synchronized (sPoolSync) {
                if (sPool != null) {
                    RunnableIm m = sPool;
                    sPool = m.next;
                    m.next = null;
                    sPoolSize--;
                    return m;
                }
            }
            return new RunnableIm();
        }

        static RunnableIm obtain(long time, Runnable runnable, long sysTime) {
            RunnableIm obtain = obtain();
            obtain.time = time;
            obtain.runnable = runnable;
            obtain.sysTime = sysTime;
            obtain.nextRunnableIm = null;
            return obtain;
        }

        void recycle() {
            time = 0;
            runnable = null;
            sysTime = 0;
            nextRunnableIm = null;

            synchronized (sPoolSync) {
                if (sPoolSize < MAX_POOL_SIZE) {
                    next = sPool;
                    sPool = this;
                    sPoolSize++;
                }
            }
        }

        long getTime() {
            return time;
        }

        long getSysTime() {
            return sysTime;
        }

        Runnable getRunnable() {
            return runnable;
        }
    }

}
