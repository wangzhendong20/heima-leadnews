package com.heima.wemedia.controller.v1;

import com.heima.common.constants.WemediaConstants;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.wemedia.dtos.NewsAuthDto;
import com.heima.model.wemedia.dtos.WmNewsDto;
import com.heima.model.wemedia.dtos.WmNewsPageReqDto;
import com.heima.wemedia.service.WmNewsService;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/news")
public class WmNewsController {


    @Autowired
    private WmNewsService wmNewsService;

    @ApiOperation(value = "分页查询文章内容列表")
    @PostMapping("/list")
    public ResponseResult findAll(@RequestBody WmNewsPageReqDto dto){
        return  wmNewsService.findAll(dto);
    }

    @ApiOperation(value = "文章发布、修改,保存草稿")
    @PostMapping("/submit")
    public ResponseResult submitNews(@RequestBody WmNewsDto dto){
        return wmNewsService.submitNews(dto);
    }

    @ApiOperation(value = "查看详情")
    @GetMapping("/one/{id}")
    public ResponseResult lookDetail(@PathVariable Integer id){
        //TODO:待测试
        return wmNewsService.lookDetail(id);
    }

    @ApiOperation(value = "文章删除")
    @GetMapping("/del_news/{id}")
    public ResponseResult deleteNews(@PathVariable Integer id){
        //TODO:待测试
        return wmNewsService.deleteNews(id);
    }

    @ApiOperation(value = "文章上下架")
    @PostMapping("/down_or_up")
    public ResponseResult downOrUp(@RequestBody WmNewsDto wmNewsDto){
        return wmNewsService.downOrUp(wmNewsDto);
    }

    @PostMapping("/list_vo")
    public ResponseResult findList(@RequestBody NewsAuthDto newsAuthDto) {
        return wmNewsService.findList(newsAuthDto);
    }

    @GetMapping("/one_vo/{id}")
    public ResponseResult findWmNewsVo(@PathVariable("id") Integer id){
        return wmNewsService.findWmNewsVo(id);
    }

    @PostMapping("/auth_pass")
    public ResponseResult authPass(@RequestBody NewsAuthDto dto){
        return wmNewsService.updateStatus(WemediaConstants.WM_NEWS_AUTH_PASS,dto);
    }

    @PostMapping("/auth_fail")
    public ResponseResult authFail(@RequestBody NewsAuthDto dto){
        return wmNewsService.updateStatus(WemediaConstants.WM_NEWS_AUTH_FAIL,dto);
    }


}
