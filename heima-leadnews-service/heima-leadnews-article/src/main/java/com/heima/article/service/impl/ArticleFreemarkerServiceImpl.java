package com.heima.article.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.heima.article.mapper.ApArticleMapper;
import com.heima.article.mapper.ApAtricleContentMapper;
import com.heima.article.service.ArticleFreemarkerService;
import com.heima.common.constants.ArticleConstants;
import com.heima.file.service.FileStorageService;
import com.heima.model.article.pojos.ApArticle;
import com.heima.model.search.vos.SearchArticleVo;
import freemarker.template.Configuration;
import freemarker.template.Template;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

@Service
public class ArticleFreemarkerServiceImpl implements ArticleFreemarkerService {
    @Autowired
    private ApAtricleContentMapper apAtricleContentMapper;
    @Autowired
    private Configuration configuration;
    @Autowired
    private FileStorageService fileStorageService;
    @Autowired
    private ApArticleMapper apArticleMapper;
    @Autowired
    private KafkaTemplate<String,String> kafkaTemplate;

    @Override
    @Async
    public void buildArticleToMinIO(ApArticle apArticle, String content) {

        // 获取文章内容
        if (content != null && StringUtils.isNotBlank(content)) {
            //文章内容通过freemarker生成Html文件
            StringWriter out = new StringWriter();
            Template template = null;
            try {
                template = configuration.getTemplate("article.ftl");
                //数据模型
                Map<String, Object> contentDataModel = new HashMap<>();
                contentDataModel.put("content", JSONArray.parseArray(content));
                //合成
                template.process(contentDataModel,out);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }


            //把html文件上传到minio中
            InputStream in = new ByteArrayInputStream(out.toString().getBytes());
            String url = fileStorageService.uploadHtmlFile("", apArticle.getId() + ".html", in);

            // 修改ap_article表，保存static_url字段
            apArticle.setId(apArticle.getId());
            apArticle.setStaticUrl(url);
            apArticleMapper.updateById(apArticle);


            //发送消息，创建索引
            createArticleESIndex(apArticle,content,url);
        }
    }

    /**
     * //发送消息，创建索引
     * @param apArticle
     * @param content
     * @param url
     */
    private void createArticleESIndex(ApArticle apArticle, String content, String url) {
        SearchArticleVo vo = new SearchArticleVo();
        BeanUtils.copyProperties(apArticle,vo);
        vo.setContent(content);
        vo.setStaticUrl(url);

        kafkaTemplate.send(ArticleConstants.ARTICLE_ES_SYNC_TOPIC, JSON.toJSONString(vo));
    }
}
