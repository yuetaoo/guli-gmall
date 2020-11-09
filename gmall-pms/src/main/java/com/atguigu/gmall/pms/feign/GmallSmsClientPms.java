package com.atguigu.gmall.pms.feign;

import com.atguigu.gmall.sms.api.GmallSmsClient;
import org.springframework.cloud.openfeign.FeignClient;

@FeignClient("sms-service")
public interface GmallSmsClientPms extends GmallSmsClient {

}
