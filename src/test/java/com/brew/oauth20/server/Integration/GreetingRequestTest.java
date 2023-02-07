package com.brew.oauth20.server.Integration;


import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class GreetingRequestTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void performGreetingRequestWhenNameThenOk() throws Exception {
        this.mockMvc.perform(get("/greeting")
                        .param("name", "test")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isString())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.content").value("Hello, test!"));
    }
}
