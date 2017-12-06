package yz.lock;

/**
 * author: liuyazong
 * datetime: 2017/12/3 下午4:18
 */
public interface DistLock<T> {
    /**
     * Acquires the lock.
     *
     * @throws Exception
     */
    void acquire() throws Exception;


    /**
     * Acquires the lock only if it is free at the time of invocation.
     *
     * @param time the maximum time to wait for the lock
     * @return
     * @throws Exception
     */
    boolean acquire(long time) throws Exception;

    /**
     * Releases the lock.
     *
     * @throws Exception
     */
    void release() throws Exception;
}
