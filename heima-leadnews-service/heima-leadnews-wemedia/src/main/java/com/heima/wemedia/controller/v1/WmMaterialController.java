package com.heima.wemedia.controller.v1;

import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.wemedia.dtos.WmMaterialDto;
import com.heima.wemedia.service.WmMaterialService;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/material")
public class WmMaterialController {
    @Autowired
    private WmMaterialService wmMaterialService;


    // 图片上传
    @ApiOperation(value = "图片上传")
    @PostMapping("/upload_picture")
    public ResponseResult uploadPicture(MultipartFile multipartFile){
        return wmMaterialService.uploadPicture(multipartFile);
    }

    //查询素材管理列表
    @ApiOperation(value = "查询素材管理列表")
    @PostMapping("/list")
    public ResponseResult findList(@RequestBody WmMaterialDto wmMaterialDto) {
        return wmMaterialService.findList(wmMaterialDto);
    }


    @ApiOperation(value = "图片删除")
    @GetMapping("/del_picture/{id}")
    public ResponseResult delPicture(@PathVariable Integer id) {
        //TODO:待测试
        return wmMaterialService.delPicture(id);
    }

    @ApiOperation(value = "收藏")
    @GetMapping("/collect/{id}")
    public ResponseResult collect(@PathVariable Integer id) {
        //TODO:待测试
        return wmMaterialService.CollectOrUnById(id);
    }

    @ApiOperation(value = "取消收藏")
    @GetMapping("/cancel_collect/{id}")
    public ResponseResult cancelCollect(@PathVariable Integer id) {
        //TODO:待测试
        return wmMaterialService.CollectOrUnById(id);
    }


}
