package com.zack.zojbackendquestionservice.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.zack.zojbackendmodel.codeSandbox.JudgeInfo;
import com.zack.zojbackendmodel.common.ErrorCode;
import com.zack.zojbackendmodel.constant.CommonConstant;
import com.zack.zojbackendmodel.dto.questionsubmit.QuestionSubmitAddRequest;
import com.zack.zojbackendmodel.dto.questionsubmit.QuestionSubmitQueryRequest;
import com.zack.zojbackendmodel.entity.Question;
import com.zack.zojbackendmodel.entity.QuestionSubmit;
import com.zack.zojbackendmodel.entity.User;
import com.zack.zojbackendmodel.entity.UserRank;
import com.zack.zojbackendmodel.enums.QuestionSubmitEnum;
import com.zack.zojbackendmodel.enums.QuestionSubmitLanguageEnum;
import com.zack.zojbackendmodel.exception.BusinessException;
import com.zack.zojbackendmodel.utils.SqlUtils;
import com.zack.zojbackendmodel.vo.QuestionSubmitVO;
import com.zack.zojbackendmodel.vo.QuestionVO;
import com.zack.zojbackendmodel.vo.UserVO;
import com.zack.zojbackendquestionservice.mapper.QuestionSubmitMapper;
import com.zack.zojbackendquestionservice.rabbitmq.MyMessageProducer;
import com.zack.zojbackendquestionservice.service.QuestionService;
import com.zack.zojbackendquestionservice.service.QuestionSubmitService;
import com.zack.zojbackendquestionservice.service.UserRankService;
import com.zack.zojbackendserviceclient.service.JudgeFeignClient;
import com.zack.zojbackendserviceclient.service.UserFeignClient;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * @author admin
 * @description 针对表【questionSubmit_submit(题目提交)】的数据库操作Service实现
 * @createDate 2024-03-29 10:21:55
 */
@Service
public class QuestionSubmitServiceImpl extends ServiceImpl<QuestionSubmitMapper, QuestionSubmit>
        implements QuestionSubmitService {
    @Resource
    private QuestionService questionService;

    @Resource
    private UserFeignClient userService;

    @Resource
    @Lazy
    private JudgeFeignClient judgeFeignClient;

    @Resource
    private UserRankService userRankService;

    @Resource
    private MyMessageProducer myMessageProducer;

    /**
     * 提交题目
     *
     * @param questionSubmitAddRequest
     * @param loginUser
     * @return 提交记录的id
     */
    @Override
    public long doQuestionSubmit(QuestionSubmitAddRequest questionSubmitAddRequest, User loginUser) {
        // 校验语言是否合法
        String language = questionSubmitAddRequest.getLanguage();
        QuestionSubmitLanguageEnum languageEnum = QuestionSubmitLanguageEnum.getEnumByValue(language);
        if (languageEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "编程语言错误");
        }
        long questionId = questionSubmitAddRequest.getQuestionId();
        // 判断实体是否存在，根据类别获取实体
        Question question = questionService.getById(questionId);
        if (question == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        question.setSubmitNum(question.getSubmitNum() + 1);
        questionService.updateById(question);
        // 是否已提交题目
        long userId = loginUser.getId();
        //提交题目
        QuestionSubmit questionSubmit = new QuestionSubmit();
        questionSubmit.setUserId(userId);
        questionSubmit.setQuestionId(questionId);
        questionSubmit.setCode(questionSubmitAddRequest.getCode());
        questionSubmit.setLanguage(language);
        // 设置初始状态
        questionSubmit.setStatus(QuestionSubmitEnum.WATITING.getValue());
        questionSubmit.setJudgeInfo("{}");
        boolean save = this.save(questionSubmit);
        if (!save) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "数据插入失败");
        }
        // 增加用户提交数
        long userRankId = userRankService.getUserRankIdByUserId(userId);
        UserRank userRank = new UserRank();
        userRank.setId(userRankId);
        userRank.setSubmitNum(userRankService.getUserRankSubmitNum(userId) + 1);
        userRankService.updateById(userRank);
        // 执行判题服务
        Long questionSubmitId = questionSubmit.getId();
        myMessageProducer.sendMessage("code_exchange", "my_routingKey", String.valueOf(questionSubmitId));
//        CompletableFuture.runAsync(() -> {
//            judgeFeignClient.doJudge(questionSubmitId);
//            QuestionSubmit questionSubmit1 = getById(questionSubmitId);
//            JudgeInfo judgeInfo = JSONUtil.toBean(questionSubmit1.getJudgeInfo(), JudgeInfo.class);
//            if ("Accepted".equals(judgeInfo.getMessage())) {
//                // 增加用户Ac数
//                UserRank acUpdateUserRank = new UserRank();
//                acUpdateUserRank.setId(userRankService.getUserRankIdByUserId(userId));
//                acUpdateUserRank.setAcNum(userRankService.getUserRankAcNum(userId) + 1);
//                userRankService.updateById(acUpdateUserRank);
//                question.setAcceptNum(question.getAcceptNum() + 1);
//                // 增加题目Ac数
//            }
//        });
        return questionSubmitId;
        // 每个用户串行提交题目
        // 锁必须要包裹住事务方法
