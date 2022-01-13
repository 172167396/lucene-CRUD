package com.dailu.lucenedemo;

import com.dailu.lucenedemo.entity.Story;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

@SpringBootTest
class LuceneDemoApplicationTests {

    @Test
    void contextLoads() throws IOException {
        Path path = Paths.get("storage/index");
        Directory directory = FSDirectory.open(path);
        //索引库还可以存放到内存中
        //Directory directory = new RAMDirectory();
        //创建一个标准分析器
        Analyzer analyzer = new StandardAnalyzer();
        //创建indexwriterCofig对象
        IndexWriterConfig config = new IndexWriterConfig(analyzer);
        //创建indexwriter对象
        IndexWriter indexWriter = new IndexWriter(directory, config);

        List<Story> stories = Arrays.asList(
                new Story("00001", "聊斋志异", "各种短篇鬼故事", "清代小说家蒲松龄",20),
                new Story("00002", "西游记", "孙悟空、猪八戒、沙僧、唐僧等人翻山越岭西天取经的故事,一路上降服各种妖魔鬼怪", "吴承恩",25),
                new Story("00003", "水浒传", "108将聚义梁山泊，劫富济贫", "施耐庵",30),
                new Story("00004", "三国演义", "刘关张结义，魏蜀吴三足鼎立", "罗贯中",35));
        stories.forEach(s -> {
            Field id = new StringField("id", s.getId(), Field.Store.YES);
            //StringField的话无法分词查找
            Field fileNameField = new TextField("title", s.getTitle(), Field.Store.YES);
            Field fileContentField = new TextField("content", s.getContent(), Field.Store.NO);
            Field author = new StringField("author", s.getAuthor(), Field.Store.YES);
            Document document = new Document();
            document.add(id);
            document.add(fileNameField);
            document.add(fileContentField);
            document.add(author);
            //创建索引，并写入索引库
            try {
                indexWriter.addDocument(document);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        //关闭indexwriter
        indexWriter.close();
    }

    @Test
    public void find() throws IOException, ParseException {
        Directory directory = FSDirectory.open(Paths.get("storage/index"));// 索引库目录
        Analyzer analyzer = new StandardAnalyzer();
        // 1、把查询字符串转为查询对象(存储的都是二进制文件，普通的String肯定无法查询，因此需要转换)
        QueryParser queryParser = new QueryParser("content", analyzer);// 只在内容里面查询
        Query query = queryParser.parse("魔鬼");
        // 2、查询，得到中间结果
        IndexReader indexReader = DirectoryReader.open(directory);
        IndexSearcher indexSearcher = new IndexSearcher(indexReader);
        TopDocs topDocs = indexSearcher.search(query, 100);// 根据指定查询条件查询，只返回前n条结果
        int count = (int) topDocs.totalHits.value;// 总结果数
        System.out.println("总条数：" + count);
        ScoreDoc[] scoreDocs = topDocs.scoreDocs;// 按照得分进行排序后的前n条结果的信息

        for (ScoreDoc scoreDoc : scoreDocs) {
            float score = scoreDoc.score;// 相关度得分
            int docId = scoreDoc.doc; // Document在数据库的内部编号(是唯一的，由lucene自动生成)
            // 根据编号取出真正的Document数据
            Document doc = indexSearcher.doc(docId);
            // 把Document转成Article
            Story story = new Story(
                    doc.getField("id").stringValue(),
                    doc.getField("title").stringValue(),
                    null,
                    doc.getField("author").stringValue(),
                    doc.getField("price").numericValue().intValue()
            );
            System.out.println(story + "得分：" + score);
        }
        indexReader.close();
    }
}
