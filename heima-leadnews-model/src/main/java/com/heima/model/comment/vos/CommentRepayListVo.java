package com.heima.model.comment.vos;

import com.heima.model.comment.pojos.ApComment;
import com.heima.model.comment.pojos.ApCommentRepay;
import lombok.Data;

import java.util.List;

@Data
public class CommentRepayListVo  {

    private ApComment apComments;
    private List<ApCommentRepay> apCommentRepays;
}
