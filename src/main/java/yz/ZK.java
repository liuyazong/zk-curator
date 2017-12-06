package yz;

import lombok.extern.slf4j.Slf4j;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.Version;
import org.apache.zookeeper.ZooKeeper;

import java.io.IOException;
import java.util.List;

import static org.apache.zookeeper.CreateMode.*;
import static org.apache.zookeeper.ZooDefs.Ids.OPEN_ACL_UNSAFE;

/**
 * Hello world!
 */
@Slf4j
public class ZK {
    public static void main(String[] args) throws IOException, InterruptedException, KeeperException {

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
    }
}
