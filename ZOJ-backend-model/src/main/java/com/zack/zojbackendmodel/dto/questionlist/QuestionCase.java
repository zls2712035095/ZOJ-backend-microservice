package com.zack.zojbackendmodel.dto.questionlist;

import lombok.Data;

import java.io.Serializable;

/**
 * 题目用例
 */
@Data
public class QuestionCase implements Serializable {

    /**
     * 输入用例
     */
    private Long input;
}
