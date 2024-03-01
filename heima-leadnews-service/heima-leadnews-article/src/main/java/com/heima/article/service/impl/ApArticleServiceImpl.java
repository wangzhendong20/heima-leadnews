package com.heima.article.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.heima.article.mapper.ApArticleConfigMapper;
import com.heima.article.mapper.ApArticleMapper;
import com.heima.article.mapper.ApAtricleContentMapper;
import com.heima.article.service.ApArticleService;
import com.heima.article.service.ArticleFreemarkerService;
import com.heima.common.constants.ArticleConstants;
import com.heima.common.constants.BehaviorConstants;
import com.heima.common.redis.CacheService;
import com.heima.model.article.dtos.ArticleCommentDto;
import com.heima.model.article.dtos.ArticleDto;
import com.heima.model.article.dtos.ArticleHomeDto;
import com.heima.model.article.dtos.ArticleInfoDto;
import com.heima.model.article.pojos.ApArticle;
import com.heima.model.article.pojos.ApArticleConfig;
import com.heima.model.article.pojos.ApArticleContent;
import com.heima.model.article.vos.ArticleCommnetVo;
import com.heima.model.article.vos.HotArticleVo;
import com.heima.model.common.dtos.PageResponseResult;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.mess.ArticleVisitStreamMess;
import com.heima.model.user.pojos.ApUser;
import com.heima.model.wemedia.dtos.StatisticsDto;
import com.heima.utils.common.DateUtils;
import com.heima.utils.thread.AppThreadLocalUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;


@Service
@Transactional
@Slf4j
public class ApArticleServiceImpl  extends ServiceImpl<ApArticleMapper, ApArticle> implements ApArticleService {

    @Autowired
    private ArticleFreemarkerService articleFreemarkerService;
    // 单页最大加载的数字
    private final static short MAX_PAGE_SIZE = 50;

    @Autowired
    private ApArticleMapper apArticleMapper;
    @Autowired
    private ApArticleConfigMapper apArticleConfigMapper;
    @Autowired
    private ApAtricleContentMapper apAtricleContentMapper;
    @Autowired
    private CacheService cacheService;

    /**
     * 根据参数加载文章列表
     * @param loadtype 1为加载更多  2为加载最新
     * @param dto
     * @return
     */
    @Override
    public ResponseResult load(Short loadtype, ArticleHomeDto dto) {
        //1.校验参数
        Integer size = dto.getSize();
        if(size == null || size == 0){
            size = 10;
        }
        size = Math.min(size,MAX_PAGE_SIZE);
        dto.setSize(size);

        //类型参数检验
        if(!loadtype.equals(ArticleConstants.LOADTYPE_LOAD_MORE)&&!loadtype.equals(ArticleConstants.LOADTYPE_LOAD_NEW)){
            loadtype = ArticleConstants.LOADTYPE_LOAD_MORE;
        }
        //文章频道校验
        if(StringUtils.isEmpty(dto.getTag())){
            dto.setTag(ArticleConstants.DEFAULT_TAG);
        }

        //时间校验
        if(dto.getMaxBehotTime() == null) dto.setMaxBehotTime(new Date());
        if(dto.getMinBehotTime() == null) dto.setMinBehotTime(new Date());
        //2.查询数据
        List<ApArticle> apArticles = apArticleMapper.loadArticleList(dto, loadtype);

        //3.结果封装
        ResponseResult responseResult = ResponseResult.okResult(apArticles);
        return responseResult;
    }

    /**
     * 保存app端相关文章
     * @param articleDto
     * @return
     */
    @Override
//    @GlobalTransactional
    public ResponseResult saveArticle(ArticleDto articleDto) {
        //1.检查参数
        if(articleDto == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }

        ApArticle apArticle = new ApArticle();
        BeanUtils.copyProperties(articleDto,apArticle);

        //2.判断是否存在id
        if (apArticle.getId() == null) {
            //2.1 不存在id  保存  文章  文章配置  文章内容
            save(apArticle);

            ApArticleConfig apArticleConfig = new ApArticleConfig(apArticle.getId());
            apArticleConfigMapper.insert(apArticleConfig);

            ApArticleContent apArticleContent = new ApArticleContent();
            apArticleContent.setArticleId(apArticle.getId());
            apArticleContent.setContent(articleDto.getContent());
            apAtricleContentMapper.insert(apArticleContent);

        } else {
            //2.2 存在id   修改  文章  文章内容
            updateById(apArticle);

            ApArticleContent apArticleContent = apAtricleContentMapper.selectOne(Wrappers.<ApArticleContent>lambdaQuery().eq(ApArticleContent::getArticleId, apArticle.getId()));
            apArticleContent.setContent(articleDto.getContent());
            apAtricleContentMapper.updateById(apArticleContent);

        }

        //异步调用 生成静态文件上传到minio中
        articleFreemarkerService.buildArticleToMinIO(apArticle, articleDto.getContent());


        return ResponseResult.okResult(apArticle.getId());
    }

