package com.heima.article.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.heima.article.mapper.ApArticleConfigMapper;
import com.heima.article.service.ApArticleConfigService;
import com.heima.model.article.pojos.ApArticleConfig;
import com.heima.model.comment.dtos.CommentConfigDto;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@Slf4j
@Transactional
public class ApArticleConfigImpl extends ServiceImpl<ApArticleConfigMapper, ApArticleConfig> implements ApArticleConfigService {

    @Override
    public void updateByMap(Map map) {
        Object enable = map.get("enable");
        boolean isDown = true;
        if (enable.equals(1)) {
            isDown = false;
        }

        update(Wrappers.<ApArticleConfig>lambdaUpdate().eq(ApArticleConfig::getArticleId,map.get("articleId"))
                .set(ApArticleConfig::getIsDown,isDown));

    }

    @Override
    public ResponseResult updateCommentStatus(CommentConfigDto dto) {

        update(Wrappers.<ApArticleConfig>lambdaUpdate().eq(ApArticleConfig::getArticleId,dto.getArticleId())
                .eq(ApArticleConfig::getIsComment,dto.getOperation()));
        return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
    }
}
