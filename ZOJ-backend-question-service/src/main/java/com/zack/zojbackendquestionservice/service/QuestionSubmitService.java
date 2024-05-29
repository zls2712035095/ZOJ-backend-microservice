package com.zack.zojbackendquestionservice.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zack.zojbackendmodel.dto.questionsubmit.QuestionSubmitAddRequest;
import com.zack.zojbackendmodel.dto.questionsubmit.QuestionSubmitQueryRequest;
import com.zack.zojbackendmodel.entity.QuestionSubmit;
import com.zack.zojbackendmodel.entity.User;
import com.zack.zojbackendmodel.vo.QuestionSubmitVO;


/**
* @author admin
* @description 针对表【question_submit(题目提交)】的数据库操作Service
* @createDate 2024-03-29 10:21:55
*/
public interface QuestionSubmitService extends IService<QuestionSubmit> {
    /**
     * 提交
     *
     * @param questionSubmitAddRequest
     * @param loginUser
     * @return
     */
    long doQuestionSubmit(QuestionSubmitAddRequest questionSubmitAddRequest, User loginUser);

    /**
     * 获取查询条件
     *
     * @param questionSubmitQueryRequest
     * @return
     */
    QueryWrapper<QuestionSubmit> getQueryWrapper(QuestionSubmitQueryRequest questionSubmitQueryRequest);

    /**
     * 获取题目封装
     *
     * @param questionSubmit
     * @param loginUSer
     * @return
     */
    QuestionSubmitVO getQuestionSubmitVO(QuestionSubmit questionSubmit, User loginUSer);

    /**
     * 分页获取题目封装
     *
     * @param questionSubmitPage
     * @param loginUSer
     * @return
     */
    Page<QuestionSubmitVO> getQuestionSubmitVOPage(Page<QuestionSubmit> questionSubmitPage, User loginUSer);
//    /**
//     * 题目提交（内部服务）
//     *
//     * @param userId
//     * @param questionId
//     * @return
//     */
//    int doQuestionSubmitInner(long userId, long questionId);
}
