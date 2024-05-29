package com.zack.zojbackendquestionservice.controller;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.zack.zojbackendmodel.annotation.AuthCheck;
import com.zack.zojbackendmodel.common.BaseResponse;
import com.zack.zojbackendmodel.common.DeleteRequest;
import com.zack.zojbackendmodel.common.ErrorCode;
import com.zack.zojbackendmodel.common.ResultUtils;
import com.zack.zojbackendmodel.constant.UserConstant;
import com.zack.zojbackendmodel.dto.comment.*;
import com.zack.zojbackendmodel.dto.question.*;
import com.zack.zojbackendmodel.dto.questionlist.*;
import com.zack.zojbackendmodel.dto.questionsubmit.QuestionSubmitAddRequest;
import com.zack.zojbackendmodel.dto.questionsubmit.QuestionSubmitQueryRequest;
import com.zack.zojbackendmodel.dto.userrank.UserRankQueryRequest;
import com.zack.zojbackendmodel.entity.*;
import com.zack.zojbackendmodel.exception.BusinessException;
import com.zack.zojbackendmodel.exception.ThrowUtils;
import com.zack.zojbackendmodel.vo.QuestionListVO;
import com.zack.zojbackendmodel.vo.QuestionSubmitVO;
import com.zack.zojbackendmodel.vo.QuestionVO;
import com.zack.zojbackendquestionservice.service.*;
import com.zack.zojbackendserviceclient.service.UserFeignClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 题目接口
 */
@RestController
@RequestMapping("/")
@Slf4j
public class QuestionController {

    @Resource
    private QuestionService questionService;

    @Resource
    private UserFeignClient userFeignClient;

    @Resource
    private UserRankService userRankService;


