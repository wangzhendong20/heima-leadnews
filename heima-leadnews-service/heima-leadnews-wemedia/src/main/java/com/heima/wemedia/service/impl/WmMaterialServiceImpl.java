package com.heima.wemedia.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.heima.common.exception.ExceptionCatch;
import com.heima.file.service.FileStorageService;
import com.heima.model.common.dtos.PageResponseResult;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.wemedia.dtos.WmMaterialDto;
import com.heima.model.wemedia.pojos.WmMaterial;
import com.heima.model.wemedia.pojos.WmNewsMaterial;
import com.heima.utils.thread.WmThreadLocalUtils;
import com.heima.wemedia.mapper.WmMaterialMapper;
import com.heima.wemedia.mapper.WmNewsMaterialMapper;
import com.heima.wemedia.service.WmMaterialService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Date;
import java.util.UUID;

@Slf4j
@Service
@Transactional
public class WmMaterialServiceImpl extends ServiceImpl<WmMaterialMapper, WmMaterial> implements WmMaterialService {

    @Autowired
    private FileStorageService fileStorageServicel;
    @Autowired
    private WmNewsMaterialMapper wmNewsMaterialMapper;

    @Override
    public ResponseResult delPicture(Integer id) {
        if (id == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }
        WmMaterial material = getById(id);
        if (material == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.DATA_NOT_EXIST);
        }
        WmNewsMaterial wmNewsMaterial = wmNewsMaterialMapper.selectOne(Wrappers.<WmNewsMaterial>lambdaQuery().eq(WmNewsMaterial::getMaterialId, material.getId()));
        if (wmNewsMaterial != null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }
        baseMapper.delete(Wrappers.<WmMaterial>lambdaQuery().eq(WmMaterial::getId,material.getId()));
        fileStorageServicel.delete(material.getUrl());
        return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
    }

    @Override
    public ResponseResult uploadPicture(MultipartFile multipartFile) {

        // 1.检查参数
        if (multipartFile == null || multipartFile.getSize() == 0) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }
        // 2.上传图片到minIO中
        String fileName = UUID.randomUUID().toString().replaceAll("-", "");
        String originalFilename = multipartFile.getOriginalFilename();
        String postfix = originalFilename.substring(originalFilename.lastIndexOf("."));
        String fileUrl = null;
        try {
            fileUrl = fileStorageServicel.uploadImgFile("", fileName + postfix, multipartFile.getInputStream());
            log.info("上传图片到MinIO中,fileId:{}",fileUrl);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("WmMaterialServiceImpl-上传文件失败");
        }
        // 3.保存到数据库中
        WmMaterial wmMaterial = new WmMaterial();
        wmMaterial.setUserId(WmThreadLocalUtils.getUser().getId());
        wmMaterial.setUrl(fileUrl);
        wmMaterial.setIsCollection((short) 0);
        wmMaterial.setType((short) 0);
        wmMaterial.setCreatedTime(new Date());
        save(wmMaterial);
        // 4.返回结果
        return ResponseResult.okResult(wmMaterial);
    }

    @Override
    public ResponseResult findList(WmMaterialDto wmMaterialDto) {
        wmMaterialDto.checkParam();

        IPage<WmMaterial> page = new Page<>(wmMaterialDto.getPage(), wmMaterialDto.getSize());
        LambdaQueryWrapper<WmMaterial> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(wmMaterialDto.getIsCollection()!=null&&wmMaterialDto.getIsCollection()==1,WmMaterial::getIsCollection,wmMaterialDto.getIsCollection());
        queryWrapper.eq(WmMaterial::getUserId,WmThreadLocalUtils.getUser().getId());
        queryWrapper.orderByDesc(WmMaterial::getCreatedTime);

        IPage<WmMaterial> pageModel = page(page, queryWrapper);

        ResponseResult pageResponseResult = new PageResponseResult(wmMaterialDto.getPage(), wmMaterialDto.getSize(), (int) pageModel.getTotal());
        pageResponseResult.setData(page.getRecords());
        return pageResponseResult;
    }

    @Override
    public ResponseResult CollectOrUnById(Integer id) {

        WmMaterial material = getById(id);
        if (material == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }
        if (material.getIsCollection() == 1) {
            material.setIsCollection((short) 0);
        } else {
            material.setIsCollection((short) 1);
        }

        updateById(material);
        return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);

    }


}
