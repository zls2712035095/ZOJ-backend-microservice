package com.zack.zojbackendquestionservice;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableDiscoveryClient
@EnableFeignClients(basePackages = {"com.zack.zojbackendserviceclient.service"})
@SpringBootApplication
@MapperScan("com.zack.zojbackendquestionservice.mapper")
@EnableScheduling
@EnableAspectJAutoProxy(proxyTargetClass = true, exposeProxy = true)
@ComponentScan("com.zack")
public class ZojBackendQuestionServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ZojBackendQuestionServiceApplication.class, args);
    }

    @Bean
    TomcatServletWebServerFactory servletContainer() {
        return new TomcatServletWebServerFactory(8103);
    }
}
