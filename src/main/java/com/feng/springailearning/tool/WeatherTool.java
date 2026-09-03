package com.feng.springailearning.tool;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.time.LocalDateTime;

public class WeatherTool {

    @Tool(description = "获取指定城市的当前天气信息")
    public String getCurrentWeather(
            @ToolParam(description = "城市名称，例如：北京、上海、广州") String city) {
        System.out.println("【Tool Called】查询天气，城市: " + city);

        return switch (city == null ? "" : city.trim()) {
            case "北京" -> "北京 当前天气：晴朗，温度 18°C，湿度 45%";
            case "上海" -> "上海 当前天气：多云，温度 20°C，湿度 60%";
            case "广州" -> "广州 当前天气：小雨，温度 24°C，湿度 80%";
            default -> city + " 当前天气：晴，温度 22°C（模拟数据）";
        };
    }

    @Tool(description = "获取当前系统时间")
    public String getCurrentTime() {
        return "当前系统时间：" + LocalDateTime.now();
    }
}
