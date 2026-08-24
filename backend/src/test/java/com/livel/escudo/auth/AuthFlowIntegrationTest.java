package com.livel.escudo.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AuthFlowIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;

    @Test void registerScanAndReadHistory() throws Exception {
        String email="persona"+System.nanoTime()+"@example.com";
        String json=mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(new AuthController.RegisterRequest(email,"Clave-segura-123"))))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        JsonNode tokens=mapper.readTree(json); String access=tokens.path("accessToken").asText();
        mvc.perform(post("/api/scans/text").header("Authorization","Bearer "+access).contentType(MediaType.APPLICATION_JSON)
                .content("{\"content\":\"Urgente, compartí tu contraseña para cobrar el premio\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.score").isNumber()).andExpect(jsonPath("$.guest").value(false));
        mvc.perform(get("/api/scans").header("Authorization","Bearer "+access))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content[0].type").value("TEXT"));
    }
}
