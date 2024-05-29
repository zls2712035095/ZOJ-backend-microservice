package com.zack.zojbackendjudgeservice.judge;


import com.zack.zojbackendjudgeservice.judge.strategy.DefaultJudgeStrategy;
import com.zack.zojbackendjudgeservice.judge.strategy.JavaLanguageJudgeStrategy;
import com.zack.zojbackendjudgeservice.judge.strategy.JudgeContext;
import com.zack.zojbackendjudgeservice.judge.strategy.JudgeStrategy;
import com.zack.zojbackendmodel.codeSandbox.JudgeInfo;
import com.zack.zojbackendmodel.entity.QuestionSubmit;
import org.springframework.stereotype.Service;

/**
 * 判题管理
 */
@Service
public class JudgeManager {

    /**
     * 执行判题
     *
     * @param judgeContext
     * @return
     */
    JudgeInfo doJudge(JudgeContext judgeContext) {
        QuestionSubmit questionSubmit = judgeContext.getQuestionSubmit();
        String language = questionSubmit.getLanguage();
        JudgeStrategy judgeStrategy = new DefaultJudgeStrategy();
        if ("java".equals(language)) {
            judgeStrategy = new JavaLanguageJudgeStrategy();
        }
        return judgeStrategy.doJudge(judgeContext);
    }
}