    @Override
    public ResponseResult loadArticleBehavior(ArticleInfoDto dto) {
        //检查参数
        if (dto == null || dto.getArticleId() == null || dto.getAuthorId() == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }

        boolean isfollow = false, islike = false, isunlike = false, iscollection = false;

        ApUser user = AppThreadLocalUtils.getUser();
        if (user != null) {
            //喜欢
            String likeJson = (String) cacheService.hGet(BehaviorConstants.LIKE_BEHAVIOR + dto.getArticleId(), user.getId().toString());
            if (StringUtils.isNotBlank(likeJson)) {
                islike = true;
            }

            String unLikeJson = (String) cacheService.hGet(BehaviorConstants.UN_LIKE_BEHAVIOR + dto.getArticleId(), user.getId().toString());
            if (StringUtils.isNotBlank(unLikeJson)) {
                isunlike = true;
            }

            String followJson = (String) cacheService.hGet(BehaviorConstants.APUSER_FOLLOW_RELATION + user.getId(), dto.getAuthorId().toString());
            if (StringUtils.isNotBlank(followJson)) {
                isfollow = true;
            }

            String collectionJson = (String) cacheService.hGet(BehaviorConstants.COLLECTION_BEHAVIOR + user.getId(), dto.getArticleId().toString());
            if (StringUtils.isNotBlank(collectionJson)) {
                iscollection = true;
            }
        }

        Map<String,Object> map = new HashMap<>();
        map.put("isfollow",isfollow);
        map.put("islike",islike);
        map.put("isunlike",isunlike);
        map.put("iscollection",iscollection);

        return ResponseResult.okResult(map);
    }

    /**
     * 根据参数加载文章列表
     *
     * @param loadtype  1为加载更多  2为加载最新
     * @param dto
     * @param firstPage true 是首页
     * @return
     */
    @Override
    public ResponseResult load2(Short loadtype, ArticleHomeDto dto, Boolean firstPage) {
        if (firstPage) {
            String jsonStr = cacheService.get(ArticleConstants.HOT_ARTICLE_FIRST_PAGE + dto.getTag());
            if (StringUtils.isNotBlank(jsonStr)) {
                List<HotArticleVo> hotArticleVoList = JSON.parseArray(jsonStr, HotArticleVo.class);
                return ResponseResult.okResult(hotArticleVoList);
            }
        }
        return load(loadtype,dto);
    }

    /**
     * 更新文章的分值 同时更新缓存中的热点文章数据
     *
     * @param mess
     */
    @Override
    public void updateScore(ArticleVisitStreamMess mess) {
        //1.更新文章的阅读、点赞、收藏、评论的数量
        ApArticle apArticle = updateArticle(mess);
        //2.计算文章分值
        Integer score = computeScore(apArticle);
        score = score * 3;
        //3.替换当前文章对应频道的热点数据
        replaceDataToRedis(apArticle,score,ArticleConstants.HOT_ARTICLE_FIRST_PAGE+apArticle.getChannelId());
        //4.替换推荐对应的热点数据
        replaceDataToRedis(apArticle,score,ArticleConstants.HOT_ARTICLE_FIRST_PAGE+ArticleConstants.DEFAULT_TAG);
    }

    /**
     * 查看文章评论列表
     * @param dto
     * @return
     */
    @Override
    public PageResponseResult findNewsComments(ArticleCommentDto dto) {
        Integer currentPage = dto.getPage();
        dto.setPage((dto.getPage()-1) * dto.getSize());
        List<ArticleCommnetVo> list = apArticleMapper.findNewsComments(dto);
        int count = apArticleMapper.findNewsCommentsCount(dto);

        PageResponseResult responseResult = new PageResponseResult(currentPage,dto.getSize(),count);
        responseResult.setData(list);
        return responseResult;
    }

    /**
     * 图文统计统计
     * @param wmUserId
     * @param beginDate
     * @param endDate
     * @return
     */
    @Override
    public ResponseResult queryLikesAndConllections(Integer wmUserId, Date beginDate, Date endDate) {
        Map map = apArticleMapper.queryLikesAndConllections(wmUserId,beginDate,endDate);
        return ResponseResult.okResult(map);
    }

