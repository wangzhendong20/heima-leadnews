package com.heima.wemedia.service;

import com.heima.model.article.dtos.ArticleCommentDto;
import com.heima.model.comment.dtos.CommentConfigDto;
import com.heima.model.comment.dtos.CommentLikeDto;
import com.heima.model.comment.dtos.CommentManageDto;
import com.heima.model.comment.dtos.CommentRepaySaveDto;
import com.heima.model.common.dtos.PageResponseResult;
import com.heima.model.common.dtos.ResponseResult;

public interface CommentManageService {

    PageResponseResult findNewsComments(ArticleCommentDto dto);

    ResponseResult updateCommentStatus(CommentConfigDto dto);

    ResponseResult list(CommentManageDto dto);

    ResponseResult saveCommentRepay(CommentRepaySaveDto dto);

    ResponseResult like(CommentLikeDto dto);

    ResponseResult delComment(String commentId);

    ResponseResult delCommentRepay(String commentRepayId);
}
