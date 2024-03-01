package com.heima.wemedia.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.heima.model.common.dtos.PageResponseResult;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.wemedia.dtos.ChannelDto;
import com.heima.model.wemedia.pojos.WmChannel;
import com.heima.model.wemedia.pojos.WmNews;
import com.heima.wemedia.mapper.WmChannelMapper;
import com.heima.wemedia.service.WmChannelService;
import com.heima.wemedia.service.WmNewsService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

@Service
@Transactional
@Slf4j
public class WmChannelServiceImpl extends ServiceImpl<WmChannelMapper, WmChannel> implements WmChannelService {
    @Autowired
    private WmNewsService wmNewsService;

    /**
     * 查询所有频道
     * @return
     */
    @Override
    public ResponseResult findAll() {
        return ResponseResult.okResult(list());
    }

    @Override
    public ResponseResult findByNameAndPage(ChannelDto dto) {

        if (dto == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }
        //检查分页参数
        dto.checkParam();

        IPage page = new Page(dto.getPage(), dto.getSize());

        LambdaQueryWrapper<WmChannel> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        if (StringUtils.isNotBlank(dto.getName())) {
            lambdaQueryWrapper.like(WmChannel::getName,dto.getName());
        }

        IPage pageModel = page(page, lambdaQueryWrapper);

        PageResponseResult pageResponseResult = new PageResponseResult(dto.getPage(), dto.getSize(), (int) pageModel.getTotal());
        pageResponseResult.setData(pageModel.getRecords());
        return pageResponseResult;

    }

    @Override
    public ResponseResult insert(WmChannel adChannel) {
        //1.检查参数
        if (adChannel == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }
        //是否已经存在
        WmChannel channel = getOne(Wrappers.<WmChannel>lambdaQuery().eq(WmChannel::getName, adChannel.getName()));
        if (channel != null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.DATA_EXIST, "频道已存在");
        }

        //2.保存
        adChannel.setCreatedTime(new Date());
        save(adChannel);
        return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
    }

    @Override
    public ResponseResult update(WmChannel adChannel) {
        if (adChannel == null || adChannel.getId() == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }
        //是否被文章使用
        LambdaQueryWrapper<WmNews> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(WmNews::getChannelId,adChannel.getId());
        lambdaQueryWrapper.eq(WmNews::getStatus,WmNews.Status.PUBLISHED.getCode());
        int count = wmNewsService.count(lambdaQueryWrapper);
        if (count > 0) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID,"频道被引用不能修改或禁用");
        }
        //修改
        update(adChannel);
        return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
    }

    @Override
    public ResponseResult delete(Integer id) {
        if (id == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }
        //2.查询频道
        WmChannel channel = getById(id);
        if (channel == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.DATA_NOT_EXIST);
        }
        //3.频道是否有效
        if (channel.getStatus()) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID,"频道有效，不能删除");
        }
        //判断是否被引用
        LambdaQueryWrapper<WmNews> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(WmNews::getChannelId,channel.getId());
        lambdaQueryWrapper.eq(WmNews::getStatus,WmNews.Status.PUBLISHED.getCode());
        int count = wmNewsService.count(lambdaQueryWrapper);
        if (count > 0) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID,"频道被引用不能修改或禁用");
        }
        removeById(id);
        return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
    }
}
