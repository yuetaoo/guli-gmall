package com.atguigu.gmall.schedule;

import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;
//定时任务封装
public class TimerDemo {

    public static void main(String[] args) {
        System.out.println("系统当前时间:" + System.currentTimeMillis());

        new DelayTask().scheduled(() -> {
            System.out.println("通过延时队列定义的定时任务" + System.currentTimeMillis());
        }, 10l, 5l, TimeUnit.SECONDS);
    }
}

class DelayTask implements Delayed{

    private Long  timeOut;//任务执行时间
    private DelayQueue<DelayTask> delayQueue = new DelayQueue<>();

    /**
     * 如果该方法返回值小于等于0，该元素才能从延时队列中出队
     */
    @Override
    public long getDelay(TimeUnit unit) {
        return timeOut - System.currentTimeMillis();
    }

    /**
     * 对所有任务进行排序
     * @param o
     * @return
     */
    @Override
    public int compareTo(Delayed o) {
        return (int)(this.timeOut - ((DelayTask)o).timeOut);
    }

    /**
     *
     * @param task
     * @param delay 延迟执行时间
     * @param period 周期执行时间
     * @param timeUnit
     */
    public void scheduled(Runnable task, Long delay, Long period, TimeUnit timeUnit){
        //toMillis()转换成毫秒值
        try {
            this.timeOut = System.currentTimeMillis() + timeUnit.toMillis(delay);
            int i = 1;
            while (true){
                if(i != 1){
                    //在之前的执行时间上加period周期时间
                    this.timeOut = this.timeOut + timeUnit.toMillis(period);
                }
                delayQueue.put(this);
                delayQueue.take();
                new Thread(task).start();
                i++;
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
