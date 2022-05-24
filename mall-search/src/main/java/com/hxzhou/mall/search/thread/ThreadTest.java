package com.hxzhou.mall.search.thread;

import java.util.concurrent.*;

public class ThreadTest {

    public static ExecutorService executor = Executors.newFixedThreadPool(10);

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        System.out.println("main...start...");

        /**
         * 创建和启动异步任务
         */
//        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
//            System.out.println("当前线程：" + Thread.currentThread().getId());
//            int i = 10 / 2;
//            System.out.println("运行结果：" + i);
//        }, executor);
//        CompletableFuture<Integer> future = CompletableFuture.supplyAsync(() -> {
//            System.out.println("当前线程：" + Thread.currentThread().getId());
//            int i = 10 / 2;
//            System.out.println("运行结果：" + i);
//            return i;
//        }, executor);

        /**
         * 计算完成时回调方法：方法完成时的感知
         */
//        CompletableFuture<Integer> future = CompletableFuture.supplyAsync(() -> {
//            System.out.println("当前线程：" + Thread.currentThread().getId());
//            int i = 10 / 0;
//            System.out.println("运行结果：" + i);
//            return i;
//        }, executor).whenComplete((res, exception) -> {
//            // 虽然能得到异常信息，但是没法修改返回数据
//            System.out.println("异步任务成功完成了......结果是：" + res + "；异常是：" + exception);
//        }).exceptionally(throwable -> {
//            // 可以感知异常，同时返回默认值
//            return 10;
//        });

        /**
         * handle方法：方法完成时的处理
         */
//        CompletableFuture<Integer> future = CompletableFuture.supplyAsync(() -> {
//            System.out.println("当前线程：" + Thread.currentThread().getId());
//            int i = 10 / 2;
//            System.out.println("运行结果：" + i);
//            return i;
//        }, executor).handle((integer, throwable) -> {
//            if(integer != null) return integer * 2;
//            if(throwable != null) return -1;
//            return 0;
//        });

        /**
         * 线程串行化
         *      1 thenRun：不能获取上一步的执行结果，无返回值
         *      2 thenAccept：能接收上一步结果，但是无返回值
         *      3 thenApply：即能接收到上一步的结果，也有返回值
         */
//        CompletableFuture.supplyAsync(() -> {
//            System.out.println("当前线程：" + Thread.currentThread().getId());
//            int i = 10 / 2;
//            System.out.println("运行结果：" + i);
//            return i;
//        }, executor).thenApplyAsync(integer -> {
//            int a = 100;
//            System.out.println("thenApplyAsync即可以接收上一步结果：" + integer + "；也有返回值：" + a);
//            return a;
//        }, executor).thenAcceptAsync(integer -> {
//            System.out.println("thenAcceptAsync可以接收到上一步的结果：" + integer);
//        }, executor).thenRunAsync(() -> {
//            System.out.println("thenRun无法接收到上一层的结果");
//        }, executor);

//        CompletableFuture<Object> future01 = CompletableFuture.supplyAsync(() -> {
//            System.out.println("任务1线程：" + Thread.currentThread().getId());
//            int i = 10 / 2;
//            System.out.println("任务1结束！！！");
//            return i;
//        }, executor);
//
//        CompletableFuture<Object> future02 = CompletableFuture.supplyAsync(() -> {
//            System.out.println("任务2线程：" + Thread.currentThread().getId());
//            int i = 10 / 5;
//            try {
//                Thread.sleep(3000);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//            System.out.println("任务2结束！！！");
//            return "Hello";
//        }, executor);

        /**
         * 两个都完成
         */
//        CompletableFuture<Void> future03 = future01.runAfterBothAsync(future02, () -> {
//            System.out.println("任务3开始！！！");
//        }, executor);
//        CompletableFuture<Void> future03 = future01.thenAcceptBothAsync(future02, (f1, f2) -> {
//            System.out.println("任务3开始，同时接收到前两个值：" + f1 + "===" + f2);
//        }, executor);
//        CompletableFuture<String> future03 = future01.thenCombineAsync(future02, (f1, f2) -> {
//            System.out.println("任务3开始，不仅可以接收到前两个值：" + f1 + "===" + f2 + "；还能返回一个值：" + "HAHA");
//            return "HAHA";
//        });

        /**
         * 两个只要有一个完成即可
         */
//        future01.runAfterEitherAsync(future02, () -> {
//            System.out.println("任务3开始！！！");
//        }, executor);
//        future01.acceptEitherAsync(future02, (res) -> {
//            System.out.println("任务3开始，同时接收到前面的值：" + res);
//        }, executor);
//        CompletableFuture<String> future03 = future01.applyToEitherAsync(future02, (res) -> {
//            System.out.println("任务3开始，不仅可以接收到前两个值：" + res + "；还能返回一个值：" + "HAHA");
//            return "HAHA";
//        }, executor);

//        System.out.println("main...end...");

        /**
         * 多任务组合
         */
        CompletableFuture<Object> futureImg = CompletableFuture.supplyAsync(() -> {
            System.out.println("查询商品的图片信息线程：" + Thread.currentThread().getId());
            return "Hello.jpg";
        }, executor);

