package cn.mrcode.newstudy.hpbase._11.reference;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class EmployeeCache extends Thread {
    //单例
    private static EmployeeCache cache;

    //容器
    private Map<String, WeakEmployee> referent;

    //引用队列当SoftEmployee对象中的目标对象被销毁后 会自定把SoftEmployee对象加入到该序列中
    //这样就可以及时的清掉没有目标对象的SoftEmployee
    private ReferenceQueue<Employee> queue;

    //同步锁
    private static Object lock = new Object();

    //继承SoftReference,实现对对象的软引用
    //这个类所引用的目标对象会在JVM内存不足时自动回收
    private class WeakEmployee extends WeakReference<Employee> {

        private String key;

        public String getKey() {
            return key;
        }

        public WeakEmployee(Employee referent, ReferenceQueue<Employee> queue) {
            super(referent, queue);
            this.key = referent.getId();
        }

    }

    public synchronized Employee getEmployee(String id) {
        Employee e = null;
        if (referent.containsKey(id)) {
            e = referent.get(id).get();
        }
        if (e == null) {
            e = new Employee(id);
            cacheEmployee(e);
        }
        return e;
    }

    //缓存对象
    private void cacheEmployee(Employee e) {
        cleanCache();// 清除垃圾引用
        WeakEmployee ref = new WeakEmployee(e, queue);
        referent.put(e.getId(), ref);
    }

    //私有化构造参数
    private EmployeeCache() {
        this.referent = Collections.synchronizedMap(new HashMap<String, WeakEmployee>());
        this.queue = new ReferenceQueue<Employee>();
    }

    //获得实例
    public static EmployeeCache getInstance() {
        if (cache == null) {
            synchronized (lock) {
                if (cache == null) {
                    cache = new EmployeeCache();
                }
            }
        }
        return cache;
    }

    //将SoftEmployee中目标元素为空的对象清除
    private void cleanCache() {
        WeakEmployee se = null;
        while ((se = (WeakEmployee) queue.poll()) != null) {
            referent.remove(se.getKey());
            System.out.println("对象ID : " + se.getKey() + "已经被JVM回收");
        }
    }

    public int getSize() {
        return referent.size();
    }

    //清除缓存
    public void clearCache() {
        cleanCache();
        referent.clear();
    }

    @Override
    public void run() {
        Reference<? extends Employee> r = null;
//        try {
//            int cnt = 0;
////            while ((r = queue.remove()) != null) {
////                System.out.println((cnt++) + "回收了:" + r);
////            }
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
        while (true){}
    }

    public static void main(String[] args) throws InterruptedException {
        EmployeeCache cache = EmployeeCache.getInstance();
        cache.start();
        for (int i = 0; i < 60000; i++) {
            cache.getEmployee(String.valueOf(i));
            System.gc();
        }
        cache.join();
    }
}
