package yz;

import lombok.extern.slf4j.Slf4j;
import org.apache.zookeeper.ZooKeeper;
import org.junit.Test;
import yz.lock.DistLock;
import yz.lock.zk.ZkDistLockImpl;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * author: liuyazong
 * datetime: 2017/12/4 上午11:40
 */
@Slf4j
public class ZkLockTest {
    @Test
    public void test() throws InterruptedException, IOException {

        ZooKeeper zooKeeper = new ZooKeeper("127.0.0.1:2181", 3000, event -> log.debug(event.toString()));

        int nThreads = 30;
        CountDownLatch countDownLatch = new CountDownLatch(nThreads);
        ExecutorService pool = Executors.newFixedThreadPool(nThreads);
        for (int i = 0; i < nThreads; i++) {
            pool.execute(() -> {

                DistLock lock = new ZkDistLockImpl(zooKeeper, "test_lock1");
                try {
                    if (lock.acquire(1000)) {
                        log.debug("do sth.");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    try {
                        lock.release();
                        countDownLatch.countDown();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        }
        countDownLatch.await();
        pool.shutdown();
        zooKeeper.close();
    }

    @Test
    public void testOwnerThread() throws IOException, InterruptedException {
        ZooKeeper zooKeeper = new ZooKeeper("127.0.0.1:2181", 3000, event -> log.debug(event.toString()));
        Thread t1 = new Thread(() -> {
            DistLock lock = new ZkDistLockImpl(zooKeeper, "test_lock1");
            try {
                lock.acquire(1000);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                Thread t2 = new Thread(() -> {
                    try {
                        lock.release();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
                t2.start();
                try {
                    t2.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }
        });
        t1.start();
        t1.join();
    }
}
