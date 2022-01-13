package com.dailu.lucenedemo.controller;

import com.dailu.lucenedemo.entity.Story;
import com.dailu.lucenedemo.utils.LuceneUtil;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;


@RestController
public class LuceneController {

    private final Map<String, Story> stories = new ConcurrentHashMap<>();


    @GetMapping("/init")
    public String init() {
        List<Story> storyList = Arrays.asList(
                new Story("00001", "聊斋志异", "各种短篇鬼故事", "清代小说家蒲松龄", 20),
                new Story("00002", "西游记", "孙悟空、猪八戒、沙僧、唐僧等人翻山越岭西天取经的故事,一路上降服各种妖魔鬼怪", "吴承恩", 25),
                new Story("00003", "水浒传", "108将聚义梁山泊，劫富济贫", "施耐庵", 30),
                new Story("00004", "三国演义", "刘关张结义，魏蜀吴三足鼎立", "罗贯中", 35));
        storyList.forEach(s -> {
            stories.put(s.getId(), s);
            LuceneUtil.addStoryIndex(s);
        });
        return "success";
    }

    @PostMapping("/save")
    public String save(@RequestBody Story story) {
        if (ObjectUtils.isEmpty(story.getId())) {
            story.setId(UUID.randomUUID().toString().replace("-", ""));
        }
        stories.put(story.getId(), story);
        LuceneUtil.addStoryIndex(story);
        return "success";
    }

    @GetMapping("/query/{content}")
    public List<Story> query(@PathVariable String content) {
        List<Story> result = LuceneUtil.query("content", content, 100, Story.class);
        result.forEach(s -> s.setContent(stories.get(s.getId()).getContent()));
        return result;
    }

    @PostMapping("/update")
    public String update(@RequestBody Story story) {
        Story story1 = stories.get(story.getId());
        if (story1 == null) {
            return "该小说不存在";
        }
        stories.put(story.getId(), story);
        LuceneUtil.updateIndex(story);
        return "success";
    }

    @GetMapping("/delete/{id}")
    public String delete(@PathVariable String id) {
        Story story = stories.get(id);
        if (story == null) {
            return "该小说不存在";
        }
        stories.remove(id);
        LuceneUtil.removeIndex(id);
        return "success";
    }


    @GetMapping("/deleteAll")
    public String deleteAll() {
        stories.clear();
        LuceneUtil.removeAll();
        return "success";
    }
}
