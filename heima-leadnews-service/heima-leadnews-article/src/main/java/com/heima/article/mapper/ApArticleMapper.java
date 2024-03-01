package com.heima.article.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.heima.model.article.dtos.ArticleCommentDto;
import com.heima.model.article.dtos.ArticleHomeDto;
import com.heima.model.article.pojos.ApArticle;
import com.heima.model.article.vos.ArticleCommnetVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;
import java.util.Map;

@Mapper
public interface ApArticleMapper extends BaseMapper<ApArticle> {
    List<ApArticle> loadArticleList(@Param("dto") ArticleHomeDto dto,@Param("type") Short loadtype);

    List<ApArticle> findArticleListByLast5days(@Param("dayParam") Date dayParam);

    List<ArticleCommnetVo> findNewsComments(@Param("dto") ArticleCommentDto dto);

    int findNewsCommentsCount(@Param("dto") ArticleCommentDto dto);

    Map queryLikesAndConllections(@Param("wmUserId")Integer wmUserId,@Param("beginDate") Date beginDate,@Param("endDate") Date endDate);
}
