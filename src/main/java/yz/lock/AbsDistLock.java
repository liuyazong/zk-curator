package yz.lock;

import lombok.Data;

/**
 * author: liuyazong
 * datetime: 2017/12/4 上午11:47
 */
@Data
public abstract class AbsDistLock<T> implements DistLock<T> {
    private T resource;
    private String lock;
    private Thread ownerThread;

    public AbsDistLock(T resource, String lock) {
        this.resource = resource;
        this.lock = lock;
        this.ownerThread=Thread.currentThread();
    }
}
