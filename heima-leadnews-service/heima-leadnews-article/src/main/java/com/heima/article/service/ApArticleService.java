package com.heima.article.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.heima.model.article.dtos.ArticleCommentDto;
import com.heima.model.article.dtos.ArticleDto;
import com.heima.model.article.dtos.ArticleHomeDto;
import com.heima.model.article.dtos.ArticleInfoDto;
import com.heima.model.article.pojos.ApArticle;
import com.heima.model.common.dtos.PageResponseResult;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.mess.ArticleVisitStreamMess;
import com.heima.model.wemedia.dtos.StatisticsDto;

import java.util.Date;

public interface ApArticleService extends IService<ApArticle> {
    /**
     * 根据参数加载文章列表
     * @param loadtype 1为加载更多  2为加载最新
     * @param dto
     * @return
     */
    ResponseResult load(Short loadtype, ArticleHomeDto dto);

    ResponseResult saveArticle(ArticleDto articleDto);

    ResponseResult loadArticleBehavior(ArticleInfoDto dto);

    /**
     * 根据参数加载文章列表
     * @param loadtype 1为加载更多  2为加载最新
     * @param dto
     * @param firstPage true 是首页
     * @return
     */
    ResponseResult load2(Short loadtype, ArticleHomeDto dto, Boolean firstPage);

    /**
     * 更新文章的分值 同时更新缓存中的热点文章数据
     * @param mess
     */
    public void updateScore(ArticleVisitStreamMess mess);

    PageResponseResult findNewsComments(ArticleCommentDto dto);

    ResponseResult queryLikesAndConllections(Integer wmUserId, Date beginDate, Date endDate);

    PageResponseResult newPage(StatisticsDto dto);
}
