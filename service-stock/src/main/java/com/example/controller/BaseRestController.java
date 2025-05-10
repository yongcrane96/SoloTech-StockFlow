package com.example.controller;

import com.example.response.ErrorResponse;
import com.example.response.SuccessCodeResponse;
import com.example.response.SuccessResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class BaseRestController {
    protected ResponseEntity<?> getResponse(HttpStatus httpStatus, Object result, int size) {
        if (size >= 0)
            log.info("httpStatus={}, result={}", httpStatus, (result != null) ? result : "null");
        else
            log.info("httpStatus={}, result={}", httpStatus, size);

        return new ResponseEntity<>(result, httpStatus);
    }

    protected ResponseEntity<?> getSuccessResponse(HttpStatus httpStatus, Object result, int size) {
        SuccessResponse response = new SuccessResponse();
        response.setData(result);
        return getResponse(httpStatus, response, size);
    }

    protected ResponseEntity<?> getSuccessCodeResponse(HttpStatus httpStatus, String responseCode, Object result, int size) {
        if (result == null) {
            Map<String, String> simpleResponse = new HashMap<String, String>();
            simpleResponse.put("responseCode", responseCode);
            return getResponse(httpStatus, simpleResponse, size);
        }

        SuccessCodeResponse response = new SuccessCodeResponse();
        response.setResponseCode(responseCode);
        response.setData(result);
        return getResponse(httpStatus, response, size);
    }

    protected ResponseEntity<?> getOkResponse(Object result) {
        return getSuccessResponse(HttpStatus.OK, result, -1);
    }

    protected ResponseEntity<?> getOkResponse(String responseCode, Object result) {
        return getSuccessCodeResponse(HttpStatus.OK, responseCode, result, -1);
    }

    protected ResponseEntity<?> getOkResponse(Map resultMap) {
        return getSuccessResponse(HttpStatus.OK, resultMap, resultMap.size());
    }

    protected ResponseEntity<?> getOkListResponse(List resultList) {
        int size = 0;
        if (resultList != null)
            size = resultList.size();

        return getSuccessResponse(HttpStatus.OK, resultList, size);
    }

    protected ResponseEntity<?> getCreatedResponse(Object result) {
        return getSuccessResponse(HttpStatus.CREATED, result, -1);
    }

    protected ResponseEntity<?> getErrorResponse(String errorMessage) {
        log.error(errorMessage);
        ErrorResponse response = new ErrorResponse();
        response.setMessage(errorMessage);
        return getResponse(HttpStatus.BAD_REQUEST, response, -1);
    }
}
