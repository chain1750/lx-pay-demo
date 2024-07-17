package com.lx.pay.controller;

import com.lx.pay.model.IResult;
import com.lx.pay.service.TaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 定时任务接口<br>
 * 除了通过回调方式得知支付结果，还需要通过轮询来保证支付结果能够正常变更。<br>
 * 这里定义接口是为了给到定时任务服务（如XXL-Job）去调用，支付服务本身不设置定时任务。
 *
 * @author chenhaizhuang
 */
@RestController
@RequestMapping("/task")
public class TaskController {

    @Autowired
    private TaskService taskService;

    /**
     * 处理未支付
     *
     * @return IResult
     */
    @PostMapping("/handleNotPay")
    public IResult<Void> handleNotPay() {
        taskService.handleNotPay();
        return IResult.success();
    }

    /**
     * 处理退款中
     *
     * @return IResult
     */
    @PostMapping("/handleInRefund")
    public IResult<Void> handleInRefund() {
        taskService.handleInRefund();
        return IResult.success();
    }
}
