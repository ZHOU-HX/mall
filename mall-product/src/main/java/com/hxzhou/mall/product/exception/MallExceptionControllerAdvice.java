package com.hxzhou.mall.product.exception;

import com.hxzhou.common.exception.BizCodeEnum;
import com.hxzhou.common.utils.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * 集中处理所有异常
 */
@Slf4j
//@ResponseBody
//@ControllerAdvice(basePackages = "com.hxzhou.mall.product.controller")
@RestControllerAdvice(basePackages = "com.hxzhou.mall.product.controller")
public class MallExceptionControllerAdvice {

    /**
     * 数据校验的异常
     * @param e
     * @return
     */
    @ExceptionHandler(value = MethodArgumentNotValidException.class)
    public R handleVaildException(MethodArgumentNotValidException e) {
//        log.error("数据校验出现问题:{}，异常类型:{}", e.getMessage(), e.getClass());

        BindingResult bindingResult = e.getBindingResult();
        Map<String, String> errorMap = new HashMap<>();

        // 获取校验的错误结果
        bindingResult.getFieldErrors().forEach((fieldError) -> {
            // FieldError获取到错误提示
            String message = fieldError.getDefaultMessage();
            // 获取错误的属性名字
            String field = fieldError.getField();

            errorMap.put(field, message);
        });

        return R.error(BizCodeEnum.VAILD_EXCEPTION.getCode(), BizCodeEnum.VAILD_EXCEPTION.getMsg()).put("data", errorMap);
    }

    /**
     * 总异常：没有别的异常能处理了，它就来处理了
     * @param e
     * @return
     */
    @ExceptionHandler(value = Throwable.class)
    public R handleException(Throwable e) {
        log.error(e.getMessage());
        return R.error(BizCodeEnum.UNKNOW_EXCEPTION.getCode(), BizCodeEnum.UNKNOW_EXCEPTION.getMsg());
    }
}
