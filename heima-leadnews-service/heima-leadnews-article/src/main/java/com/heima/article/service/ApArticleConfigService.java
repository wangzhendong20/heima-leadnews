package com.heima.article.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.heima.model.article.pojos.ApArticleConfig;
import com.heima.model.comment.dtos.CommentConfigDto;
import com.heima.model.common.dtos.ResponseResult;

import java.util.Map;

public interface ApArticleConfigService extends IService<ApArticleConfig> {
    void updateByMap(Map map);

    ResponseResult updateCommentStatus(CommentConfigDto dto);
}
