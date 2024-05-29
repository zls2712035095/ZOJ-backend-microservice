package com.zack.zojbackendjudgeservice.judge.codesandbox.impl;


import com.zack.zojbackendjudgeservice.judge.codesandbox.CodeSandbox;
import com.zack.zojbackendmodel.codeSandbox.ExecuteCodeRequest;
import com.zack.zojbackendmodel.codeSandbox.ExecuteCodeResponse;

/**
 * 第三方代码沙箱（调用现成接口的沙箱）
 */
public class ThirdPartyCodeSandbox implements CodeSandbox {
    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) {
        System.out.println("ThirdPartyCodeSandbox");
        return null;
    }
}
