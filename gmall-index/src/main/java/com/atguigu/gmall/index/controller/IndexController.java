package com.atguigu.gmall.index.controller;

import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.index.service.IndexService;
import com.atguigu.gmall.pms.entity.CategoryEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

@Controller
public class IndexController {

    @Autowired
    private IndexService indexService;

    @GetMapping("index/cates/{pId}")
    @ResponseBody
    public ResponseVo<List<CategoryEntity>> quueryLv2CategoryWithSubsByPid(@PathVariable("pId") Long pId){
        List<CategoryEntity> categoryEntities = indexService.quueryLv2CategoryWithSubsByPid(pId);
        return ResponseVo.ok(categoryEntities);
    }

    @GetMapping({"/","/index"})
    public String toIndex(Model model){
        //查询一级分类
        List<CategoryEntity> cates = indexService.queryLv1Categories();
        model.addAttribute("categories", cates);
        return "index";
    }
}
