package com.zack.zojbackendjudgeservice.judge.strategy;


import com.zack.zojbackendmodel.codeSandbox.JudgeInfo;

/**
 * 判题策略
 */
public interface JudgeStrategy {

    /**
     * 执行判题
     * @param judgeContext
     * @return
     */
    JudgeInfo doJudge(JudgeContext judgeContext);
}
