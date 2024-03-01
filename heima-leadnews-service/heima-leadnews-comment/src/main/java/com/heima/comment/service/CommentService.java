package com.heima.comment.service;


import com.heima.model.comment.dtos.CommentDto;
import com.heima.model.comment.dtos.CommentLikeDto;
import com.heima.model.comment.vos.CommentSaveDto;
import com.heima.model.common.dtos.ResponseResult;

public interface CommentService {
    ResponseResult saveComment(CommentSaveDto dto);

    ResponseResult like(CommentLikeDto dto);

    ResponseResult findByArticleId(CommentDto dto);
}
