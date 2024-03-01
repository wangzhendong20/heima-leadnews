package com.heima.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.heima.apis.wemedia.IWemediaClient;
import com.heima.common.constants.UserConstants;
import com.heima.model.common.dtos.PageResponseResult;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.user.dtos.AuthDto;
import com.heima.model.user.pojos.ApUser;
import com.heima.model.user.pojos.ApUserRealname;
import com.heima.model.wemedia.pojos.WmUser;
import com.heima.user.mapper.ApUserMapper;
import com.heima.user.mapper.ApUserRealnameMapper;
import com.heima.user.service.ApUserRealnameService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
@Slf4j
public class ApUserRealnameServiceImpl extends ServiceImpl<ApUserRealnameMapper, ApUserRealname> implements ApUserRealnameService {
    @Autowired
    private ApUserMapper apUserMapper;
    @Autowired
    private IWemediaClient wemediaClient;

    /**
     * 按照状态分页查询用户列表
     *
     * @param dto
     * @return
     */
    @Override
    public ResponseResult loadListByStatus(AuthDto dto) {

        if (dto == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }

        //分页条件检查
        dto.checkParam();

        IPage page = new Page(dto.getPage(),dto.getSize());
        LambdaQueryWrapper<ApUserRealname> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        if (dto.getStatus() != null) {
            lambdaQueryWrapper.eq(ApUserRealname::getStatus,dto.getStatus());
        }
        IPage pageModel = page(page,lambdaQueryWrapper);

        PageResponseResult pageResponseResult = new PageResponseResult(dto.getPage(), dto.getSize(), (int) pageModel.getTotal());
        pageResponseResult.setData(pageModel.getRecords());
        return pageResponseResult;
    }

    /**
     * @param dto
     * @param status 2审核失败    9审核成功
     * @return
     */
    @Override
    public ResponseResult updateStatus(AuthDto dto, Short status) {

        if (dto == null || dto.getId() == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }

        //2.修改认证状态
        ApUserRealname apUserRealname = new ApUserRealname();
        apUserRealname.setId(dto.getId());
        apUserRealname.setStatus(status);
        if (StringUtils.isNotBlank(dto.getMsg())) {
            apUserRealname.setReason(dto.getMsg());
        }
        updateById(apUserRealname);

        if (status.equals(UserConstants.PASS_AUTH)) {
            ResponseResult responseResult = createWmUserAndAuthor(dto);
            if(responseResult != null){
                return responseResult;
            }
        }
        return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
    }

    /**
     * 创建自媒体账户
     * app端用户申请成为自媒体人，然后发出申请，审核通过后根据App端用户信息创建自媒体账户。
     * @param dto
     * @return
     */
    private ResponseResult createWmUserAndAuthor(AuthDto dto) {
        Integer userRealnameId = dto.getId();
        //查询用户认证信息
        ApUserRealname apUserRealname = getById(userRealnameId);
        if (apUserRealname == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.DATA_NOT_EXIST);
        }
        //查询app端用户信息
        Integer userId = apUserRealname.getUserId();
        ApUser apUser = apUserMapper.selectById(userId);
        if (apUser == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.DATA_NOT_EXIST);
        }
        //创建自媒体账户
        WmUser wmUser = wemediaClient.findWmUserByName(apUser.getName());
        if (wmUser == null) {
            wmUser = new WmUser();
            BeanUtils.copyProperties(apUser,wmUser);
            wmUser.setCreatedTime(new Date());
            wmUser.setStatus(9);
            wemediaClient.saveWmUser(wmUser);
        }
        apUser.setFlag((short) 1);
        apUserMapper.updateById(apUser);

        return null;
    }
}
