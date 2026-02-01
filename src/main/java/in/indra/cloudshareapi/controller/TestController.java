package in.indra.cloudshareapi.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;
import java.util.Map;

@RequestMapping("/test")
@RestController
public class TestController {

    @GetMapping("/home")
    public String home() {
        return "API is running! Time: " + new Date();
    }

    @GetMapping("/check")
    public String check() {
        return "Check endpoint works!";
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of(
                "status", "UP",
                "service", "cloudshare-api",
                "timestamp", new Date().toString()
        );
    }
}