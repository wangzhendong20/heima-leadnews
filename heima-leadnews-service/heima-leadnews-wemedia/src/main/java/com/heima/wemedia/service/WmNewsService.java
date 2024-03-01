package com.heima.wemedia.service;


import com.baomidou.mybatisplus.extension.service.IService;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.wemedia.dtos.NewsAuthDto;
import com.heima.model.wemedia.dtos.WmNewsDto;
import com.heima.model.wemedia.dtos.WmNewsPageReqDto;
import com.heima.model.wemedia.pojos.WmNews;

public interface WmNewsService extends IService<WmNews> {

    /**
     * 查询文章
     * @param dto
     * @return
     */
    public ResponseResult findAll(WmNewsPageReqDto dto);

    ResponseResult submitNews(WmNewsDto dto);

    ResponseResult lookDetail(Integer id);

    ResponseResult deleteNews(Integer id);

    ResponseResult downOrUp(WmNewsDto wmNewsDto);

    ResponseResult findList(NewsAuthDto newsAuthDto);

    ResponseResult findWmNewsVo(Integer id);

    ResponseResult updateStatus(Short wmNewsAuthPass, NewsAuthDto dto);
}
