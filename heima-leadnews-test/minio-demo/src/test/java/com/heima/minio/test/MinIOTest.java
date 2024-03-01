package com.heima.minio.test;


import com.heima.file.service.FileStorageService;
import com.heima.minio.MinIOApplication;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.FileInputStream;
import java.io.FileNotFoundException;

@SpringBootTest(classes = MinIOApplication.class)
@RunWith(SpringRunner.class)
public class MinIOTest {

    @Autowired
    private FileStorageService fileStorageService;

    //把list.html文件上传到minio中，并且可以在浏览器中访问

//    @Test
//    public void test() throws FileNotFoundException {
//        FileInputStream fileInputStream = new FileInputStream("D:\\BaiduNetdiskDownload\\3、黑马程序员Java微服务项目《黑马头条》\\day02-app端文章查看，静态化freemarker,分布式文件系统minIO\\资料\\模板文件\\plugins\\css\\index.css");
//        String path = fileStorageService.uploadHtmlFile("", "index.css", fileInputStream);
//        /*FileInputStream fileInputStream = new FileInputStream("E:\\tmp\\ak47.jpg");
//        String path = fileStorageService.uploadImgFile("", "ak47.jpg", fileInputStream);
//        System.out.println(path);*/
//    }





    /**
     * 把list.html文件上传到minio中，并且可以在浏览器中访问
     * @param args
     */
    public static void main(String[] args) {

        try {
            FileInputStream fileInputStream = new FileInputStream("D:\\BaiduNetdiskDownload\\3、黑马程序员Java微服务项目《黑马头条》\\day02-app端文章查看，静态化freemarker,分布式文件系统minIO\\资料\\模板文件\\plugins\\js\\axios.min.js");

            //1，获取minio的链接信息  创建一个minio的客户端
            MinioClient minioClient = MinioClient.builder().credentials("minioadmin", "minioadmin").endpoint("http://localhost:9000").build();


            //2.上传
            PutObjectArgs putObjectArgs = PutObjectArgs.builder()
                    .object("plugins/js/axios.min.js") //文件名词
                    .contentType("text/js") //文件类型
                    .bucket("leadnews") //桶名称  与minio管理界面创建的桶一致即可
                    .stream(fileInputStream,fileInputStream.available(),-1).build();
            minioClient.putObject(putObjectArgs);

            //访问路径
            System.out.println("http://localhost:9000/leadnews/list.html");
        } catch (Exception e) {
            e.printStackTrace();
        }



    }

}
