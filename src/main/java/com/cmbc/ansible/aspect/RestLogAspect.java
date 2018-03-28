package com.cmbc.ansible.aspect;

import lombok.extern.log4j.Log4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;

/**
 * Created by rtdl-lixing on 2017/11/29.
 */

@Aspect
@Service
@Log4j
public class RestLogAspect {
    ThreadLocal<Long> startTime = new ThreadLocal<>();

    @Pointcut("execution(public * com.cmbc.ansible.web.rest.AnsibleRestful.*(..))")
    public void restServiceLog() {
    }

    @Before("restServiceLog()")
    public void doBefore(JoinPoint joinPoint) throws Throwable {
        startTime.set(System.currentTimeMillis());
        //接收到请求，记录请求内容
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes.getRequest();
        //记录请求内容
        log.info("==============Controller Request================");
        log.info("URL : " + request.getRequestURL().toString());
        log.info("Http_method : " + request.getMethod());
        log.info("IP : " + request.getRemoteAddr());
        log.info("Class_method : " + joinPoint.getSignature().getDeclaringTypeName() + "." + joinPoint.getSignature().getName());
        log.info("Args : " + Arrays.toString(joinPoint.getArgs()));
    }

    @AfterReturning(returning = "ret", pointcut = "restServiceLog()")
    public void doAfterReturning(Object ret) throws Throwable {
        //处理玩请求，返回内容
        log.info("==============Controller Response=================");
        log.info("Spend Time : " + (System.currentTimeMillis() - startTime.get()));
        log.info("Response : " + ret);
    }
} 
