package com.zack.zojbackendjudgeservice.judge;

import cn.hutool.json.JSONUtil;

import com.zack.zojbackendjudgeservice.judge.codesandbox.CodeSandbox;
import com.zack.zojbackendjudgeservice.judge.codesandbox.CodeSandboxFactory;
import com.zack.zojbackendjudgeservice.judge.codesandbox.CodeSandboxProxy;
import com.zack.zojbackendjudgeservice.judge.strategy.JudgeContext;
import com.zack.zojbackendmodel.codeSandbox.ExecuteCodeRequest;
import com.zack.zojbackendmodel.codeSandbox.ExecuteCodeResponse;
import com.zack.zojbackendmodel.codeSandbox.JudgeInfo;
import com.zack.zojbackendmodel.common.ErrorCode;
import com.zack.zojbackendmodel.dto.question.JudgeCase;
import com.zack.zojbackendmodel.entity.Question;
import com.zack.zojbackendmodel.entity.QuestionSubmit;
import com.zack.zojbackendmodel.entity.UserRank;
import com.zack.zojbackendmodel.enums.QuestionSubmitEnum;
import com.zack.zojbackendmodel.exception.BusinessException;
import com.zack.zojbackendserviceclient.service.QuestionFeignClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class JudgeServiceImpl implements JudgeService {

    @Resource
    private QuestionFeignClient questionFeignClient;


    @Resource
    private JudgeManager judgeManager;

    @Value("${codesandbox.type:example}")
    private String type;


    @Override
    public QuestionSubmit doJudge(long questionSubmitId) {
        // 1）传入题目的提交 id，获取到对应的题目、提交信息（包含代码、编程语言等）
        QuestionSubmit questionSubmit = questionFeignClient.getQuestionSubmitById(questionSubmitId);
        if (questionSubmit == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "提交信息不存在");
        }
        Long questionId = questionSubmit.getQuestionId();
        Question question = questionFeignClient.getQuestionById(questionId);
        if (question == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "题目不存在");
        }
        // 2）如果题目提交状态不为等待中，就不用重复执行了
        if (!questionSubmit.getStatus().equals(QuestionSubmitEnum.WATITING.getValue())) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "题目正在评测中");
        }
        // 3）更改判题（题目提交）的状态为 “判题中”，防止重复执行
        QuestionSubmit questionSubmitUpdate = new QuestionSubmit();
        questionSubmitUpdate.setId(questionSubmitId);
        questionSubmitUpdate.setStatus(QuestionSubmitEnum.RUNNING.getValue());
        boolean update = questionFeignClient.updateQuestionSubmitById(questionSubmitUpdate);
        if (!update) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "题目状态更新错误");
        }
        // 4）调用沙箱，获取到执行结果
        CodeSandbox codeSandbox = CodeSandboxFactory.newInstance(type);
        codeSandbox = new CodeSandboxProxy(codeSandbox);
        String language = questionSubmit.getLanguage();
        String code = questionSubmit.getCode();
        // 获取输入用例
        String judgeCaseStr = question.getJudgeCase();
        List<JudgeCase> judgeCaseList = JSONUtil.toList(judgeCaseStr, JudgeCase.class);
        List<String> inputList = judgeCaseList.stream().map(JudgeCase::getInput).collect(Collectors.toList());
        ExecuteCodeRequest executeCodeRequest = ExecuteCodeRequest.builder()
                .code(code)
                .language(language)
                .inputList(inputList)
                .build();
        ExecuteCodeResponse executeCodeResponse = codeSandbox.executeCode(executeCodeRequest);
        List<String> outputList = executeCodeResponse.getOutputList();
        // 5）根据沙箱的执行结果，设置题目的判题状态和信息
        JudgeContext judgeContext = new JudgeContext();
        judgeContext.setJudgeInfo(executeCodeResponse.getJudgeInfo());
        judgeContext.setInputList(inputList);
        judgeContext.setOutputList(outputList);
        judgeContext.setJudgeCaseList(judgeCaseList);
        judgeContext.setQuestion(question);
        judgeContext.setQuestionSubmit(questionSubmit);
        JudgeInfo judgeInfo = judgeManager.doJudge(judgeContext);
        // 6）修改数据库中的判题结果
        questionSubmitUpdate = new QuestionSubmit();
        questionSubmitUpdate.setId(questionSubmitId);
        questionSubmitUpdate.setStatus(QuestionSubmitEnum.SUCCEED.getValue());
        questionSubmitUpdate.setJudgeInfo(JSONUtil.toJsonStr(judgeInfo));
        update = questionFeignClient.updateQuestionSubmitById(questionSubmitUpdate);
        if (!update) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "题目状态更新错误");
        }
        QuestionSubmit questionSubmitResult = questionFeignClient.getQuestionSubmitById(questionSubmitId);

        judgeInfo = JSONUtil.toBean(questionSubmitResult.getJudgeInfo(), JudgeInfo.class);
            if ("Accepted".equals(judgeInfo.getMessage())) {
                // 增加用户Ac数
                UserRank acUpdateUserRank = new UserRank();
                acUpdateUserRank.setId(questionFeignClient.getUserRankIdByUserId(questionSubmitResult.getUserId()));
                acUpdateUserRank.setAcNum(questionFeignClient.getUserRankUserACNumByUserId(questionSubmitResult.getUserId()) + 1);
                questionFeignClient.updateUserRank(acUpdateUserRank);
                // 增加题目Ac数
                question.setAcceptNum(question.getAcceptNum() + 1);
                questionFeignClient.updateQuestion(question);
            }

        return questionSubmitResult;
    }
}