//        QuestionSubmitService questionSubmitService = (QuestionSubmitService) AopContext.currentProxy();
//        synchronized (String.valueOf(userId).intern()) {
//            return questionSubmitService.doQuestionSubmitInner(userId, questionSubmitId);
//        }
    }


    /**
     * 获取查询包装类（用户根据哪些字段查询，根据前端传来的请求对象，得到 mybatis 框架支持的查询 QueryWrapper 类）
     *
     * @param questionSubmitQueryRequest
     * @return
     */
    @Override
    public QueryWrapper<QuestionSubmit> getQueryWrapper(QuestionSubmitQueryRequest questionSubmitQueryRequest) {
        QueryWrapper<QuestionSubmit> queryWrapper = new QueryWrapper<>();
        if (questionSubmitQueryRequest == null) {
            return queryWrapper;
        }
        String language = questionSubmitQueryRequest.getLanguage();
        Integer status = questionSubmitQueryRequest.getStatus();
        Long questionId = questionSubmitQueryRequest.getQuestionId();
        Long userId = questionSubmitQueryRequest.getUserId();
        String sortOrder = questionSubmitQueryRequest.getSortOrder();
        String sortField = questionSubmitQueryRequest.getSortField();

        // 拼接查询条件
        queryWrapper.like(StringUtils.isNotBlank(language), "language", language);
        queryWrapper.like(QuestionSubmitEnum.getEnumByValue(status) != null, "status", status);
        queryWrapper.eq(ObjectUtils.isNotEmpty(questionId), "questionId", questionId);
        queryWrapper.eq(ObjectUtils.isNotEmpty(userId), "userId", userId);
        queryWrapper.eq("isDelete", false);

        queryWrapper.orderBy(SqlUtils.validSortField(sortField), sortOrder.equals(CommonConstant.SORT_ORDER_ASC),
                sortField);

        return queryWrapper;
    }

    @Override
    public QuestionSubmitVO getQuestionSubmitVO(QuestionSubmit questionSubmit, User loginUSer) {
        QuestionSubmitVO questionSubmitVO = QuestionSubmitVO.objToVo(questionSubmit);
        // 1. 关联查询用户信息
        long userId = loginUSer.getId();
        User user = null;
        if (userId > 0) {
            user = userService.getById(userId);
        }
        UserVO userVO = userService.getUserVO(user);
        questionSubmitVO.setUserVO(userVO);
        // 2. 查看questionVo信息
        long questionId = questionSubmit.getQuestionId();
        Question question = null;
        if (questionId > 0) {
            question = questionService.getById(questionId);
        }
        QuestionVO questionVO = QuestionVO.objToVo(question);
        questionSubmitVO.setQuestionVO(questionVO);
        if (userId != questionSubmitVO.getUserId() && !userService.isAdmin(loginUSer)) {
            questionSubmitVO.setCode(null);
        }
        return questionSubmitVO;
    }

    @Override
    public Page<QuestionSubmitVO> getQuestionSubmitVOPage(Page<QuestionSubmit> questionSubmitPage, User loginUSer) {
        List<QuestionSubmit> questionSubmitList = questionSubmitPage.getRecords();
        Page<QuestionSubmitVO> questionSubmitVOPage = new Page<>(questionSubmitPage.getCurrent(), questionSubmitPage.getSize(), questionSubmitPage.getTotal());
        if (CollUtil.isEmpty(questionSubmitList)) {
            return questionSubmitVOPage;
        }

        List<QuestionSubmitVO> questionSubmitVOList = questionSubmitList.stream()
                .map(questionSubmit -> getQuestionSubmitVO(questionSubmit, loginUSer))
                .collect(Collectors.toList());

        questionSubmitVOPage.setRecords(questionSubmitVOList);
        return questionSubmitVOPage;
    }
    /**
     * 封装了事务的方法
     *
     * @param userId
     * @param questionSubmitId
     * @return
     */
//    @Override
//    @Transactional(rollbackFor = Exception.class)
//    public int doQuestionSubmitInner(long userId, long questionSubmitId) {
//        QuestionSubmit questionSubmit = new QuestionSubmit();
//        questionSubmit.setUserId(userId);
//        questionSubmit.setQuestionSubmitId(questionSubmitId);
//        QueryWrapper<QuestionSubmit> thumbQueryWrapper = new QueryWrapper<>(questionSubmit);
//        QuestionSubmit oldQuestionSubmit = this.getOne(thumbQueryWrapper);
//        boolean result;
//        // 已提交题目
//        if (oldQuestionSubmit != null) {
//            result = this.remove(thumbQueryWrapper);
//            if (result) {
//                // 提交题目数 - 1
//                result = questionSubmitService.update()
//                        .eq("id", questionSubmitId)
//                        .gt("thumbNum", 0)
//                        .setSql("thumbNum = thumbNum - 1")
//                        .update();
//                return result ? -1 : 0;
//            } else {
//                throw new BusinessException(ErrorCode.SYSTEM_ERROR);
//            }
//        } else {
//            // 未提交题目
//            result = this.save(questionSubmit);
//            if (result) {
//                // 提交题目数 + 1
//                result = questionSubmitService.update()
//                        .eq("id", questionSubmitId)
//                        .setSql("thumbNum = thumbNum + 1")
//                        .update();
//                return result ? 1 : 0;
//            } else {
//                throw new BusinessException(ErrorCode.SYSTEM_ERROR);
//            }
//        }
//    }
}




