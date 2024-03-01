package com.heima.user.service.impl;

import com.heima.common.constants.BehaviorConstants;
import com.heima.common.redis.CacheService;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.user.dtos.UserRelationDto;
import com.heima.model.user.pojos.ApUser;
import com.heima.user.service.ApUserRelationService;
import com.heima.utils.thread.AppThreadLocalUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ApUserRelationServiceImpl implements ApUserRelationService {
    @Autowired
    private CacheService cacheService;
    @Override
    public ResponseResult follow(UserRelationDto dto) {
        //参数校验
        if (dto == null || dto.getOperation() < 0 || dto.getOperation() > 1) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }

        //获取登录用户
        ApUser user = AppThreadLocalUtils.getUser();
        if (user == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.NEED_LOGIN);
        }


        // 关注
        Integer userId = user.getId();
        Integer authorId = dto.getAuthorId();
        if (dto.getOperation() == 0) {
            //将对方写入我的关注里
            cacheService.zAdd(BehaviorConstants.APUSER_FOLLOW_RELATION+userId,authorId.toString(),System.currentTimeMillis());
            //将我写入对方的粉丝中
            cacheService.zAdd(BehaviorConstants.APUSER_FANS_RELATION+authorId,userId.toString(),System.currentTimeMillis());
        } else {
            //取消关注
            cacheService.zRemove(BehaviorConstants.APUSER_FOLLOW_RELATION+userId,authorId.toString());
            cacheService.zRemove(BehaviorConstants.APUSER_FANS_RELATION+authorId,userId.toString());
        }
        return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
    }
}
