# Zookeeper & Curator Framework

## Zookeeper

### API

Zookeeper Api 基础操作

```java
    //创建客户端
    ZooKeeper zooKeeper = new ZooKeeper("127.0.0.1:2181", 5000, event -> log.debug(event.toString()));
    {
        //创建临时节点
        String path = "/test0";
        String result = zooKeeper.create(path, "1".getBytes(), OPEN_ACL_UNSAFE, EPHEMERAL);
        log.debug("create {} node: {}", EPHEMERAL, result);
    }
    {
        //创建持久节点
        String path = "/test1";
        String result = zooKeeper.create(path, "1".getBytes(), OPEN_ACL_UNSAFE, PERSISTENT);
        log.debug("create {} node: {}", PERSISTENT, result);

        //获取节点数据
        byte[] data = zooKeeper.getData(path, event -> log.debug(event.toString()), null);
        log.debug("get data of path:{},{}", path, new String(data));
        {
            //创建子节点
            String result1 = zooKeeper.create(path.concat(path), "1".getBytes(), OPEN_ACL_UNSAFE, PERSISTENT);
            log.debug("create {} node: {}", PERSISTENT, result1);
            //获取子节点
            List<String> children = zooKeeper.getChildren(path, true);
            log.debug("children of path:{},{}", path, children);
            //删除子节点
            zooKeeper.delete(result1, Version.REVISION);
            log.debug("delete {} node: {}", PERSISTENT, result1);
        }
        zooKeeper.delete(result, Version.REVISION);
        log.debug("delete {} node: {}", PERSISTENT, result);
    }
    {
        //创建临时顺序节点
        String path = "/test2-";
        String result = zooKeeper.create(path, "1".getBytes(), OPEN_ACL_UNSAFE, EPHEMERAL_SEQUENTIAL);
        log.debug("create {} node: {}", EPHEMERAL_SEQUENTIAL, result);
        zooKeeper.delete(result, Version.REVISION);
        log.debug("delete {} node: {}", EPHEMERAL_SEQUENTIAL, result);
    }
    {
        //创建持久顺序节点
        String path = "/test3-";
        String result = zooKeeper.create(path, "1".getBytes(), OPEN_ACL_UNSAFE, PERSISTENT_SEQUENTIAL);
        log.debug("create {} node: {}", PERSISTENT_SEQUENTIAL, result);
        zooKeeper.delete(result, Version.REVISION);
        log.debug("delete {} node: {}", PERSISTENT_SEQUENTIAL, result);
    }
    //关闭客户端
    zooKeeper.close();
```
    
### 应用

* 实现分布式锁

`yz.lock.zk.ZkDistLockImpl`

* 顺手来个基于Redis的分布式锁

`yz.lock.redis.RedisDistLockImpl`

## Curator Framework

### API

#### Curator Framework Api 基础操作

```java
    //创建客户端
    CuratorFramework client = CuratorFrameworkFactory.builder()
            .connectString("127.0.0.1:2181")
            .sessionTimeoutMs(3000)
            .connectionTimeoutMs(3000)
            .retryPolicy(new ExponentialBackoffRetry(1000, 3))
            .namespace("curator")
            .build();
    //启动客户端
    client.start();
    {
        //创建临时节点
        CreateMode mode = CreateMode.EPHEMERAL;
        String path = client.create().withMode(mode).forPath("/EPHEMERAL");
        log.debug(" create {} path {}", mode, path);
    }
    {
        //创建临时顺序节点
        CreateMode mode = CreateMode.EPHEMERAL_SEQUENTIAL;
        String path = client.create().withMode(mode).forPath("/EPHEMERAL_SEQUENTIAL");
        log.debug(" create {} path {}", mode, path);
    }
    {
        //创建持久节点
        CreateMode mode = CreateMode.PERSISTENT;
        String path = client.create().withMode(mode).forPath("/PERSISTENT", "1".getBytes());
        log.debug(" create {} path {}", mode, path);
        //获取节点数据
        byte[] bytes = client.getData().forPath(path);
        log.debug("get data for path {},data {}", path, new String(bytes));
        //设置节点数据
        Stat stat = client.setData().forPath(path, "2".getBytes());
        log.debug("set data for path {},data {},stat {}", path, new String(client.getData().forPath(path)), stat);
        //删除节点
        Void aVoid = client.delete().forPath(path);
        log.debug(" delete {} path {}", mode, path);
    }

    {
        //创建持久顺序节点
        CreateMode mode = CreateMode.PERSISTENT_SEQUENTIAL;
        String path = client.create().withMode(mode).forPath("/PERSISTENT_SEQUENTIAL");
        log.debug(" create {} path {}", mode, path);
        Void aVoid = client.delete().forPath(path);
        log.debug(" delete {} path {}", mode, path);
    }
    {
        //检查节点是否存在
        String path = "/test";
        Stat stat = client.checkExists().forPath(path);
        log.debug("check exists for path {},stat {}", path, stat);
    }
```

#### 异步操作

```java
    {
        //异步操作
        String path = client
                .create()
                .creatingParentsIfNeeded()
                .withMode(CreateMode.EPHEMERAL)
                .inBackground((curatorFramework, event) -> log.debug("inBackground event {}", event))
                .forPath("/inBackground");
        log.debug("inBackground path {}", path);
    }
```

#### 事务