        CompletableFuture<Object> futureAttr = CompletableFuture.supplyAsync(() -> {
            System.out.println("查询商品的属性信息线程：" + Thread.currentThread().getId());
            return "黑色+256G";
        }, executor);

        CompletableFuture<Object> futureDesc = CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                System.out.println("查询商品的详细信息线程：" + Thread.currentThread().getId());
            }
            return "星Star";
        }, executor);

//        CompletableFuture<Void> allOf = CompletableFuture.allOf(futureImg, futureAttr, futureDesc);
//        allOf.get();        // 等待所有结果完成
//        System.out.println("main...end..." + futureImg.get() + "=>" + futureAttr.get() + "=>" + futureDesc.get() + "=>");

        CompletableFuture<Object> anyOf = CompletableFuture.anyOf(futureImg, futureAttr, futureDesc);
        anyOf.get();        // 等待所有结果完成
        System.out.println("main...end..." + anyOf.get());

    }

    public void thread(String[] args) throws ExecutionException, InterruptedException {
        System.out.println("main...start...");
        /**
         * 初始化线程的四种方式
         *      1 继承Thread
         *      2 实现Runnable接口
         *      3 实现Callable接口 + FutureTask （可以拿到返回结果，可以处理异常）
         *      4 线程池
         *
         * 区别：
         *      1和2不能得到返回值；3可以得到返回值
         *      1、2、3都不能控制资源；4可以控制资源，使之系统性能稳定
         */
//        // 1 继承Thread
//        Thread01 thread = new Thread01();
//        thread.start();     // 启动线程

//        // 2 实现Runnable接口
//        Runnable01 runnable01 = new Runnable01();
//        new Thread(runnable01).start();

//        // 3 实现Callable接口
//        FutureTask<Integer> futureTask = new FutureTask<>(new Callable01());
//        new Thread(futureTask).start();
//
//        // 等待整个线程执行完成，获取返回结果
//        Integer integer = futureTask.get();
//        System.out.println("Callable的返回结果为：" + integer);

        // 4 线程池：给线程池直接提交任务
        // 以后的业务代码里面，以上三种启动线程的方式都不用，而是将所有的多线程异步任务都交给线程池执行
        /**
         * 线程池【ExecutorService】
         *      1 创建
         *          1.1 Executors
         *          1.2 new ThreadPoolExecutor()
         *      2 七大参数
         *          2.1 corePoolSize：核心线程数【只要线程池不销毁就一直存在，除非设置超时allowCoreThreadTimeOut】。
         *                              线程池，创建好以后就准备就绪的线程数量，就等待来接收异步任务去执行【俗话来说：有编制的】
         *          2.2 maximumPoolSize：最大线程数量。控制资源并发
         *          2.3 keepAliveTime：存活时间。如果当前的线程数量大于core数量，就释放空闲的线程【maximumPoolSize-corePoolSize】。
         *                              只要线程空闲大于指定的keepAliveTime，就【解雇临时工】
         *          2.4 unit：时间单位。
         *          2.5 workQueue：阻塞队列。如果任务有很多，就会将目前多的任务放在队列里面，只要有线程空闲，就会去队列里面取出新的任务继续执行。
         *          2.6 threadFactory：线程的创建工厂
         *          2.7 RejectedExecutionHandler handler：拒绝策略。如果队列满了，按照我们指定的拒绝策略拒绝执行任务。
         *      3 运行流程
         *          3.1 线程池创建，准备好core数量的核心线程，准备接受任务
         *          3.2 新的任务进来，用core准备好的空间线程执行
         *              3.2.1 core满了，就将再进来的任务放入阻塞队列中。空闲的core就会自己去阻塞队列获取任务执行。
         *              3.2.2 阻塞队列满了，就直接开新线程执行，最大只能开到max指定的数量。
         *              3.2.3 max都执行好了，Max-core数量空闲的线程会在keepAliveTime指定的时间后自动销毁，最终保持core大小。
         *              3.2.4 如果线程数开到max的数量，还有新任务进来，就会使用reject指定的拒绝策略进行处理。
         *          3.3 所有的线程创建都是由指定的factory创建的。
         *      4 面试
         *          一个线程池 core 7 ； max 20 ； queue 50 ； 100并发进来时要怎么分配
         *          回答：
         *              7个会得到立即执行；50个进入队列；再开13个进行执行。最后剩下的30个就是用拒绝策略（如果不想抛弃还要执行，可以用CallerRunsPolicy）
         */
//        service.execute(new Runnable01());

        System.out.println("main...end...");
    }

    // 1 继承Thread
    public static class Thread01 extends Thread {
        @Override
        public void run() {
            System.out.println("当前线程：" + Thread.currentThread().getId());
            int i = 10 / 2;
            System.out.println("运行结果：" + i);
        }
    }

    // 2 实现Runnable接口
    public static class Runnable01 implements Runnable {
        @Override
        public void run() {
            System.out.println("当前线程：" + Thread.currentThread().getId());
            int i = 10 / 2;
            System.out.println("运行结果：" + i);
        }
    }

    // 3 实现Callable接口
    public static class Callable01 implements Callable<Integer> {

        @Override
        public Integer call() throws Exception {
            System.out.println("当前线程：" + Thread.currentThread().getId());
            int i = 10 / 2;
            System.out.println("运行结果：" + i);
            return i;
        }
    }
}
