package com.zack.zojbackendjudgeservice.judge.strategy;

import com.zack.zojbackendmodel.codeSandbox.JudgeInfo;
import com.zack.zojbackendmodel.dto.question.JudgeCase;
import com.zack.zojbackendmodel.entity.Question;
import com.zack.zojbackendmodel.entity.QuestionSubmit;
import lombok.Data;

import java.util.List;

/**
 * 上下文（用于定义在策略中传递的参数
 */
@Data
public class JudgeContext {

    private JudgeInfo judgeInfo;

    private List<String> inputList;

    private List<String> outputList;

    private List<JudgeCase> judgeCaseList;

    private Question question;

    private QuestionSubmit questionSubmit;
}
