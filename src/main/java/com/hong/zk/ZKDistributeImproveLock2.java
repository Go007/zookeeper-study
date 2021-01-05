package com.hong.zk;

import org.I0Itec.zkclient.IZkDataListener;
import org.I0Itec.zkclient.ZkClient;
import org.I0Itec.zkclient.exception.ZkNodeExistsException;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

public class ZKDistributeImproveLock2 implements Lock {

    private ThreadLocal<ZkLockBean> tl;

    private String lockPath;

    private ZkClient client;

    public ZKDistributeImproveLock2(String lockPath) {
        super();
        tl = new ThreadLocal<>();
        this.lockPath = lockPath;
        client = new ZkClient("localhost:2181");
        client.setZkSerializer(new MyZkSerializer());
        if (!this.client.exists(lockPath)) {
            try {
                this.client.createPersistent(lockPath);
            } catch (ZkNodeExistsException e) {

            }
        }
    }

    @Override
    public void lock() {
        if (!tryLock()) {
            // 阻塞等待
            waitForLock();
            // 递归调用，再次尝试加锁
            lock();
        }
    }

    @Override
    public boolean tryLock() {
        ZkLockBean zkLockBean = tl.get();
        if (zkLockBean != null) {
            int count = zkLockBean.getReentrantCount();
            if (count > 0) {
                zkLockBean.setReentrantCount(++count);
                return true;
            }
        } else {
            zkLockBean = new ZkLockBean();
            tl.set(zkLockBean);
            zkLockBean.setCurrentPath(this.client.createEphemeralSequential(lockPath + "/", "aaa"));
        }

        List<String> children = this.client.getChildren(lockPath);
        Collections.sort(children);

        if (zkLockBean.getCurrentPath().equals(lockPath + "/" + children.get(0))) {
            zkLockBean.setReentrantCount(1);
            return true;
        }

        int curIndex = children.indexOf(zkLockBean.getCurrentPath().substring(lockPath.length() + 1));
        zkLockBean.setBeforePath(lockPath + "/" + children.get(curIndex - 1));
        return false;
    }

    private void waitForLock() {
        CountDownLatch cdl = new CountDownLatch(1);

        IZkDataListener listener = new IZkDataListener() {
            @Override
            public void handleDataDeleted(String dataPath) {
                System.out.println("-----监听到节点被删除----");
                cdl.countDown();
            }

            @Override
            public void handleDataChange(String dataPath, Object data) {

            }
        };

        String beforePath = tl.get().getBeforePath();
        client.subscribeDataChanges(beforePath, listener);

        if (this.client.exists(beforePath)) {
            try {
                cdl.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }

        client.unsubscribeDataChanges(beforePath, listener);
    }

    @Override
    public void unlock() {
        if (tl.get() == null) {
            return;
        }

        int count = tl.get().getReentrantCount();
        if (count > 1) {
            tl.get().setReentrantCount(--count);
            return;
        }

        this.client.delete(tl.get().getCurrentPath());
    }

    @Override
    public void lockInterruptibly() throws InterruptedException {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public Condition newCondition() {
        // TODO Auto-generated method stub
        return null;
    }

    public static void main(String[] args) {
        // 并发数
        int currency = 50;

        // 循环屏障
        CyclicBarrier cb = new CyclicBarrier(currency);

        // 多线程模拟高并发
        for (int i = 0; i < currency; i++) {
            new Thread(() -> {

                System.out.println(Thread.currentThread().getName() + "---------我准备好---------------");
                // 等待一起出发
                try {
                    cb.await();
                } catch (InterruptedException | BrokenBarrierException e) {
                    e.printStackTrace();
                }

                ZKDistributeImproveLock2 lock = new ZKDistributeImproveLock2("/distLock");

                try {
                    lock.lock();
                    // 模拟业务耗时
                    Thread.sleep(200);
                    System.out.println(Thread.currentThread().getName() + " 获得锁！");
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    lock.unlock();
                }
            }).start();
        }
    }

}