    /**
     * 分页查询 图文统计
     * @param dto
     * @return
     */
    @Override
    public PageResponseResult newPage(StatisticsDto dto) {

        //类型转换
        Date beginDate = DateUtils.stringToDate(dto.getBeginDate());
        Date endDate = DateUtils.stringToDate(dto.getEndDate());

        //检查参数
        dto.checkParam();

        //分页查询
        IPage page = new Page(dto.getPage(),dto.getSize());
        LambdaQueryWrapper<ApArticle> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(ApArticle::getAuthorId,dto.getWmUserId());
        lambdaQueryWrapper.between(ApArticle::getPublishTime,beginDate,endDate);
        lambdaQueryWrapper.select(ApArticle::getId,ApArticle::getTitle,ApArticle::getLikes,ApArticle::getCollection,ApArticle::getComment,ApArticle::getViews);
        lambdaQueryWrapper.orderByDesc(ApArticle::getPublishTime);

        page = page(page,lambdaQueryWrapper);

        PageResponseResult responseResult = new PageResponseResult(dto.getPage(), dto.getSize(), (int) page.getTotal());
        responseResult.setData(page.getRecords());

        return responseResult;
    }

    private void replaceDataToRedis(ApArticle apArticle, Integer score, String s) {
        String articleListStr = cacheService.get(s);
        if (StringUtils.isNotBlank(articleListStr)) {
            List<HotArticleVo> hotArticleVoList = JSON.parseArray(articleListStr, HotArticleVo.class);

            boolean flag = true;

            //如果缓存中存在，只需更新分值
            for (HotArticleVo hotArticleVo : hotArticleVoList) {
                if (hotArticleVo.getId().equals(apArticle.getId())) {
                    hotArticleVo.setScore(score);
                    flag = false;
                    break;
                }
            }

            if (flag) {
                if (hotArticleVoList.size() >= 30) {
                    hotArticleVoList = hotArticleVoList.stream().sorted(Comparator.comparing(HotArticleVo::getScore).reversed()).collect(Collectors.toList());
                    HotArticleVo lastHot = hotArticleVoList.get(hotArticleVoList.size() - 1);
                    if (lastHot.getScore() < score) {
                        hotArticleVoList.remove(lastHot);
                        HotArticleVo hot = new HotArticleVo();
                        BeanUtils.copyProperties(apArticle,hot);
                        hot.setScore(score);
                        hotArticleVoList.add(hot);
                    }
                } else {
                    HotArticleVo hot = new HotArticleVo();
                    BeanUtils.copyProperties(apArticle,hot);
                    hot.setScore(score);
                    hotArticleVoList.add(hot);
                }
            }

            hotArticleVoList = hotArticleVoList.stream().sorted(Comparator.comparing(HotArticleVo::getScore).reversed()).collect(Collectors.toList());
            cacheService.set(s,JSON.toJSONString(hotArticleVoList));
        }
    }

    private ApArticle updateArticle(ArticleVisitStreamMess mess) {
        ApArticle apArticle = getById(mess.getArticleId());
        apArticle.setCollection(apArticle.getCollection()==null?0:apArticle.getCollection()+mess.getCollect());
        apArticle.setCollection(apArticle.getComment()==null?0:apArticle.getComment()+mess.getComment());
        apArticle.setCollection(apArticle.getLikes()==null?0:apArticle.getLikes()+mess.getLike());
        apArticle.setCollection(apArticle.getViews()==null?0:apArticle.getViews()+mess.getView());
        updateById(apArticle);
        return apArticle;
    }

    private Integer computeScore(ApArticle apArticle) {
        Integer score = 0;
        if (apArticle.getLikes() != null) {
            score += apArticle.getLikes() * ArticleConstants.HOT_ARTICLE_LIKE_WEIGHT;
        }
        if (apArticle.getViews() != null) {
            score += apArticle.getLikes() * ArticleConstants.HOT_ARTICLE_VIEW_WEIGHT;
        }
        if (apArticle.getComment() != null) {
            score += apArticle.getLikes() * ArticleConstants.HOT_ARTICLE_COMMENT_WEIGHT;
        }
        if (apArticle.getCollection() != null) {
            score += apArticle.getLikes() * ArticleConstants.HOT_ARTICLE_COLLECTION_WEIGHT;
        }
        return score;
    }
}
