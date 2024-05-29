package com.zack.zojbackendmodel.dto.userorder;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 创建请求
 */
@Data
public class UserorderAddRequest implements Serializable {

    /**
     * id
     */
    private Long id;


    /**
     * 用户昵称
     */
    private String userName;

    /**
     * 用户头像
     */
    private String userAvatar;

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

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 是否删除
     */
    private Integer isDelete;

    private static final long serialVersionUID = 1L;
}