package com.lcl.yunpicturebackend.domain.po;

import lombok.Data;

import java.util.Arrays;
import java.util.List;

@Data
public class PictureTagCategory {
    private List<String> tagList;
    private List<String> categoryList;
}
