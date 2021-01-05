package com.hong.zk;

/**
 * @Description:
 * @Author wanghong
 * @Date 2021/1/5 15:07
 * @Version V1.0
 **/
public class Test {

    private static ThreadLocal<ZkLockBean> tl = new ThreadLocal<>();

    public static void main(String[] args) {
        ZkLockBean zkLockBean = tl.get();
        if (zkLockBean == null){
            zkLockBean = new ZkLockBean();
            tl.set(zkLockBean);
            zkLockBean.setCurrentPath("1234");
        }

        zkLockBean.setBeforePath("5678");

        zkLockBean = tl.get();
        System.out.println(zkLockBean);
    }
}
