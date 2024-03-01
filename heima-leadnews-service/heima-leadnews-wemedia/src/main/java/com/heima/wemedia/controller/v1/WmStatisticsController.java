package com.heima.wemedia.controller.v1;

import com.heima.model.common.dtos.PageResponseResult;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.wemedia.dtos.StatisticsDto;
import com.heima.wemedia.service.WmStatisticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/statistics")
public class WmStatisticsController {
    @Autowired
    private WmStatisticsService wmStatisticsService;

    /**
     * 图文统计
     *
     * @param beginDate
     * @param endDate
     * @return
     */
    @GetMapping("/newsDimension")
    public ResponseResult newsDimension(String beginDate, String endDate) {
        return wmStatisticsService.newsDimension(beginDate,endDate);
    }

    /**
     * 分页查询图文统计
     *
     * @param dto
     * @return
     */
    @GetMapping("/newsPage")
    public PageResponseResult newsPage(StatisticsDto dto){
        return wmStatisticsService.newsPage(dto);
    }


}
