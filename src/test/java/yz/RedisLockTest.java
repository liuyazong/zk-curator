package yz;

import org.junit.Test;
import redis.clients.jedis.JedisPool;
import yz.lock.DistLock;
import yz.lock.redis.RedisDistLockImpl;

/**
 * author: liuyazong
 * datetime: 2017/12/4 下午5:17
 */
public class RedisLockTest {
    @Test
    public void test() throws Exception {
        DistLock lock = new RedisDistLockImpl(new JedisPool("127.0.0.1",6379), "redis_lock");
        lock.acquire(1000);
        lock.release();

    }
}
