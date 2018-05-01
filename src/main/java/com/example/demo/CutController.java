package com.example.demo;

import com.huaban.analysis.jieba.JiebaSegmenter;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
public class CutController {

    private JiebaSegmenter segmenter = new JiebaSegmenter();
    @GetMapping("/cut")
    public String cut(@RequestParam(name="text") String text, Model model) {
        String cutOff = segmenter.process(text, JiebaSegmenter.SegMode.INDEX).toString();
        model.addAttribute("text", cutOff);
        return "cut";
    }

    @GetMapping("/keyword")
    public String keyword(@RequestParam(name="title", required = false, defaultValue = "") String title,
                          @RequestParam(name="content") String content,
                          Model model) {
        List<String> res = TextRank.getKeyword(title, content);
        model.addAttribute("text", res.stream().reduce("", (x, y) -> x + ", " + y));
        return "cut";
    }
}
