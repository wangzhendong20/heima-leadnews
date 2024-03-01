package com.heima.comment.service.impl;

import com.heima.apis.user.IUserClient;
import com.heima.comment.service.CommentRepayService;
import com.heima.common.aliyun.GreenTextScan;
import com.heima.model.comment.dtos.CommentRepayDto;
import com.heima.model.comment.dtos.CommentRepayLikeDto;
import com.heima.model.comment.dtos.CommentRepaySaveDto;
import com.heima.model.comment.pojos.ApComment;
import com.heima.model.comment.pojos.ApCommentRepay;
import com.heima.model.comment.pojos.ApCommentRepayLike;
import com.heima.model.comment.vos.CommentRepayVo;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.user.pojos.ApUser;
import com.heima.utils.thread.AppThreadLocalUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class CommentRepayServiceImpl implements CommentRepayService {
    @Autowired
    private IUserClient userClient;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private GreenTextScan greenTextScan;

    @Override
    public ResponseResult saveCommentRepay(CommentRepaySaveDto dto) {

        //1.检查参数
        if(dto == null || StringUtils.isBlank(dto.getContent()) || dto.getCommentId() == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }
        if(dto.getContent().length() > 140){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID,"评论内容不能超过140字");
        }

        //检验登录
        ApUser user = AppThreadLocalUtils.getUser();
        if (user == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.NEED_LOGIN);
        }

        //安全检查
        boolean isTextScan = handleTextScan(dto.getContent());
        if (!isTextScan) ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);

        //保存回复
        ApUser dbUser = userClient.findUserById(user.getId());
        if (dbUser == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }
        ApCommentRepay apCommentRepay = new ApCommentRepay();
        apCommentRepay.setAuthorId(user.getId());
        apCommentRepay.setContent(dto.getContent());
        apCommentRepay.setCreatedTime(new Date());
        apCommentRepay.setCommentId(dto.getCommentId());
        apCommentRepay.setAuthorName(dbUser.getName());
        apCommentRepay.setUpdatedTime(new Date());
        apCommentRepay.setLikes(0);
        mongoTemplate.save(apCommentRepay);

        //更新回复数量
        ApComment apComment = mongoTemplate.findById(dto.getCommentId(),ApComment.class);
        apComment.setReply(apComment.getReply()+1);
        mongoTemplate.save(apComment);

        return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
    }

    private boolean handleTextScan(String content) {
        boolean flag = true;

        if(content.length() == 0) {
            return flag;
        }

        try {
            Map map = greenTextScan.greeTextScan(content);
            if (map != null) {
                flag = false;
            }
        } catch (Exception e) {
            flag = false;
            throw new RuntimeException(e);
        }
        return flag;
    }

    @Override
    public ResponseResult saveCommentRepayLike(CommentRepayLikeDto dto) {

        //1.检查参数
        if (dto == null || dto.getCommentRepayId() == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }
        //是否登录
        ApUser user = AppThreadLocalUtils.getUser();
        if (user == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }

        ApCommentRepay commentRepay = mongoTemplate.findById(dto.getCommentRepayId(), ApCommentRepay.class);

        //点赞
        if (commentRepay != null && dto.getOperation() == 0) {
            //更新回复点赞数量
            commentRepay.setLikes(commentRepay.getLikes()+1);
            mongoTemplate.save(commentRepay);

            //保存回复点赞数据
            ApCommentRepayLike apCommentRepayLike = new ApCommentRepayLike();
            apCommentRepayLike.setCommentRepayId(commentRepay.getId());
            apCommentRepayLike.setAuthorId(user.getId());
            mongoTemplate.save(apCommentRepayLike);
        } else {
            int tmp = commentRepay.getLikes() - 1;
            tmp = tmp < 1 ? 0 : tmp;
            commentRepay.setLikes(tmp);
            mongoTemplate.save(commentRepay);

            //删除回复点赞数据
            Query query = Query.query(Criteria.where("commentRepayId").is(commentRepay).and("authorId").is(user.getId()));
            mongoTemplate.remove(query,ApCommentRepayLike.class);
        }

        Map<String,Object> result = new HashMap<>();
        result.put("likes",commentRepay.getLikes());
        return ResponseResult.okResult(result);
    }

    @Override
    public ResponseResult loadCommentRepay(CommentRepayDto dto) {

        if (dto == null || dto.getCommentId() == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }
        int size = 20;

        //加载数据
        Query query = Query.query(Criteria.where("commentId").is(dto.getCommentId()).and("createdTime").lt(dto.getMinDate()));
        query.with(Sort.by(Sort.Direction.DESC)).limit(size);
        List<ApCommentRepay> apCommentRepayList = mongoTemplate.find(query, ApCommentRepay.class);

        //数据封装返回
        //1.用户未登录
        ApUser user = AppThreadLocalUtils.getUser();
        if (user == null) {
            return ResponseResult.okResult(apCommentRepayList);
        }

        //2.用户已登录
        //2.1 查询回复里哪些被点赞了
        List<String> idList = apCommentRepayList.stream().map(x -> x.getId()).collect(Collectors.toList());
        Query query1 = Query.query(Criteria.where("commentRepayId").in(idList).and("authorId").is(user.getId()));
        List<ApCommentRepayLike> apCommentRepayLikes = mongoTemplate.find(query, ApCommentRepayLike.class);
        if (apCommentRepayLikes == null && apCommentRepayLikes.size() == 0) {
            return ResponseResult.okResult(apCommentRepayList);
        }

        List<CommentRepayVo> result = new ArrayList<>();
        apCommentRepayList.forEach(x -> {
            CommentRepayVo vo = new CommentRepayVo();
            BeanUtils.copyProperties(x,vo);
            for (ApCommentRepayLike apCommentRepayLike : apCommentRepayLikes) {
                if (x.getId().equals(apCommentRepayLike.getCommentRepayId())) {
                    vo.setOperation((short) 0);
                    break;
                }
            }
            result.add(vo);
        });
        return ResponseResult.okResult(result);
    }
}
