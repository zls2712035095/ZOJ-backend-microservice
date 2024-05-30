package com.zack.zojbackendquestionservice.controller.inner;


import com.zack.zojbackendmodel.entity.Question;
import com.zack.zojbackendmodel.entity.QuestionSubmit;
import com.zack.zojbackendmodel.entity.UserRank;
import com.zack.zojbackendquestionservice.service.QuestionService;
import com.zack.zojbackendquestionservice.service.QuestionSubmitService;
import com.zack.zojbackendquestionservice.service.UserRankService;
import com.zack.zojbackendserviceclient.service.QuestionFeignClient;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * 该服务仅内部调用，不是给前端的
 */
@RestController
@RequestMapping("/inner")
public class QuestionInnerController implements QuestionFeignClient {

    @Resource
    private QuestionService questionService;

    @Resource
    private QuestionSubmitService questionSubmitService;

    @Resource
    private UserRankService userRankService;

    @GetMapping("/get/id")
    @Override
    public Question getQuestionById(@RequestParam("questionId") long questionId) {
        return questionService.getById(questionId);
    }

    @PostMapping("/update")
    @Override
    public boolean updateQuestion(@RequestBody Question question){

        return questionService.updateById(question);
    }

    @GetMapping("/question_submit/get/id")
    @Override
    public QuestionSubmit getQuestionSubmitById(@RequestParam("questionId") long questionSubmitId) {
        return questionSubmitService.getById(questionSubmitId);
    }

    @PostMapping("/question_submit/update")
    @Override
    public boolean updateQuestionSubmitById(@RequestBody QuestionSubmit questionSubmit) {
        return questionSubmitService.updateById(questionSubmit);
    }

    @GetMapping("/userRank/getIdByUserId")
    @Override
    public Long getUserRankIdByUserId(@RequestParam("userId") long userId) {
        return userRankService.getUserRankIdByUserId(userId);
    }

    @GetMapping("/userRank/getACNumByUserId")
    @Override
    public Integer getUserRankUserACNumByUserId(@RequestParam("userId") long userId) {
        return userRankService.getUserRankAcNum(userId);
    }

    @PostMapping("/userRank/update")
    @Override
    public boolean updateUserRank(@RequestBody UserRank userRank) {
        return userRankService.updateById(userRank);
    }

}
