package com.heima.wemedia.feign;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.heima.apis.wemedia.IWemediaClient;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.wemedia.pojos.WmUser;
import com.heima.wemedia.service.WmChannelService;
import com.heima.wemedia.service.WmUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class WemediaClient implements IWemediaClient {
    @Autowired
    private WmUserService wmUserService;
    @Autowired
    private WmChannelService wmChannelService;
    @Override
    public WmUser findWmUserByName(String name) {

        return wmUserService.getOne(Wrappers.<WmUser>lambdaQuery().eq(WmUser::getName,name));
    }

    @Override
    public ResponseResult saveWmUser(WmUser wmUser) {
        wmUserService.save(wmUser);
        return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
    }

    @Override
    public ResponseResult getChannels() {
        return wmChannelService.findAll();
    }
}
