package yz.lock.zk;

import lombok.extern.slf4j.Slf4j;
import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;
import yz.lock.AbsDistLock;
import yz.lock.LockException;

import java.util.Comparator;
import java.util.List;

/**
 * author: liuyazong
 * datetime: 2017/12/3 下午4:27
 */
@Slf4j
public class ZkDistLockImpl extends AbsDistLock<ZooKeeper> {

    private String lockPath;

    public ZkDistLockImpl(ZooKeeper resource, String lock) {
        super(resource, lock);
    }

    @Override
    public void acquire() throws Exception {
        //创建父节点
        String parent = createParent(this.getLock());
        //创建子节点
        String child = createChild(parent);
        //获取子节点
        boolean isLocked = false;
        while (!isLocked) {
            List<String> children = this.getResource().getChildren(parent, false);
            children.sort(Comparator.naturalOrder());
            log.debug("children:{}", children);
            isLocked = isLocked(parent, child, children);
        }
    }


    @Override
    public boolean acquire(long time) throws Exception {
        //创建父节点
        String parent = createParent(this.getLock());
        //创建子节点
        String child = createChild(parent);
        //获取子节点
        long start = System.currentTimeMillis();
        boolean isLocked = false;
        while (!isLocked && (System.currentTimeMillis() - start) <= time) {
            List<String> children = this.getResource().getChildren(parent, false);
            children.sort(Comparator.naturalOrder());
            log.debug("children:{}", children);
            isLocked = isLocked(parent, child, children);
        }
        return isLocked;
    }

    private String createChild(String parent) throws KeeperException, InterruptedException {
        return this.getResource().create(parent.concat("/").concat("dist_lock_"), "1".getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);
    }

    private String createParent(String lockName) throws KeeperException, InterruptedException {
        //创建父节点
        String parent = "/".concat(lockName);
        if (null == this.getResource().exists(parent, false)) {
            synchronized (ZkDistLockImpl.class) {
                try {
                    parent = this.getResource().create(parent, "1".getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
                } catch (Exception e) {
                    //ignore
                }
            }
        }
        return parent;
    }

    private boolean isLocked(String parent, String child, List<String> children) throws KeeperException, InterruptedException {
        boolean isLocked;
        if (child.split("/")[2].equals(children.get(0))) {
            log.debug("{} lock success", child);
            isLocked = true;
            this.lockPath = child;
        } else {
            String ch1 = parent.concat("/").concat(children.get(1));
            Stat stat = this.getResource().exists(ch1, false);
            if (null != stat) {
                isLocked = false;
            } else {
                isLocked = true;
                this.lockPath = ch1;
                log.debug("{} lock success", ch1);
            }
        }
        return isLocked;
    }

    @Override
    public void release() throws Exception {
        Thread currentThread = Thread.currentThread();
        if (null == this.lockPath) {
            //ignore
        } else {
            if (!currentThread.equals(this.getOwnerThread())) {
                throw new LockException(String.format("the lock %s on thread %s can not unlock by thread %s", this.lockPath, this.getOwnerThread(), currentThread));
            }
            try {
                this.getResource().delete(this.lockPath, Version.REVISION);
                log.debug("{} unlock success", this.lockPath);
            } catch (Exception e) {
                log.error(String.format("%s unlock failed", this.lockPath), e);
            }
        }
    }

}
