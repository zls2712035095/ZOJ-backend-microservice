package com.zack.zojbackendmodel.dto.userrank;


import com.zack.zojbackendmodel.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 用户查询请求
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class UserRankQueryRequest extends PageRequest implements Serializable {
    /**
     * id
     */
    private Long id;

    /**
     * 用户 id
     */
    private Long userId;

    /**
     * 用户昵称
     */
    private String userName;

    /**
     * 用户简介
     */
    private String userProfile;

    /**
     * 过题总数
     */
    private Integer acNum;

    /**
     * 提交题目总数
     */
    private Integer submitNum;

    private static final long serialVersionUID = 1L;
}