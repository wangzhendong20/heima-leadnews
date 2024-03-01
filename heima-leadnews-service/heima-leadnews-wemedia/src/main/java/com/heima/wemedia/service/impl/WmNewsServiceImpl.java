package com.heima.wemedia.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.heima.common.constants.WemediaConstants;
import com.heima.common.constants.WmNewsMessageConstants;
import com.heima.common.exception.CustomException;
import com.heima.model.common.dtos.PageResponseResult;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.wemedia.dtos.NewsAuthDto;
import com.heima.model.wemedia.dtos.WmNewsDto;
import com.heima.model.wemedia.dtos.WmNewsPageReqDto;
import com.heima.model.wemedia.pojos.WmMaterial;
import com.heima.model.wemedia.pojos.WmNews;
import com.heima.model.wemedia.pojos.WmNewsMaterial;
import com.heima.model.wemedia.pojos.WmUser;
import com.heima.model.wemedia.vo.WmNewsVo;
import com.heima.utils.thread.WmThreadLocalUtils;
import com.heima.wemedia.mapper.WmMaterialMapper;
import com.heima.wemedia.mapper.WmNewsMapper;
import com.heima.wemedia.mapper.WmNewsMaterialMapper;
import com.heima.wemedia.mapper.WmUserMapper;
import com.heima.wemedia.service.WmNewsAutoScanService;
import com.heima.wemedia.service.WmNewsService;
import com.heima.wemedia.service.WmNewsTaskService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;


@Service
@Slf4j
@Transactional
public class WmNewsServiceImpl  extends ServiceImpl<WmNewsMapper, WmNews> implements WmNewsService {

    @Autowired
    private WmNewsMaterialMapper wmNewsMaterialMapper;
    @Autowired
    private WmMaterialMapper wmMaterialMapper;
    @Autowired
    private WmNewsAutoScanService wmNewsAutoScanService;
    @Autowired
    private WmNewsTaskService wmNewsTaskService;
    @Autowired
    private KafkaTemplate<String,String> kafkaTemplate;
    @Autowired
    private WmNewsMapper wmNewsMapper;
    @Autowired
    private WmUserMapper wmUserMapper;

    /**
     * 查询文章
     * @param dto
     * @return
     */
    @Override
    public ResponseResult findAll(WmNewsPageReqDto dto) {

        //1.检查参数
        if(dto == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }
        //分页参数检查
        dto.checkParam();
        //获取当前登录人的信息
        WmUser user = WmThreadLocalUtils.getUser();
        if(user == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.NEED_LOGIN);
        }

        IPage<WmNews> page = new Page<>(dto.getPage(),dto.getSize());
        LambdaQueryWrapper<WmNews> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        //状态精确查询
        if(dto.getStatus() != null){
            lambdaQueryWrapper.eq(WmNews::getStatus,dto.getStatus());
        }

        //频道精确查询
        if(dto.getChannelId() != null){
            lambdaQueryWrapper.eq(WmNews::getChannelId,dto.getChannelId());
        }

        //时间范围查询
        if(dto.getBeginPubDate()!=null && dto.getEndPubDate()!=null){
            lambdaQueryWrapper.between(WmNews::getPublishTime,dto.getBeginPubDate(),dto.getEndPubDate());
        }

        //关键字模糊查询
        if(StringUtils.isNotBlank(dto.getKeyword())){
            lambdaQueryWrapper.like(WmNews::getTitle,dto.getKeyword());
        }

        //查询当前登录用户的文章
        lambdaQueryWrapper.eq(WmNews::getUserId,user.getId());

        //发布时间倒序查询
        lambdaQueryWrapper.orderByDesc(WmNews::getCreatedTime);

        IPage<WmNews> pageModel = page(page, lambdaQueryWrapper);

        ResponseResult responseResult = new PageResponseResult(dto.getPage(), dto.getSize(), (int) pageModel.getTotal());
        responseResult.setData(pageModel.getRecords());

