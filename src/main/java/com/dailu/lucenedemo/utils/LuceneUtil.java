package com.dailu.lucenedemo.utils;

import com.dailu.lucenedemo.entity.Story;
import com.dailu.lucenedemo.exception.AppRuntimeException;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class LuceneUtil {

    @Value("${lucene.indexPath}")
    private String indexPath;

    private static final Analyzer standardAnalyzer = new StandardAnalyzer();
    private static Directory directory;

    @PostConstruct
    public void init() throws IOException {
        directory = FSDirectory.open(Paths.get(indexPath));
        //索引库还可以存放到内存中
        //Directory directory = new RAMDirectory();
    }

    public static void addStoryIndex(Story story) {
        Field id = LuceneUtil.buildField("id", story.getId(), true, true);
        Field content = LuceneUtil.buildField("content", story.getContent(), true, false);
        Field title = LuceneUtil.buildField("title", story.getTitle(), true, false);
        Field author = LuceneUtil.buildField("author", story.getAuthor(), true, true);
        Field price = LuceneUtil.buildField("price", String.valueOf(story.getPrice()), true, true);
        Document document = new Document();
        Arrays.asList(id, content, title, author, price).forEach(document::add);
        addIndex(document);
    }

    public static void addIndex(Document document) {
        try (IndexWriter indexWriter = new IndexWriter(directory, new IndexWriterConfig(standardAnalyzer))) {
            //先删后增
            indexWriter.updateDocument(new Term("id", document.getField("id").stringValue()), document);
        } catch (IOException e) {
            throw new AppRuntimeException(e);
        }
    }

    //    @SuppressWarnings("all")
    public static <T> List<T> query(String property, String value, int limits, Class<T> resultType) {
        IndexSearcher indexSearcher = getIndexSearcher();
        TopDocs topDocs;
        try {
            topDocs = indexSearcher.search(new QueryParser(property, standardAnalyzer).parse(value), limits);
        } catch (Exception e) {
            throw new AppRuntimeException(e);
        }
        // 总结果数
        int count = (int) topDocs.totalHits.value;
        log.info("总结果数：" + count);
        List<T> results = Arrays.stream(topDocs.scoreDocs).map(scoreDoc -> {
            // 相关度得分
            float score = scoreDoc.score;
            log.info("得分：" + score);
            // Document在数据库的内部编号(是唯一的，由lucene自动生成)
            int docId = scoreDoc.doc;
            T t;
            // 根据编号取出真正的Document数据
            try {
                Document doc = indexSearcher.doc(docId);
                t = resultType.newInstance();
                Arrays.stream(resultType.getDeclaredFields()).forEach(field -> {
                    String fieldName = field.getName();
                    IndexableField indexableField = doc.getField(fieldName);
                    if (indexableField != null) {
                        try {
                            PropertyDescriptor pd = new PropertyDescriptor(fieldName, resultType);
                            Method writeMethod = pd.getWriteMethod();
                            Class<?> parameterType = writeMethod.getParameterTypes()[0];
                            if (parameterType.equals(String.class)) {
                                writeMethod.invoke(t, indexableField.stringValue());
                            } else if (parameterType.equals(Integer.class) || parameterType.equals(int.class)) {
                                writeMethod.invoke(t, Integer.parseInt(indexableField.stringValue()));
                            }
                        } catch (Exception e) {
                            log.error(e.getMessage(), e);
                        }
                    }
                });
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                throw new AppRuntimeException(e);
            }
            return t;
        }).collect(Collectors.toList());
        try {
            indexSearcher.getIndexReader().close();
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
        return results;
    }

    public static void removeIndex(String id) {
        //创建一个查询条件
        Query query = new TermQuery(new Term("id", id));
        //根据查询条件删除
        try (IndexWriter indexWriter = new IndexWriter(directory, new IndexWriterConfig(standardAnalyzer))) {
            indexWriter.deleteDocuments(query);
        } catch (IOException e) {
            throw new AppRuntimeException(e);
        }
    }

    public static void removeAll() {
        try (IndexWriter indexWriter = new IndexWriter(directory, new IndexWriterConfig(standardAnalyzer))) {
            indexWriter.deleteAll();
        } catch (IOException e) {
            throw new AppRuntimeException(e);
        }
    }

    public static void updateIndex(Story story) {
        Document newDocument = new Document();
        newDocument.add(new StringField("id", story.getId(), Field.Store.YES));
        newDocument.add(new TextField("content", story.getContent(), Field.Store.YES));
        newDocument.add(new TextField("title", story.getTitle(), Field.Store.YES));
        newDocument.add(new StringField("author", story.getAuthor(), Field.Store.YES));
        //修改  参数一为条件  参数二为修改的文档值
        try (IndexWriter indexWriter = new IndexWriter(directory, new IndexWriterConfig(standardAnalyzer))) {
            indexWriter.updateDocument(new Term("id", story.getId()), newDocument);
        } catch (IOException e) {
            throw new AppRuntimeException(e);
        }
    }


    public static Field buildField(String key, String value, boolean store, boolean completeMatch) {
        Field.Store st = store ? Field.Store.YES : Field.Store.NO;
        return completeMatch ? new StringField(key, value, st) : new TextField(key, value, st);
    }

    private static IndexSearcher getIndexSearcher() {
        IndexReader reader;
        IndexSearcher indexSearcher;
        try {
            reader = DirectoryReader.open(directory);
            indexSearcher = new IndexSearcher(reader);
        } catch (IOException e) {
            throw new AppRuntimeException(e);
        }
        return indexSearcher;
    }

}
