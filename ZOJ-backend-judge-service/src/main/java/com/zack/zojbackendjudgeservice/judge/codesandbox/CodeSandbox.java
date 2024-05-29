package com.zack.zojbackendjudgeservice.judge.codesandbox;


import com.zack.zojbackendmodel.codeSandbox.ExecuteCodeRequest;
import com.zack.zojbackendmodel.codeSandbox.ExecuteCodeResponse;

/**
 * 代码沙箱接口定义
 */
public interface CodeSandbox {

    /**
     *  执行代码
     *
     * @param executeCodeRequest
     * @return
     */
    ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest);
}
