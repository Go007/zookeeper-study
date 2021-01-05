package com.hong.zk;

/**
 * @Description: Zk 分布式锁实现的构成属性
 * @Author wanghong
 * @Date 2021/1/5 13:57
 * @Version V1.0
 **/
public class ZkLockBean {

    private String currentPath;

    private String beforePath;

    private int reentrantCount;

    public String getCurrentPath() {
        return currentPath;
    }

    public void setCurrentPath(String currentPath) {
        this.currentPath = currentPath;
    }

    public String getBeforePath() {
        return beforePath;
    }

    public void setBeforePath(String beforePath) {
        this.beforePath = beforePath;
    }

    public int getReentrantCount() {
        return reentrantCount;
    }

    public void setReentrantCount(int reentrantCount) {
        this.reentrantCount = reentrantCount;
    }

    @Override
    public String toString() {
        return "ZkLockBean{" +
                "currentPath='" + currentPath + '\'' +
                ", beforePath='" + beforePath + '\'' +
                ", reentrantCount=" + reentrantCount +
                '}';
    }
}
