package com.heima.article.test;

import com.alibaba.fastjson.JSONArray;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.heima.article.ArticleApplication;
import com.heima.article.mapper.ApArticleMapper;
import com.heima.article.mapper.ApAtricleContentMapper;
import com.heima.file.service.FileStorageService;
import com.heima.model.article.pojos.ApArticle;
import com.heima.model.article.pojos.ApArticleContent;
import freemarker.template.Configuration;
import freemarker.template.Template;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;


@SpringBootTest(classes = ArticleApplication.class)
@RunWith(SpringRunner.class)
public class ArticleFreemarkerTest {
    @Autowired
    private ApAtricleContentMapper apAtricleContentMapper;
    @Autowired
    private Configuration configuration;
    @Autowired
    private FileStorageService fileStorageService;
    @Autowired
    private ApArticleMapper apArticleMapper;

    @Test
    public void createStaticUrlTest() throws Exception {
        // 获取文章内容
        ApArticleContent apArticleContent = apAtricleContentMapper.selectOne(
                Wrappers.<ApArticleContent>lambdaQuery().eq(ApArticleContent::getArticleId, "1383827787629252610L"));
        if (apArticleContent != null && StringUtils.isNotBlank(apArticleContent.getContent())) {
            //文章内容通过freemarker生成Html文件
            StringWriter out = new StringWriter();
            Template template = configuration.getTemplate("article.ftl");

            Map<String, Object> params = new HashMap<>();
            params.put("content", JSONArray.parseArray(apArticleContent.getContent()));

            template.process(params,out);
            //把html文件上传到minio中
            InputStream in = new ByteArrayInputStream(out.toString().getBytes());
            String url = fileStorageService.uploadHtmlFile("", apArticleContent.getArticleId() + ".html", in);

            // 修改ap_article表，保存static_url字段
            ApArticle apArticle = new ApArticle();
            apArticle.setId(apArticleContent.getArticleId());
            apArticle.setStaticUrl(url);
            apArticleMapper.updateById(apArticle);

        }

    }
}
