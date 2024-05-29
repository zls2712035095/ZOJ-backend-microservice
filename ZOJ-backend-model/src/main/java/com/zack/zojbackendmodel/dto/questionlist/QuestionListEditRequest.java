package com.zack.zojbackendmodel.dto.questionlist;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 编辑请求
 */
@Data
public class QuestionListEditRequest implements Serializable {

    /**
     * id
     */
    private Long id;

    /**
     * 题单标题
     */
    private String title;

    /**
     * 题单内容
     */
    private String content;

    /**
     * 题单标签列表（json 数组）
     */
    private List<String> tags;

    /**
     * 判题用例（json 数组）
     */
    private List<QuestionCase> questionCase;


    private static final long serialVersionUID = 1L;
}