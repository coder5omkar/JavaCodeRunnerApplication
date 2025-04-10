package com.example.javacoderunner;

import org.springframework.ai.chat.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class JavaClassGenerator {

    private final ChatClient chatClient;

    public JavaClassGenerator(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    public String generateFullClass(String partialCode) {
        String prompt = """
        Convert the following Java code snippet into a full Java class.
        Add necessary imports and a main method if needed.dont include any other character than
        java code 

        Code :
        %s
        """.formatted(partialCode);

        return chatClient.call(prompt);
    }
}
