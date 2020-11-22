package com.atguigu.gmall.schedule.jobhandler;

import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.handler.annotation.XxlJob;
import com.xxl.job.core.log.XxlJobLogger;
import org.springframework.stereotype.Component;

@Component
public class MyJobHandler {

    /**
     * 方法格式要求 public ReturnT<String> execute(String param)
     * @param param 接收调度中心传过来的参数
     * @return
     */
    @XxlJob("myJobHandler") // myJobHandler：唯一标识
    public ReturnT<String> handler(String param){
        System.out.println("定时任务，参数是：" + param);
        //向调度中心输出日志
        XxlJobLogger.log("向调度中心输出日志");
        return ReturnT.SUCCESS;
    }
}
