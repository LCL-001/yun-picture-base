package com.lcl.yunpicturebackend;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@SpringBootApplication
@MapperScan("com.lcl.yunpicturebackend.mapper")
@EnableAspectJAutoProxy(exposeProxy = true)
public class YunPictureBaseApplication {

    public static void main(String[] args) {
        SpringApplication.run(YunPictureBaseApplication.class, args);
    }

}
