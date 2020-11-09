package com.atguigu.gmall.search.controller;

import com.atguigu.gmall.search.pojo.SearchParamVo;
import com.atguigu.gmall.search.pojo.SearchResponseVo;
import com.atguigu.gmall.search.service.SearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("search")
public class SearchController {

    @Autowired
    private SearchService searchService;

    @GetMapping
    public String search(SearchParamVo searchParamVo, Model model){
        SearchResponseVo responseVo = searchService.search(searchParamVo);
        model.addAttribute("response", responseVo);
        model.addAttribute("searchParam", searchParamVo);
        System.out.println(responseVo);
        return "search";
    }

}
