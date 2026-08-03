package com.example.controller;

import com.example.entity.Message;
import com.example.repository.MessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/hello")
public class HelloController {

    @Autowired
    private MessageRepository messageRepository;

    @GetMapping
    public Message getHelloWorld() {
        return messageRepository.findById(1L)
                .orElse(new Message(1L, "Code not DB - Hello World from Spring Boot + PostgreSQL!"));
    }
}
