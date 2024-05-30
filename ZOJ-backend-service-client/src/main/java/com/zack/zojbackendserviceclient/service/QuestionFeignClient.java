package com.zack.zojbackendserviceclient.service;


import com.zack.zojbackendmodel.entity.Question;
import com.zack.zojbackendmodel.entity.QuestionSubmit;
import com.zack.zojbackendmodel.entity.UserRank;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

/**
* @author admin
* @description 针对表【question(题目)】的数据库操作Service
* @createDate 2024-03-29 10:20:48
*/
@FeignClient(name = "ZOJ-backend-question-service", path = "/api/question/inner")
public interface QuestionFeignClient {

    @GetMapping("/get/id")
    Question getQuestionById(@RequestParam("questionId") long questionId);

    @PostMapping("/update")
    boolean updateQuestion(@RequestBody Question question);

    @GetMapping("/question_submit/get/id")
    QuestionSubmit getQuestionSubmitById(@RequestParam("questionId") long questionSubmitId);

    @PostMapping("/question_submit/update")
    boolean updateQuestionSubmitById(@RequestBody QuestionSubmit questionSubmit);

    @GetMapping("/userRank/getIdByUserId")
    Long getUserRankIdByUserId(@RequestParam("userId") long userId);

    @GetMapping("/userRank/getACNumByUserId")
    Integer getUserRankUserACNumByUserId(@RequestParam("userId") long userId);

    @PostMapping("/userRank/update")
    boolean updateUserRank(@RequestBody UserRank userRank);

}