    /**
     * 创建
     *
     * @param questionAddRequest
     * @param request
     * @return
     */
    @PostMapping("/add")
    public BaseResponse<Long> addQuestion(@RequestBody QuestionAddRequest questionAddRequest, HttpServletRequest request) {
        if (questionAddRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Question question = new Question();
        BeanUtils.copyProperties(questionAddRequest, question);
        List<String> tags = questionAddRequest.getTags();
        if (tags != null) {
            question.setTags(JSONUtil.toJsonStr(tags));
        }
        List<JudgeCase> judgeCases = questionAddRequest.getJudgeCase();
        if (judgeCases != null) {
            question.setJudgeCase(JSONUtil.toJsonStr(judgeCases));
        }
        JudgeConfig judgeConfig = questionAddRequest.getJudgeConfig();
        if (judgeConfig != null) {
            question.setJudgeConfig(JSONUtil.toJsonStr(judgeConfig));
        }
        questionService.validQuestion(question, true);
        User loginUser = userFeignClient.getLoginUser(request);
        question.setUserId(loginUser.getId());
        question.setFavourNum(0);
        question.setThumbNum(0);
        boolean result = questionService.save(question);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        long newQuestionId = question.getId();
        return ResultUtils.success(newQuestionId);
    }

    /**
     * 删除
     *
     * @param deleteRequest
     * @param request
     * @return
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteQuestion(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = userFeignClient.getLoginUser(request);
        long id = deleteRequest.getId();
        // 判断是否存在
        Question oldQuestion = questionService.getById(id);
        ThrowUtils.throwIf(oldQuestion == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可删除
        if (!oldQuestion.getUserId().equals(user.getId()) && !userFeignClient.isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        boolean b = questionService.removeById(id);
        return ResultUtils.success(b);
    }

    /**
     * 更新（仅管理员）
     *
     * @param questionUpdateRequest
     * @return
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateQuestion(@RequestBody QuestionUpdateRequest questionUpdateRequest) {
        if (questionUpdateRequest == null || questionUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Question question = new Question();
        BeanUtils.copyProperties(questionUpdateRequest, question);
        List<String> tags = questionUpdateRequest.getTags();
        if (tags != null) {
            question.setTags(JSONUtil.toJsonStr(tags));
        }
        List<JudgeCase> judgeCases = questionUpdateRequest.getJudgeCase();
        if (judgeCases != null) {
            question.setJudgeCase(JSONUtil.toJsonStr(judgeCases));
        }
        JudgeConfig judgeConfig = questionUpdateRequest.getJudgeConfig();
        if (judgeConfig != null) {
            question.setJudgeConfig(JSONUtil.toJsonStr(judgeConfig));
        }
        // 参数校验
        questionService.validQuestion(question, false);
        long id = questionUpdateRequest.getId();
        // 判断是否存在
        Question oldQuestion = questionService.getById(id);
        ThrowUtils.throwIf(oldQuestion == null, ErrorCode.NOT_FOUND_ERROR);
        boolean result = questionService.updateById(question);
        return ResultUtils.success(result);
    }

    /**
     * 根据 id 获取（脱敏）
     *
     * @param id
     * @return
     */
    @GetMapping("/get/vo")
    public BaseResponse<QuestionVO> getQuestionVOById(long id, HttpServletRequest request) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Question question = questionService.getById(id);
        if (question == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        return ResultUtils.success(questionService.getQuestionVO(question, request));
    }

    /**
     * 根据 id 获取
     *
     * @param id
     * @return
     */
    @GetMapping("/get")
    public BaseResponse<Question> getQuestionById(long id, HttpServletRequest request) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Question question = questionService.getById(id);
        if (question == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        User loginUser = userFeignClient.getLoginUser(request);
        //不是本人或管理员, 不能获取全部数据
        if(!question.getUserId().equals(loginUser.getId()) && !userFeignClient.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        return ResultUtils.success(question);
    }
    /**
     * 分页获取列表（仅管理员）
     *
     * @param questionQueryRequest
     * @return
     */
    @PostMapping("/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<Question>> listQuestionByPage(@RequestBody QuestionQueryRequest questionQueryRequest) {
        long current = questionQueryRequest.getCurrent();
        long size = questionQueryRequest.getPageSize();
        Page<Question> questionPage = questionService.page(new Page<>(current, size),
                questionService.getQueryWrapper(questionQueryRequest));
        return ResultUtils.success(questionPage);
    }

    /**
     * 分页获取列表（封装类）
     *
     * @param questionQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/list/page/vo")
    public BaseResponse<Page<QuestionVO>> listQuestionVOByPage(@RequestBody QuestionQueryRequest questionQueryRequest,
            HttpServletRequest request) {
        long current = questionQueryRequest.getCurrent();
        long size = questionQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        Page<Question> questionPage = questionService.page(new Page<>(current, size),
                questionService.getQueryWrapper(questionQueryRequest));
        return ResultUtils.success(questionService.getQuestionVOPage(questionPage, request));
    }

    /**
     * 分页获取当前用户创建的资源列表
     *
     * @param questionQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/my/list/page/vo")
    public BaseResponse<Page<QuestionVO>> listMyQuestionVOByPage(@RequestBody QuestionQueryRequest questionQueryRequest,
            HttpServletRequest request) {
        if (questionQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userFeignClient.getLoginUser(request);
        questionQueryRequest.setUserId(loginUser.getId());
        long current = questionQueryRequest.getCurrent();
        long size = questionQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        Page<Question> questionPage = questionService.page(new Page<>(current, size),
                questionService.getQueryWrapper(questionQueryRequest));
        return ResultUtils.success(questionService.getQuestionVOPage(questionPage, request));
    }

    // endregion

//    /**
//     * 分页搜索（从 ES 查询，封装类）
//     *
//     * @param questionQueryRequest
//     * @param request
//     * @return
//     */
//    @PostMapping("/search/page/vo")
//    public BaseResponse<Page<QuestionVO>> searchQuestionVOByPage(@RequestBody QuestionQueryRequest questionQueryRequest,
//            HttpServletRequest request) {
//        long size = questionQueryRequest.getPageSize();
//        // 限制爬虫
//        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
//        Page<Question> questionPage = questionService.searchFromEs(questionQueryRequest);
//        return ResultUtils.success(questionService.getQuestionVOPage(questionPage, request));
//    }

    /**
     * 编辑（用户）
     *
     * @param questionEditRequest
     * @param request
     * @return
     */
    @PostMapping("/edit")
    public BaseResponse<Boolean> editQuestion(@RequestBody QuestionEditRequest questionEditRequest, HttpServletRequest request) {
        if (questionEditRequest == null || questionEditRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Question question = new Question();
        BeanUtils.copyProperties(questionEditRequest, question);
        List<String> tags = questionEditRequest.getTags();
        if (tags != null) {
            question.setTags(JSONUtil.toJsonStr(tags));
        }
        List<JudgeCase> judgeCases = questionEditRequest.getJudgeCase();
        if (judgeCases != null) {
            question.setJudgeCase(JSONUtil.toJsonStr(judgeCases));
        }
        JudgeConfig judgeConfig = questionEditRequest.getJudgeConfig();
        if (judgeConfig != null) {
            question.setJudgeConfig(JSONUtil.toJsonStr(judgeConfig));
        }

        // 参数校验
        questionService.validQuestion(question, false);
        User loginUser = userFeignClient.getLoginUser(request);
        long id = questionEditRequest.getId();
        // 判断是否存在
        Question oldQuestion = questionService.getById(id);
        ThrowUtils.throwIf(oldQuestion == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可编辑
        if (!oldQuestion.getUserId().equals(loginUser.getId()) && !userFeignClient.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        boolean result = questionService.updateById(question);
        return ResultUtils.success(result);
    }

    @Resource
    private QuestionSubmitService questionSubmitService;


    /**
     * 提交 / 取消提交
     *
     * @param questionSubmitAddRequest
     * @param request
     * @return resultNum 本次提交变化数
     */
    @PostMapping("/question_submit/do")
    public BaseResponse<Long> doQuestionSubmit(@RequestBody QuestionSubmitAddRequest questionSubmitAddRequest,
                                               HttpServletRequest request) {
        if (questionSubmitAddRequest == null || questionSubmitAddRequest.getQuestionId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 登录才能提交
        final User loginUser = userFeignClient.getLoginUser(request);
        long result = questionSubmitService.doQuestionSubmit(questionSubmitAddRequest, loginUser);
        return ResultUtils.success(result);
    }


    @PostMapping("/question_submit/list/page")
    public BaseResponse<Page<QuestionSubmitVO>> listQuestionSubmitByPage(@RequestBody QuestionSubmitQueryRequest questionSubmitQueryRequest, HttpServletRequest request) {
        long current = questionSubmitQueryRequest.getCurrent();
        long size = questionSubmitQueryRequest.getPageSize();

        Page<QuestionSubmit> questionSubmitPage = questionSubmitService.page(new Page<>(current, size), questionSubmitService.getQueryWrapper(questionSubmitQueryRequest));
        final User loginUser = userFeignClient.getLoginUser(request);
        return ResultUtils.success(questionSubmitService.getQuestionSubmitVOPage(questionSubmitPage, loginUser));
    }

    /**
     * 根据 id 获取questionSubmit
     *
     * @param id
     * @return
     */
    @GetMapping("/question_submit/get")
    public BaseResponse<QuestionSubmit> getQuestionSubmitById(long id, HttpServletRequest request) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        QuestionSubmit questionSubmit = questionSubmitService.getById(id);
        if (questionSubmit == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        System.out.println("查看记录"+userFeignClient.getLoginUser(request));
        return ResultUtils.success(questionSubmit);
    }
    /**
     * 根据 id 获取（脱敏）
     *
     * @param id
     * @return
     */
    @GetMapping("/question_submit/get/vo")
    public BaseResponse<QuestionSubmitVO> getQuestionSubmitVOById(long id, HttpServletRequest request) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        QuestionSubmit questionSubmit = questionSubmitService.getById(id);
        if (questionSubmit == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        User loginUser = userFeignClient.getLoginUser(request);
        return ResultUtils.success(questionSubmitService.getQuestionSubmitVO(questionSubmit, loginUser));
    }

    /**
     * 根据 userId 获取排行信息
     *
     * @return
     */
    @GetMapping("/userrank/get")
    public BaseResponse<UserRank> getUserRank(HttpServletRequest request) {
        UserRank userRank = userRankService.getUserRank(request);
        if (userRank == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        return ResultUtils.success(userRank);
    }

    @PostMapping("/userrank/list/page")
    public BaseResponse<Page<UserRank>> listUserRankByPage(@RequestBody UserRankQueryRequest userRankQueryRequest, HttpServletRequest request) {
        long current = userRankQueryRequest.getCurrent();
        long size = userRankQueryRequest.getPageSize();

        Page<UserRank> userRankPage = userRankService.page(new Page<>(current, size), userRankService.getQueryWrapper(userRankQueryRequest));
        return ResultUtils.success(userRankPage);
    }

    @Resource
    private CommentService commentService;


    /**
     * 创建或回复评论
     *
     * @param commentAddRequest
     * @param request
     * @return
     */
    @PostMapping("/comment/add")
    public BaseResponse<Long> addComment(@RequestBody CommentAddRequest commentAddRequest, HttpServletRequest request) {
        if (commentAddRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Comment comment = new Comment();
        BeanUtils.copyProperties(commentAddRequest, comment);
        comment.setThumbNum(0);
        comment.setFavourNum(0);
        User user = userFeignClient.getLoginUser(request);
        comment.setUserId(user.getId());
        comment.setUserName(user.getUserName());
        boolean result = commentService.save(comment);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(comment.getId());
    }

    /**
     * 删除
     *
     * @param deleteRequest
     * @param request
     * @return
     */
    @PostMapping("/comment/delete")
    public BaseResponse<Boolean> deleteComment(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = userFeignClient.getLoginUser(request);
        long id = deleteRequest.getId();
        // 判断是否存在
        Comment oldComment = commentService.getById(id);
        ThrowUtils.throwIf(oldComment == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可删除
        if (!oldComment.getUserId().equals(user.getId()) && !userFeignClient.isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        boolean b = commentService.removeById(id);
        return ResultUtils.success(b);
    }

    /**
     * 更新（仅管理员）
     *
     * @param commentUpdateRequest
     * @return
     */
    @PostMapping("/comment/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateComment(@RequestBody CommentUpdateRequest commentUpdateRequest) {
        if (commentUpdateRequest == null || commentUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Comment comment = new Comment();
        BeanUtils.copyProperties(commentUpdateRequest, comment);
        long id = commentUpdateRequest.getId();
        // 判断是否存在
        Comment oldComment = commentService.getById(id);
        ThrowUtils.throwIf(oldComment == null, ErrorCode.NOT_FOUND_ERROR);
        boolean result = commentService.updateById(comment);
        return ResultUtils.success(result);
    }

    /**
     * 根据 id 获取
     *
     * @param id
     * @return
     */
    @GetMapping("/comment/get")
    public BaseResponse<Comments> getCommentById(long id, HttpServletRequest request) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Comment comment = commentService.getById(id);
        Comments comments = new Comments();
        BeanUtils.copyProperties(comment, comments);
        List<Comment> chirldren = commentService.getAllByForeignId(comment.getForeignId());
        if (chirldren == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        comments.setChildrenComment(chirldren);
        if (comment == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        return ResultUtils.success(comments);
    }

    /**
     * 分页获取列表
     *
     * @param commentQueryRequest
     * @return
     */
    @PostMapping("/comment/list/comment/all/page")
    public BaseResponse<Page<Comment>> listCommentByPage(@RequestBody CommentQueryRequest commentQueryRequest) {
        long current = commentQueryRequest.getCurrent();
        long size = commentQueryRequest.getPageSize();
        Page<Comment> commentPage = commentService.page(new Page<>(current, size),
                commentService.getQueryWrapper(commentQueryRequest));
        return ResultUtils.success(commentPage);
    }

    /**
     * 分页获取列表
     *
     * @param commentQueryRequest
     * @return
     */
    @PostMapping("/comment/list/comment/root/page")
    public BaseResponse<Page<Comments>> listCommentsByPage(@RequestBody CommentQueryRequest commentQueryRequest) {
        long current = commentQueryRequest.getCurrent();
        long size = commentQueryRequest.getPageSize();
        List<Comments> comments = commentService.getCommentsPageList(commentQueryRequest.getForeignId());
        Page<Comments> commentsPage = new Page<>(current, size, comments.size());
        commentsPage.setRecords(comments);
        return ResultUtils.success(commentsPage);
    }

    /**
     * 编辑（用户）
     *
     * @param commentEditRequest
     * @param request
     * @return
     */
    @PostMapping("/comment/edit")
    public BaseResponse<Boolean> editComment(@RequestBody CommentEditRequest commentEditRequest, HttpServletRequest request) {
        if (commentEditRequest == null || commentEditRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Comment comment = new Comment();
        BeanUtils.copyProperties(commentEditRequest, comment);

        User loginUser = userFeignClient.getLoginUser(request);
        long id = commentEditRequest.getId();
        // 判断是否存在
        Comment oldComment = commentService.getById(id);
        ThrowUtils.throwIf(oldComment == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可编辑
        if (!oldComment.getUserId().equals(loginUser.getId()) && !userFeignClient.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        boolean result = commentService.updateById(oldComment);
        return ResultUtils.success(result);
    }


    @Resource
    private QuestionListService questionListService;



    /**
     * 创建
     *
     * @param questionListAddRequest
     * @param request
     * @return
     */
    @PostMapping("/questionList/add")
    public BaseResponse<Long> addQuestionList(@RequestBody QuestionListAddRequest questionListAddRequest, HttpServletRequest request) {
        if (questionListAddRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        QuestionList questionList = new QuestionList();
        BeanUtils.copyProperties(questionListAddRequest, questionList);
        List<String> tags = questionListAddRequest.getTags();
        if (tags != null) {
            questionList.setTags(JSONUtil.toJsonStr(tags));
        }
        List<QuestionCase> questionsId = questionListAddRequest.getQuestionCase();
        List<Question> questionCases = questionsId.stream().map(questionId -> questionService.getById(questionId.getInput())).collect(Collectors.toList());
        if (questionCases != null) {
            questionList.setQuestionCase(JSONUtil.toJsonStr(questionCases));
        }
        questionListService.validQuestionList(questionList, true);
        User loginUser = userFeignClient.getLoginUser(request);
        questionList.setUserId(loginUser.getId());
        questionList.setFavourNum(0);
        questionList.setThumbNum(0);
        boolean result = questionListService.save(questionList);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        long newQuestionListId = questionList.getId();
        return ResultUtils.success(newQuestionListId);
    }

    /**
     * 删除
     *
     * @param deleteRequest
     * @param request
     * @return
     */
    @PostMapping("/questionList/delete")
    public BaseResponse<Boolean> deleteQuestionList(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = userFeignClient.getLoginUser(request);
        long id = deleteRequest.getId();
        // 判断是否存在
        QuestionList oldQuestionList = questionListService.getById(id);
        ThrowUtils.throwIf(oldQuestionList == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可删除
        if (!oldQuestionList.getUserId().equals(user.getId()) && !userFeignClient.isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        boolean b = questionListService.removeById(id);
        return ResultUtils.success(b);
    }

    /**
     * 更新（仅管理员）
     *
     * @param questionListUpdateRequest
     * @return
     */
    @PostMapping("/questionList/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateQuestionList(@RequestBody QuestionListUpdateRequest questionListUpdateRequest) {
        if (questionListUpdateRequest == null || questionListUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        QuestionList questionList = new QuestionList();
        BeanUtils.copyProperties(questionListUpdateRequest, questionList);
        List<String> tags = questionListUpdateRequest.getTags();
        if (tags != null) {
            questionList.setTags(JSONUtil.toJsonStr(tags));
        }
        List<QuestionCase> questionsId = questionListUpdateRequest.getQuestionCase();
        List<Question> questionCases = questionsId.stream().map(questionId -> questionService.getById(questionId.getInput())).collect(Collectors.toList());
        if (questionCases != null) {
            questionList.setQuestionCase(JSONUtil.toJsonStr(questionCases));
        }
        // 参数校验
        questionListService.validQuestionList(questionList, false);
        long id = questionListUpdateRequest.getId();
        // 判断是否存在
        QuestionList oldQuestionList = questionListService.getById(id);
        ThrowUtils.throwIf(oldQuestionList == null, ErrorCode.NOT_FOUND_ERROR);
        boolean result = questionListService.updateById(questionList);
        return ResultUtils.success(result);
    }

    /**
     * 根据 id 获取（脱敏）
     *
     * @param id
     * @return
     */
    @GetMapping("/questionList/get/vo")
    public BaseResponse<QuestionListVO> getQuestionListVOById(long id, HttpServletRequest request) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        QuestionList questionList = questionListService.getById(id);
        if (questionList == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        return ResultUtils.success(questionListService.getQuestionListVO(questionList, request));
    }

    /**
     * 根据 id 获取
     *
     * @param id
     * @return
     */
    @GetMapping("/questionList/get")
    public BaseResponse<QuestionListUpdateRequest> getQuestionListById(long id, HttpServletRequest request) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        QuestionList questionList = questionListService.getById(id);
        if (questionList == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        User loginUser = userFeignClient.getLoginUser(request);
        //不是本人或管理员, 不能获取全部数据
        if(!questionList.getUserId().equals(loginUser.getId()) && !userFeignClient.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        QuestionListVO questionListVO = questionListService.getQuestionListVO(questionList, request);
        QuestionListUpdateRequest questionListUpdateRequest = new QuestionListUpdateRequest();
        BeanUtils.copyProperties(questionListVO, questionListUpdateRequest);
        List<QuestionCase> list = questionListVO.getQuestionCase().stream().map(question -> {
            QuestionCase questionCase = new QuestionCase();
            questionCase.setInput(question.getId());
            return questionCase;
        }).collect(Collectors.toList());
        questionListUpdateRequest.setQuestionCase(list);
        return ResultUtils.success(questionListUpdateRequest);
    }
    /**
     * 分页获取列表（仅管理员）
     *
     * @param questionQueryRequest
     * @return
     */
    @PostMapping("/questionList/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<QuestionList>> listQuestionListByPage(@RequestBody QuestionListQueryRequest questionQueryRequest) {
        long current = questionQueryRequest.getCurrent();
        long size = questionQueryRequest.getPageSize();
        Page<QuestionList> questionListPage = questionListService.page(new Page<>(current, size),
                questionListService.getQueryWrapper(questionQueryRequest));
        return ResultUtils.success(questionListPage);
    }

    /**
     * 分页获取列表（封装类）
     *
     * @param questionListQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/questionList/list/page/vo")
    public BaseResponse<Page<QuestionListVO>> listQuestionListVOByPage(@RequestBody QuestionListQueryRequest questionListQueryRequest,
                                                                       HttpServletRequest request) {
        long current = questionListQueryRequest.getCurrent();
        long size = questionListQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        Page<QuestionList> questionListPage = questionListService.page(new Page<>(current, size),
                questionListService.getQueryWrapper(questionListQueryRequest));
        return ResultUtils.success(questionListService.getQuestionListVOPage(questionListPage, request));
    }

    /**
     * 分页获取当前用户创建的资源列表
     *
     * @param questionListQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/questionList/my/list/page/vo")
    public BaseResponse<Page<QuestionListVO>> listMyQuestionListVOByPage(@RequestBody QuestionListQueryRequest questionListQueryRequest,
                                                                         HttpServletRequest request) {
        if (questionListQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userFeignClient.getLoginUser(request);
        questionListQueryRequest.setUserId(loginUser.getId());
        long current = questionListQueryRequest.getCurrent();
        long size = questionListQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        Page<QuestionList> questionListPage = questionListService.page(new Page<>(current, size),
                questionListService.getQueryWrapper(questionListQueryRequest));
        return ResultUtils.success(questionListService.getQuestionListVOPage(questionListPage, request));
    }

    /**
     * 编辑（用户）
     *
     * @param questionListEditRequest
     * @param request
     * @return
     */
    @PostMapping("/questionList/edit")
    public BaseResponse<Boolean> editQuestionList(@RequestBody QuestionListEditRequest questionListEditRequest, HttpServletRequest request) {
        if (questionListEditRequest == null || questionListEditRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        QuestionList questionList = new QuestionList();
        BeanUtils.copyProperties(questionListEditRequest, questionList);
        List<String> tags = questionListEditRequest.getTags();
        if (tags != null) {
            questionList.setTags(JSONUtil.toJsonStr(tags));
        }
        List<QuestionCase> questionsId = questionListEditRequest.getQuestionCase();
        List<Question> questionCases = questionsId.stream().map(questionId -> questionService.getById(questionId.getInput())).collect(Collectors.toList());
        if (questionCases != null) {
            questionList.setQuestionCase(JSONUtil.toJsonStr(questionCases));
        }
        // 参数校验
        questionListService.validQuestionList(questionList, false);
        User loginUser = userFeignClient.getLoginUser(request);
        long id = questionListEditRequest.getId();
        // 判断是否存在
        QuestionList oldQuestionList = questionListService.getById(id);
        ThrowUtils.throwIf(oldQuestionList == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可编辑
        if (!oldQuestionList.getUserId().equals(loginUser.getId()) && !userFeignClient.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        boolean result = questionListService.updateById(questionList);
        return ResultUtils.success(result);
    }

}
