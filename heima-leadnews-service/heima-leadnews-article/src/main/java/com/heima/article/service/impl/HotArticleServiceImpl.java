package com.heima.article.service.impl;

import com.alibaba.fastjson.JSON;
import com.heima.apis.wemedia.IWemediaClient;
import com.heima.article.mapper.ApArticleMapper;
import com.heima.article.service.HotArticleService;
import com.heima.common.constants.ArticleConstants;
import com.heima.common.redis.CacheService;
import com.heima.model.article.pojos.ApArticle;
import com.heima.model.article.vos.HotArticleVo;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.wemedia.pojos.WmChannel;
import org.joda.time.DateTime;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class HotArticleServiceImpl implements HotArticleService {

    @Autowired
    private ApArticleMapper apArticleMapper;
    @Autowired
    private IWemediaClient wemediaClient;
    @Autowired
    private CacheService cacheService;
    /**
     * 计算热点文章
     */
    @Override
    public void computeHotArticle() {
        //1.查询前5天的文章数据
        Date dayParam = DateTime.now().minusDays(5).toDate();
        List<ApArticle> articleListLast5days =
                apArticleMapper.findArticleListByLast5days(dayParam);
        //2.计算文章的分数
        List<HotArticleVo> hotArticleVoList = computeHotArticle(articleListLast5days);

        //3.为每个频道缓存30条分值较高的文章
        cacheTagToRedis(hotArticleVoList);


    }

    private void cacheTagToRedis(List<HotArticleVo> hotArticleVoList) {
        //为每个频道缓存30条分值较高的文章
        ResponseResult responseResult = wemediaClient.getChannels();
        if (responseResult.getCode().equals(200)) {
            String channelJson = JSON.toJSONString(responseResult.getData());
            List<WmChannel> wmChannels = JSON.parseArray(channelJson, WmChannel.class);
            //检索出每个频道的文章
            if (!wmChannels.isEmpty() && wmChannels.size() > 0) {
                for (WmChannel wmChannel : wmChannels) {
                    List<HotArticleVo> hotArticleVos = hotArticleVoList.stream().filter(x -> x.getChannelId().equals(wmChannel.getId())).collect(Collectors.toList());
                    sortAndCache(ArticleConstants.HOT_ARTICLE_FIRST_PAGE+ wmChannel.getId(), hotArticleVos);
                }
            }
        }
        //设置推荐数据
        //给文章进行排序，取30条分值较高的文章存入redis key:频道id value:30条分值较高的文章
        sortAndCache(ArticleConstants.HOT_ARTICLE_FIRST_PAGE+ArticleConstants.DEFAULT_TAG,hotArticleVoList);
    }

    /**
     * 排序并缓存数据
     * @param key
     * @param hotArticleVos
     */
    private void sortAndCache(String key, List<HotArticleVo> hotArticleVos) {
        hotArticleVos = hotArticleVos.stream().sorted(Comparator.comparing(HotArticleVo::getScore).reversed()).collect(Collectors.toList());
        if (hotArticleVos.size() > 30) {
            hotArticleVos.subList(0,30);
        }
        cacheService.set(key,JSON.toJSONString(hotArticleVos));
    }

    /**
     * 计算文章分值
     * @param articleListLast5days
     * @return
     */
    private List<HotArticleVo> computeHotArticle(List<ApArticle> articleListLast5days) {

        List<HotArticleVo> hotArticleVoList = new ArrayList<>();

        if (!articleListLast5days.isEmpty() && articleListLast5days.size() > 0) {
            hotArticleVoList = articleListLast5days.stream().map(apArticle -> {
                HotArticleVo hotArticleVo = new HotArticleVo();
                BeanUtils.copyProperties(apArticle, hotArticleVo);
                Integer score = computeScore(apArticle);
                hotArticleVo.setScore(score);
                return hotArticleVo;
            }).collect(Collectors.toList());
        }
        return hotArticleVoList;
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
