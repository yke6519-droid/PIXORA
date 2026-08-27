package com.example.picturebackend.manager.batch;

import com.example.picturebackend.constant.PictureConstant;

import java.util.concurrent.TimeUnit;

/**
 * 记录一次批量抓图任务的截止时间。
 *
 * 超时不抛异常，而是让业务层正常返回已经完成的图片，避免事务回滚。
 */
public class BatchImportContext {
    private final long deadlineNanos;
    private volatile boolean stopped;

    public BatchImportContext() {
        this(PictureConstant.BATCH_IMPORT_TIMEOUT_MILLIS);
    }

    /** 测试时可以传入很短的时间，验证超时行为。 */
    BatchImportContext(long timeoutMillis) {
        long safeTimeoutMillis = Math.max(timeoutMillis, 0L);
        this.deadlineNanos = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(safeTimeoutMillis);
    }

    /** 返回任务是否已经超时或被主动停止。 */
    public boolean isTimeout() {
        if (stopped) {
            return true;
        }
        if (System.nanoTime() >= deadlineNanos) {
            stopped = true;
        }
        return stopped;
    }

    /** 返回当前还能使用的毫秒数，至少返回1，便于传给 HTTP 客户端。 */
    public long getRemainingMillis() {
        if (isTimeout()) {
            return 0L;
        }
        long remainingMillis = TimeUnit.NANOSECONDS.toMillis(deadlineNanos - System.nanoTime());
        return Math.max(remainingMillis, 1L);
    }

    /** 在请求被取消时主动停止后续抓取。 */
    public void stop() {
        stopped = true;
    }
}