```java
    {
        //事务
        List<CuratorTransactionResult> tx = client.transaction().forOperations(
                client.transactionOp().create().forPath("/tx"),
                client.transactionOp().check().forPath("/tx"),
                client.transactionOp().setData().forPath("/tx", "tx".getBytes()),
                client.transactionOp().delete().forPath("/tx"));
        tx.forEach(t -> {
            log.debug("tx type {},error {},for path {},result path {},stat {}", t.getType(), t.getError(), t.getForPath(), t.getResultPath(), t.getResultStat());
        });
    }
```

#### 异步事务

```java
    {
        //异步事务
        List<CuratorTransactionResult> tx = client
                .transaction()
                .inBackground((client1, event) -> {
                    log.debug("inBackground tx event {}", event);
                    List<CuratorTransactionResult> opResults = event.getOpResults();
                    opResults.forEach(t -> {
                        log.debug("inBackground tx type {},error {},for path {},result path {},stat {}", t.getType(), t.getError(), t.getForPath(), t.getResultPath(), t.getResultStat());
                    });
                })
                .forOperations(
                        client.transactionOp().create().forPath("/tx"),
                        client.transactionOp().check().forPath("/tx"),
                        client.transactionOp().setData().forPath("/tx", "tx".getBytes()),
                        client.transactionOp().delete().forPath("/tx"));

    }
```

#### 分布式锁

##### 测试类

```java
    class Test {
        int anInt = 0;

        public void increment() {
            this.anInt++;
        }
    }
```
    
##### 可重入锁

可重入锁实现类：InterProcessMutex

```java
    {
        //可重入锁
        String path = "/srlock";
        int nThreads = 1000;
        ExecutorService pool = Executors.newFixedThreadPool(nThreads);
        Test test = new Test();
        CountDownLatch countDownLatch = new CountDownLatch(nThreads);
        for (int i = 0; i < nThreads; i++) {
            pool.execute(() -> {
                InterProcessMutex lock = new InterProcessMutex(client, path);
                boolean locked = false;
                try {
                    if (locked = lock.acquire(5000, TimeUnit.MILLISECONDS)) {
                        test.increment();
                    } else {
                        log.debug("srlock {}", locked);

                    }
                } catch (Exception e) {
                    log.error("获取锁失败",e);
                } finally {
                    try {
                        if (locked) {
                            lock.release();
                        }
                    } catch (Exception e) {
                        log.error("释放锁失败",e);
                    }
                    countDownLatch.countDown();
                }
            });
        }
        countDownLatch.await();
        pool.shutdown();
        log.debug("srlock {}", test.anInt);
    }
```

##### 不可重入锁

不可重入锁实现类：InterProcessSemaphoreMutex

```java
    {
        //不可重入锁
        String path = "/slock";
        int nThreads = 1000;
        ExecutorService pool = Executors.newFixedThreadPool(nThreads);
        Test test = new Test();
        CountDownLatch countDownLatch = new CountDownLatch(nThreads);
        for (int i = 0; i < nThreads; i++) {
            pool.execute(() -> {
                InterProcessSemaphoreMutex lock = new InterProcessSemaphoreMutex(client, path);
                boolean locked = false;
                try {
                    if (locked = lock.acquire(5000, TimeUnit.MILLISECONDS)) {
                        test.increment();
                    } else {
                        log.debug("slock {}", locked);
                    }
                } catch (Exception e) {
                    log.error("获取锁失败",e);
                } finally {
                    try {
                        if (locked) {
                            lock.release();
                        }
                    } catch (Exception e) {
                        log.error("释放锁失败",e);
                    }
                    countDownLatch.countDown();
                }
            });
        }
        countDownLatch.await();
        pool.shutdown();
        log.debug("slock {}", test.anInt);
    }
```

##### 可重入读写锁

可重入读写锁实现类：InterProcessReadWriteLock

```java
    {
        //可重入读写锁
        String path = "/srrwlock";
        int nThreads = 1000;
        ExecutorService pool = Executors.newFixedThreadPool(nThreads);
        Test test = new Test();
        CountDownLatch countDownLatch = new CountDownLatch(nThreads);
        for (int i = 0; i < nThreads; i++) {
            pool.execute(() -> {
                InterProcessReadWriteLock lock = new InterProcessReadWriteLock(client, path);
                InterProcessMutex readLock = lock.readLock();
                InterProcessMutex writeLock = lock.writeLock();
                boolean wlocked = false;
                boolean rlocked = false;
                try {
                    if (wlocked = writeLock.acquire(5000, TimeUnit.MILLISECONDS)) {
                        test.increment();
                    } else {
                        log.debug("writeLock {}", wlocked);
                    }

                    if (rlocked = readLock.acquire(5000, TimeUnit.MILLISECONDS)) {
                        log.debug("readLock {}", test.anInt);
                    } else {
                        log.debug("readLock {}", rlocked);
                    }

                } catch (Exception e) {
                    log.error("获取锁失败",e);
                } finally {
                    try {
                        if (wlocked) {
                            writeLock.release();
                        }
                        if (rlocked) {
                            readLock.release();
                        }
                    } catch (Exception e) {
                        log.error("释放锁失败",e);
                    }
                    countDownLatch.countDown();
                }
            });
        }
        countDownLatch.await();
        pool.shutdown();
        log.debug("srrwlock ", test.anInt);
    }
```
    
#### 关闭客户端
    
```java
    client.close();
```
    
