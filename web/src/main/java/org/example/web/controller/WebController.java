package org.example.web.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/web")
public class WebController {

    @GetMapping
    public ResponseEntity<String> response(@RequestHeader String requestTime) {
        return ResponseEntity.ok("requestTime : " + requestTime + ", You passed filter\n");
    }
}
