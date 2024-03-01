package com.heima.behavior.service;

import com.heima.model.behavior.dtos.LikesBehaviorDto;
import com.heima.model.behavior.dtos.ReadBehaviorDto;
import com.heima.model.behavior.dtos.UnLikesBehaviorDto;
import com.heima.model.common.dtos.ResponseResult;

public interface ApBehaviorService {
    ResponseResult like(LikesBehaviorDto dto);

    ResponseResult unLike(UnLikesBehaviorDto dto);

    ResponseResult readBehavior(ReadBehaviorDto dto);
}
