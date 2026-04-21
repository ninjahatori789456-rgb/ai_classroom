package com.remoteclassroom.backend.config;

import java.util.HashMap;
import java.util.Map;

import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import com.fasterxml.jackson.databind.ObjectMapper;

@RestControllerAdvice(basePackages = "com.remoteclassroom.backend.controller")
public class GlobalResponseWrapper implements ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
            Class<? extends HttpMessageConverter<?>> selectedConverterType, ServerHttpRequest request,
            ServerHttpResponse response) {
            
        String path = request.getURI().getPath();
        if (path.startsWith("/api/health") || path.startsWith("/swagger") || path.startsWith("/v3/api-docs")) {
            return body;
        }

        if (body instanceof Map) {
            Map<?, ?> mapBody = (Map<?, ?>) body;
            if (mapBody.containsKey("success") && mapBody.containsKey("data")) {
                return body; // Already standardized
            }
            if (mapBody.containsKey("status") && mapBody.containsKey("error")) {
                return body; // Spring error, leave for exception handler
            }
        }

        Map<String, Object> wrapped = new HashMap<>();
        wrapped.put("success", true);
        wrapped.put("data", body);
        wrapped.put("message", "Success");
        
        if (body instanceof String) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                return mapper.writeValueAsString(wrapped);
            } catch (Exception e) {
                return body; // Fallback
            }
        }
        
        return wrapped;
    }
}
