package com.mall.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice // 全局异常处理器，自动捕获所有 Controller 抛出的异常
@Slf4j // Lombok 注解，自动生成 log 日志对象
public class GlobalExceptionHandler { // 全局异常处理器类
//
    /**
     * 处理参数校验异常（@Valid）
     * 当使用 @Valid 注解校验参数失败时，抛出 MethodArgumentNotValidException
     */
    @ExceptionHandler(MethodArgumentNotValidException.class) // 指定处理的异常类型
    @ResponseStatus(HttpStatus.BAD_REQUEST) // 设置响应状态码为 400（客户端错误）
    public Result<?> handleValidException(MethodArgumentNotValidException e) { // 异常处理方法
        
        // 获取所有字段错误信息，拼接成字符串
        String message = e.getBindingResult().getFieldErrors().stream() // 获取绑定结果中的所有字段错误，转为流
                
                .map(error -> error.getField() + ": " + error.getDefaultMessage()) // 拼接字段名和错误消息
                
                .reduce((a, b) -> a + "; " + b) // 用分号连接多个错误信息
                
                .orElse("参数校验失败"); // 如果没有错误信息，返回默认提示
        
        log.warn("参数校验失败: {}", message); // 记录警告日志
        
        return Result.error(400, message); // 返回错误结果，状态码 400
        
    }
    
    /**
     * 处理参数绑定异常
     * 当表单提交的数据类型与实体类字段类型不匹配时，抛出 BindException
     */
    @ExceptionHandler(BindException.class) // 指定处理的异常类型
        
    @ResponseStatus(HttpStatus.BAD_REQUEST) // 设置响应状态码为 400（客户端错误）
        
    public Result<?> handleBindException(BindException e) { // 异常处理方法
        
        // 获取所有字段错误信息，拼接成字符串
        String message = e.getBindingResult().getFieldErrors().stream() // 获取绑定结果中的所有字段错误，转为流
                
                .map(error -> error.getField() + ": " + error.getDefaultMessage()) // 拼接字段名和错误消息
                
                .reduce((a, b) -> a + "; " + b) // 用分号连接多个错误信息
                
                .orElse("参数绑定失败"); // 如果没有错误信息，返回默认提示
        
        log.warn("参数绑定失败: {}", message); // 记录警告日志
        
        return Result.error(400, message); // 返回错误结果，状态码 400
        
    }
    
    
    /**
     * 处理缺少请求参数异常
     * 当请求中缺少必需的参数时，抛出 MissingServletRequestParameterException
     */
    @ExceptionHandler(MissingServletRequestParameterException.class) // 指定处理的异常类型
        
    @ResponseStatus(HttpStatus.BAD_REQUEST) // 设置响应状态码为 400（客户端错误）
        
    public Result<?> handleMissingParam(MissingServletRequestParameterException e) { // 异常处理方法
        
        log.warn("缺少请求参数: {}", e.getParameterName()); // 记录警告日志，输出缺少的参数名
        
        return Result.error(400, "缺少参数: " + e.getParameterName()); // 返回错误结果，提示缺少的参数名
        
    }
    
    
    /**
     * 处理非法参数异常
     * 当参数值不符合业务要求时，抛出 IllegalArgumentException
     */
    @ExceptionHandler(IllegalArgumentException.class) // 指定处理的异常类型
        
    @ResponseStatus(HttpStatus.BAD_REQUEST) // 设置响应状态码为 400（客户端错误）
        
    public Result<?> handleIllegalArgument(IllegalArgumentException e) { // 异常处理方法
        
        log.warn("非法参数: {}", e.getMessage()); // 记录警告日志，输出异常消息
        
        return Result.error(400, e.getMessage()); // 返回错误结果，状态码 400
        
    }
    
    
    /**
     * 处理业务异常（自定义异常）
     * 当业务逻辑出现问题时，抛出自定义的 BusinessException
     */
    @ExceptionHandler(BusinessException.class) // 指定处理的异常类型
        
    @ResponseStatus(HttpStatus.BAD_REQUEST) // 设置响应状态码为 400（客户端错误）
        
    public Result<?> handleBusinessException(BusinessException e) { // 异常处理方法
        
        log.warn("业务异常: {}", e.getMessage()); // 记录警告日志，输出异常消息
        
        return Result.error(e.getCode(), e.getMessage()); // 返回错误结果，使用异常中的错误码和消息
        
    }
    
    
    /**
     * 处理静态资源未找到异常（如 favicon.ico）
     * 浏览器会自动请求 favicon.ico 等静态资源，未找到时静默处理，避免打印完整错误堆栈
     */
    @ExceptionHandler(NoResourceFoundException.class) // 指定处理的异常类型
    @ResponseStatus(HttpStatus.NOT_FOUND) // 设置响应状态码为 404（资源不存在）
    public Result<?> handleNoResourceFound(NoResourceFoundException e) { // 异常处理方法
        
        log.debug("静态资源未找到: {}", e.getResourcePath()); // 使用 debug 级别记录，不打印堆栈
        
        return Result.error(404, "资源不存在"); // 返回 404 错误结果
        
    }
    
    
    /**
     * 兜底异常处理：处理所有其他未被捕获的异常
     * 当系统出现未预期的异常时，返回统一的错误提示
     */
    @ExceptionHandler(Exception.class) // 指定处理的异常类型（所有异常）
        
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR) // 设置响应状态码为 500（服务器内部错误）
        
    public Result<?> handleException(Exception e) { // 异常处理方法
        
        log.error("系统异常", e); // 记录错误日志，输出完整的异常堆栈信息
        
        return Result.error(500, "系统繁忙，请稍后重试"); // 返回错误结果，提示用户稍后重试
        
    }
}
