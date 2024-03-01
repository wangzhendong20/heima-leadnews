package com.heima.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.heima.admin.mapper.AdUserMapper;
import com.heima.admin.service.AdUserService;
import com.heima.model.admin.dtos.AdUserDto;
import com.heima.model.admin.pojos.AdUser;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.utils.common.AppJwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class AdUserServiceImpl extends ServiceImpl<AdUserMapper, AdUser> implements AdUserService {
    @Override
    public ResponseResult login(AdUserDto dto) {
        //1.检查参数
        if(StringUtils.isBlank(dto.getName()) || StringUtils.isBlank(dto.getPassword())){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID,"用户名或密码为空");
        }

        //2.查询用户
        LambdaQueryWrapper<AdUser> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(AdUser::getName,dto.getName());
        AdUser adUser = getOne(lambdaQueryWrapper);
        if (adUser == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.DATA_NOT_EXIST);
        }

        //3.比对密码
        String salt = adUser.getSalt();
        String password = dto.getPassword();
        String passwordStr = DigestUtils.md5DigestAsHex((password + salt).getBytes());
        if (passwordStr.equals(adUser.getPassword())) {
            //4.返回数据  jwt
            Map<String,Object> map = new HashMap<>();
            map.put("token", AppJwtUtil.getToken(adUser.getId().longValue()));
            adUser.setSalt("");
            adUser.setPassword("");
            map.put("user",adUser);
            return ResponseResult.okResult(map);
        } else {
            return ResponseResult.errorResult(AppHttpCodeEnum.LOGIN_PASSWORD_ERROR);
        }

    }
}
