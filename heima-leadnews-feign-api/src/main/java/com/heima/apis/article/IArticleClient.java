package com.heima.apis.article;

import com.heima.apis.article.fallback.IArticleClientFallback;
import com.heima.model.article.dtos.ArticleCommentDto;
import com.heima.model.article.dtos.ArticleDto;
import com.heima.model.comment.dtos.CommentConfigDto;
import com.heima.model.common.dtos.PageResponseResult;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.wemedia.dtos.StatisticsDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.Date;

@FeignClient(value = "leadnews-article",fallback = IArticleClientFallback.class)
//@Repository
public interface IArticleClient {

    @PostMapping("/api/v1/article/save")
    public ResponseResult saveArticle(@RequestBody ArticleDto articleDto);

    @GetMapping("/api/v1/article/findArticleConfigByArticleId/{articleId}")
    public ResponseResult findArticleConfigByArticleId(@PathVariable("articleId") Long articleId);

    @PostMapping("/api/v1/article/findNewsComments")
    public PageResponseResult findNewsComments(@RequestBody ArticleCommentDto dto);

    @PostMapping("/api/v1/article/updateCommentStatus")
    public ResponseResult updateCommentStatus(@RequestBody CommentConfigDto dto);

    @GetMapping("/api/v1/article/queryLikesAndConllections")
    ResponseResult queryLikesAndConllections(@RequestParam("wmUserId") Integer wmUserId, @RequestParam("beginDate") Date beginDate, @RequestParam("endDate") Date endDate);

    @PostMapping("/api/v1/article/newPage")
    PageResponseResult newPage(@RequestBody StatisticsDto dto);

}
