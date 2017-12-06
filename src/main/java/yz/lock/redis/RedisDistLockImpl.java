package yz.lock.redis;

import lombok.extern.slf4j.Slf4j;
import redis.clients.jedis.Jedis;
import redis.clients.util.Pool;
import yz.lock.AbsDistLock;
import yz.lock.LockException;

import java.util.UUID;

/**
 * author: liuyazong
 * datetime: 2017/12/4 上午11:44
 */
@Slf4j
public class RedisDistLockImpl extends AbsDistLock<Pool<Jedis>> {

    private static final long DEFAULT_TIME = 60000;

    public RedisDistLockImpl(Pool<Jedis> resource, String lock) {
        super(resource, lock);
    }

    @Override
    public void acquire() throws Exception {
        try (Jedis resource = this.getResource().getResource();) {
            for (; ; ) {
                String set = resource.set(this.getLock(), UUID.randomUUID().toString(), "NX", "PX", DEFAULT_TIME);
                if ("OK".equals(set)) {
                    log.debug("{} lock success", this.getLock());
                    break;
                }
            }
        }
    }


    @Override
    public boolean acquire(long time) throws Exception {
        try (Jedis resource = this.getResource().getResource()) {
            long start = System.currentTimeMillis();
            for (String set = resource.set(this.getLock(), UUID.randomUUID().toString(), "NX", "PX", DEFAULT_TIME);
                 System.currentTimeMillis() - start < time;
                 set = resource.set(this.getLock(), UUID.randomUUID().toString(), "NX", "PX", DEFAULT_TIME)) {
                if ("OK".equals(set)) {
                    log.debug("{} lock success", this.getLock());
                    return true;
                }
            }
            return false;
        }
    }

    @Override
    public void release() throws Exception {
        Thread currentThread = Thread.currentThread();
        if (null == this.getLock()) {
            //ignore
        } else {
            if (!currentThread.equals(this.getOwnerThread())) {
                throw new LockException(String.format("the lock %s on thread %s can not unlock by thread %s", this.getLock(), this.getOwnerThread(), currentThread));
            }
            try {
                try (Jedis resource = this.getResource().getResource();) {
                    Long del = resource.del(this.getLock());
                    log.debug("{} unlock success {}", this.getLock(), del > 0);
                }
            } catch (Exception e) {
                log.error(String.format("%s unlock failed", this.getLock()), e);
            }
        }
    }
}
