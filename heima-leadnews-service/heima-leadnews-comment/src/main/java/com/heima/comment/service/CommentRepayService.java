package com.heima.comment.service;

import com.heima.model.comment.dtos.CommentRepayDto;
import com.heima.model.comment.dtos.CommentRepayLikeDto;
import com.heima.model.comment.dtos.CommentRepaySaveDto;
import com.heima.model.common.dtos.ResponseResult;

public interface CommentRepayService {
    ResponseResult saveCommentRepay(CommentRepaySaveDto dto);

    ResponseResult saveCommentRepayLike(CommentRepayLikeDto dto);

    ResponseResult loadCommentRepay(CommentRepayDto dto);
}
