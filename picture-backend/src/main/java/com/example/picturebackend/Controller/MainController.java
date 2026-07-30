package com.example.picturebackend.Controller;

import com.example.picturebackend.Utils.ResponseUtils;
import com.example.picturebackend.domain.request.BaseResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/health")
public class MainController {
    @GetMapping("/isSuccess")
    public BaseResponse<String> health(){
        return ResponseUtils.success("成功启动");
    }
}
