package com.lcl.yunpicturebackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
@EnableAspectJAutoProxy(exposeProxy = true)
public class YunPictureBaseApplication {

    public static void main(String[] args) {
        SpringApplication.run(YunPictureBaseApplication.class, args);
    }

}
