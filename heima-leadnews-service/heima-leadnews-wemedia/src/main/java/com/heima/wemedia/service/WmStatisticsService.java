package com.heima.wemedia.service;

import com.heima.model.common.dtos.PageResponseResult;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.wemedia.dtos.StatisticsDto;

public interface WmStatisticsService {
    ResponseResult newsDimension(String beginDate, String endDate);

    PageResponseResult newsPage(StatisticsDto dto);
}
