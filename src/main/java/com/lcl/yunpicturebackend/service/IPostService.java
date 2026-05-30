package com.lcl.yunpicturebackend.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.lcl.yunpicturebackend.domain.dto.post.PostAddRequest;
import com.lcl.yunpicturebackend.domain.dto.post.PostEditRequest;
import com.lcl.yunpicturebackend.domain.dto.post.PostQueryRequest;
import com.lcl.yunpicturebackend.domain.po.Post;
import com.lcl.yunpicturebackend.domain.vo.PostVO;

import javax.servlet.http.HttpServletRequest;

public interface IPostService extends IService<Post> {

    long publishPost(PostAddRequest request, Long userId);

    PostVO getPostVO(Post post, HttpServletRequest request);

    Page<PostVO> getPostVOPage(PostQueryRequest request, HttpServletRequest httpRequest);

    PostVO getPostVOById(Long id, HttpServletRequest request);

    void editPost(PostEditRequest request, Long userId);

    void deletePost(Long id, Long userId);
}
