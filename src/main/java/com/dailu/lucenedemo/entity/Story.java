package com.dailu.lucenedemo.entity;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class Story {
    private String id;
    private String title;
    private String content;
    private String author;
    private int price;
}
