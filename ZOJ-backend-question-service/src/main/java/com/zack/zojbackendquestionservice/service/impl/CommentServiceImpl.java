package com.zack.zojbackendquestionservice.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.zack.zojbackendmodel.common.ErrorCode;
import com.zack.zojbackendmodel.constant.CommonConstant;
import com.zack.zojbackendmodel.dto.comment.CommentQueryRequest;
import com.zack.zojbackendmodel.dto.comment.Comments;
import com.zack.zojbackendmodel.entity.Comment;
import com.zack.zojbackendmodel.exception.BusinessException;
import com.zack.zojbackendmodel.utils.SqlUtils;
import com.zack.zojbackendquestionservice.mapper.CommentMapper;
import com.zack.zojbackendquestionservice.service.CommentService;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author zls5600
 * @description 针对表【comment(评论)】的数据库操作Service实现
 * @createDate 2024-05-22 17:03:24
 */
@Service
public class CommentServiceImpl extends ServiceImpl<CommentMapper, Comment>
        implements CommentService {

    @Override
    public List<Comment> getAllByForeignId(Long forigenId) {
        CommentQueryRequest commentQueryRequest = new CommentQueryRequest();
        commentQueryRequest.setForeignId(forigenId);
        List<Comment> list = this.list(getQueryWrapper(commentQueryRequest));
        if (list == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "查询的数据为kong");
        }
        return list;
    }

    @Override
    public List<Comments> getCommentsPageList(Long forigenId) {
        List<Comment> commentList = getAllByForeignId(forigenId);
        //顶级评论
        List<Comments> commentRoot = commentList.stream().filter(comment -> comment.getPid() == null).map(comment -> new Comments(comment)).collect(Collectors.toList());
        for (Comments comment: commentRoot) {
            List<Comment> comments = commentList.stream().filter(o -> comment.getId().equals(o.getPid())).collect(Collectors.toList());
            comment.setChildrenComment(comments);
        }
        return commentRoot;
    }

    /**
     * 获取查询包装类（用户根据哪些字段查询，根据前端传来的请求对象，得到 mybatis 框架支持的查询 QueryWrapper 类）
     *
     * @param commentQueryRequest
     * @return
     */
    @Override
    public QueryWrapper<Comment> getQueryWrapper(CommentQueryRequest commentQueryRequest) {
        QueryWrapper<Comment> queryWrapper = new QueryWrapper<>();
        if (commentQueryRequest == null) {
            return queryWrapper;
        }
        Long id = commentQueryRequest.getId();
        String content = commentQueryRequest.getContent();
        Long userId = commentQueryRequest.getUserId();
        String userName = commentQueryRequest.getUserName();
        Long foreignId = commentQueryRequest.getForeignId();
        Long pid = commentQueryRequest.getPid();
        String target = commentQueryRequest.getTarget();
        String sortField = commentQueryRequest.getSortField();
        String sortOrder = commentQueryRequest.getSortOrder();
        // 拼接查询条件
        queryWrapper.like(StringUtils.isNotBlank(userName), "userName", userName);
        queryWrapper.like(StringUtils.isNotBlank(content), "content", content);
        queryWrapper.like(StringUtils.isNotBlank(target), "target", target);
        queryWrapper.eq(ObjectUtils.isNotEmpty(foreignId), "foreignId", foreignId);
        queryWrapper.eq(ObjectUtils.isNotEmpty(pid), "pid", pid);
        queryWrapper.eq(ObjectUtils.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjectUtils.isNotEmpty(userId), "userId", userId);
        queryWrapper.eq("isDelete", false);

        queryWrapper.orderBy(SqlUtils.validSortField(sortField), sortOrder.equals(CommonConstant.SORT_ORDER_ASC),
                sortField);
        return queryWrapper;
    }
}