        return responseResult;
    }

    /**
     * 发布修改文章或保存为草稿
     * @param dto
     * @return
     */
    @Override
    public ResponseResult submitNews(WmNewsDto dto) {
        if(dto == null || dto.getContent() == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }

        //1.保存或修改文章
        WmNews wmNews = new WmNews();
        //属性拷贝 属性名词和类型相同才能拷贝
        BeanUtils.copyProperties(dto,wmNews);
        //封面图片  list---> string
        if(dto.getImages() != null && dto.getImages().size() > 0){
            //[1dddfsd.jpg,sdlfjldk.jpg]-->   1dddfsd.jpg,sdlfjldk.jpg
            String imageStr = StringUtils.join(dto.getImages(), ",");
            wmNews.setImages(imageStr);
        }
        //如果当前封面类型为自动 -1
        if(dto.getType().equals(WemediaConstants.WM_NEWS_TYPE_AUTO)){
            wmNews.setType(null);
        }

        saveOrUpdateWmNews(wmNews);

        //2.判断是否为草稿  如果为草稿结束当前方法
        if(dto.getStatus().equals(WmNews.Status.NORMAL.getCode())){
            return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
        }

        //3.不是草稿，保存内容图片与素材的关系
        //获取到文章内容中的图片信息
        List<String> materials = extractUrlInfo(dto.getContent());
        saveRelativeInfoForContent(materials,wmNews.getId());

        //4.不是草稿，保存文章封面图片与素材的关系，如果当前布局是自动，需要匹配封面图片
        saveRelativeInfoForCover(dto,wmNews,materials);

        //审核文章
//        wmNewsAutoScanService.autoScanWmNews(wmNews.getId());
        wmNewsTaskService.addNewsToTask(wmNews.getId(),wmNews.getPublishTime());

        return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);

    }

    @Override
    public ResponseResult lookDetail(Integer id) {
        if(id == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }

        WmNews wmNews = getById(id);
        if (wmNews == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.DATA_NOT_EXIST);
        }

        return ResponseResult.okResult(wmNews);
    }

    @Override
    public ResponseResult deleteNews(Integer id) {

        if (id == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }

        WmNews wmNews = getById(id);
        if (wmNews == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.DATA_NOT_EXIST);
        }

        if (wmNews.getStatus().equals(WmNews.Status.PUBLISHED)) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }

        wmNewsMaterialMapper.delete(Wrappers.<WmNewsMaterial>lambdaQuery().eq(WmNewsMaterial::getNewsId,wmNews.getId()));
        baseMapper.delete(Wrappers.<WmNews>lambdaQuery().eq(WmNews::getId,wmNews.getId()));

        return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
    }

    @Override
    public ResponseResult downOrUp(WmNewsDto wmNewsDto) {

        if (wmNewsDto.getId() == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }
        WmNews wmNews = getById(wmNewsDto.getId());
        if (wmNews == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.DATA_NOT_EXIST);
        }
        if (!wmNews.getStatus().equals(WmNews.Status.PUBLISHED)) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }
        //修改文章enable
        if(wmNewsDto.getEnable() != null && wmNewsDto.getEnable() > -1 && wmNewsDto.getEnable() < 2) {
            wmNews.setEnable(wmNewsDto.getEnable());
            updateById(wmNews);
            //发送消息，通知article修改文章的配置
            if (wmNews.getArticleId() != null) {
                Map<String,Object> map  = new HashMap<>();
                map.put("articleId",wmNews.getArticleId());
                map.put("enable",wmNewsDto.getEnable());
                kafkaTemplate.send(WmNewsMessageConstants.WM_NEWS_UP_OR_DOWN_TOPIC,JSON.toJSONString(map));
            }
        }
        return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
    }

    /**
     * 文章审核的文章列表查询
     * @param newsAuthDto
     * @return
     */
    @Override
    public ResponseResult findList(NewsAuthDto newsAuthDto) {

        if (newsAuthDto == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }

        newsAuthDto.checkParam();
        //记录当前页
        Integer currentPage = newsAuthDto.getPage();
        //2.分页查询+count查询
        newsAuthDto.setPage((newsAuthDto.getPage()-1) * newsAuthDto.getSize());
        //需要查询文章作者姓名，所以需要连表查询
        List<WmNewsVo> wmNewsVolist = wmNewsMapper.findListAndPage(newsAuthDto);
        int count = wmNewsMapper.findListCount(newsAuthDto);

        PageResponseResult pageResponseResult = new PageResponseResult(currentPage, newsAuthDto.getSize(), count);
        pageResponseResult.setData(wmNewsVolist);
        return pageResponseResult;
    }

    @Override
    public ResponseResult findWmNewsVo(Integer id) {
        //1.检查参数
        if(id == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }

        WmNews wmNews = getById(id);
        if(wmNews == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.DATA_NOT_EXIST);
        }

        WmUser wmUser = wmUserMapper.selectById(wmNews.getUserId());

        WmNewsVo wmNewsVo = new WmNewsVo();
        BeanUtils.copyProperties(wmNews,wmNewsVo);
        if (wmUser != null) {
            wmNewsVo.setAuthorName(wmUser.getName());
        }
        return ResponseResult.okResult(wmNewsVo);
    }

    @Override
    public ResponseResult updateStatus(Short status, NewsAuthDto dto) {
        if (dto == null || dto.getId() == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }

        //2.查询文章信息
        WmNews wmNews = getById(dto.getId());
        if (wmNews == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.DATA_NOT_EXIST);
        }
        //3.修改文章的状态
        wmNews.setStatus(status);
        if (StringUtils.isNotBlank(dto.getMsg())) {
            wmNews.setReason(dto.getMsg());
        }
        updateById(wmNews);

        //审核成功，则需要创建app端文章数据，并修改自媒体文章
        if (status.equals(WemediaConstants.WM_NEWS_AUTH_PASS)) {
            //创建app端文章数据
            ResponseResult responseResult = wmNewsAutoScanService.saveAppArticle(wmNews);
            if (responseResult.getCode().equals(200)) {
                wmNews.setArticleId((Long) responseResult.getData());
                wmNews.setStatus(WmNews.Status.PUBLISHED.getCode());
                updateById(wmNews);
            }
        }
        return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
    }

    /**
     * 第一个功能：如果当前封面类型为自动，则设置封面类型的数据
     * 匹配规则：
     * 1，如果内容图片大于等于1，小于3  单图  type 1
     * 2，如果内容图片大于等于3  多图  type 3
     * 3，如果内容没有图片，无图  type 0
     *
     * 第二个功能：保存封面图片与素材的关系
     * @param dto
     * @param wmNews
     * @param materials
     */
    private void saveRelativeInfoForCover(WmNewsDto dto, WmNews wmNews, List<String> materials) {
        List<String> images = dto.getImages();

        //如果当前封面类型为自动，则设置封面类型的数据
        if (dto.getType().equals(WemediaConstants.WM_NEWS_TYPE_AUTO)) {
            if (materials.size() >= 3) {
                wmNews.setType(WemediaConstants.WM_NEWS_MANY_IMAGE);
                images = materials.stream().limit(3).collect(Collectors.toList());
            } else if (materials.size() >= 1 && materials.size() < 3) {
                wmNews.setType(WemediaConstants.WM_NEWS_SINGLE_IMAGE);
                images = materials.stream().limit(1).collect(Collectors.toList());
            } else {
                wmNews.setType(WemediaConstants.WM_NEWS_NONE_IMAGE);
            }

            if (images != null && images.size() > 0) {
                wmNews.setImages(StringUtils.join(images,","));
            }
            updateById(wmNews);
        }
        //第二个功能：保存封面图片与素材的关系
        if (images != null && images.size() > 0) {
            saveRelativeInfo(images, wmNews.getId(), WemediaConstants.WM_COVER_REFERENCE);
        }
    }

    private void saveRelativeInfoForContent(List<String> materials, Integer newsId) {
        saveRelativeInfo(materials,newsId,WemediaConstants.WM_CONTENT_REFERENCE);
    }

    private void saveRelativeInfo(List<String> materials, Integer newsId, Short type) {
        if(materials != null && !materials.isEmpty()){
            //通过图片的url查询素材的id
            List<WmMaterial> dbMaterials = wmMaterialMapper.selectList(Wrappers.<WmMaterial>lambdaQuery().in(WmMaterial::getUrl, materials));
            //判断素材是否有效
            if(dbMaterials==null || dbMaterials.size() == 0){
                //手动抛出异常   第一个功能：能够提示调用者素材失效了，第二个功能，进行数据的回滚
                throw new CustomException(AppHttpCodeEnum.MATERIASL_REFERENCE_FAIL);
            }
            if(materials.size() != dbMaterials.size()){
                throw new CustomException(AppHttpCodeEnum.MATERIASL_REFERENCE_FAIL);
            }

            List<Integer> idList = dbMaterials.stream().map(WmMaterial::getId).collect(Collectors.toList());

            //批量保存
            wmNewsMaterialMapper.saveRelations(idList,newsId,type);
        }
    }

    /**
     * 提取文章内容中的图片信息
     * @param content
     * @return
     */
    private List<String> extractUrlInfo(String content) {

        List<String> materials = new ArrayList<>();

        List<Map> maps = JSON.parseArray(content,Map.class);
        for (Map map : maps) {
            if (map.get("type").equals("image")) {
                String imgUrl = (String) map.get("value");
                materials.add(imgUrl);
            }
        }
        return materials;
    }


    /**
     * 保存或修改文章
     * @param wmNews
     */
    private void saveOrUpdateWmNews(WmNews wmNews) {
        //补全属性
        wmNews.setUserId(WmThreadLocalUtils.getUser().getId());
        wmNews.setCreatedTime(new Date());
        wmNews.setSubmitedTime(new Date());
        wmNews.setEnable((short)1);//默认上架

        if(wmNews.getId() == null){
            //保存
            save(wmNews);
        }else {
            //修改
            //删除文章图片与素材的关系
            wmNewsMaterialMapper.delete(Wrappers.<WmNewsMaterial>lambdaQuery().eq(WmNewsMaterial::getNewsId,wmNews.getId()));
            updateById(wmNews);
        }
    }

}
