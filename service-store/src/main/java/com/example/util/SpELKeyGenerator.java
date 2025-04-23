package com.example.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.lang.reflect.Method;

@Slf4j
public class SpELKeyGenerator {

    private static final SpelExpressionParser parser = new SpelExpressionParser();
    private static final ParameterNameDiscoverer discoverer = new DefaultParameterNameDiscoverer();

    public static String generateKey(String keySpEL, Method method, Object[] args){
        EvaluationContext evalContext = new StandardEvaluationContext();

        String[] paramNames = discoverer.getParameterNames(method);

        if(paramNames != null){
            for (int i = 0; i < paramNames.length; i++) {
                evalContext.setVariable(paramNames[i], args[i]);
            }
        }
        try{
            return  parser.parseExpression(keySpEL).getValue(evalContext, String.class);
        } catch (Exception e){
            log.error("SpEL 파싱 오류 - key: {}, error: {}", keySpEL, e.getMessage());
            throw new RuntimeException("캐시 키 생성 실패", e);
        }
    }
}
