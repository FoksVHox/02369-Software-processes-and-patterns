package com.group6.SongQueue.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.web.servlet.ModelAndView;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = ClassMode.BEFORE_EACH_TEST_METHOD)
public class ExampleViewTest {

    @Value("${TEST_ACCESS_TOKEN:internalToken}")
    private String accessToken;

    private MockHttpSession session;

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void setup() {
        session = new MockHttpSession();
        session.setAttribute("spotify_access_token", accessToken);
    }

    @Test
    void testHome() throws Exception {
        MvcResult result = mockMvc.perform(
                get("/").session(session)
            )
            .andExpect(status().isOk())
            .andReturn();

        ModelAndView modelView = result.getModelAndView();
        assertNotNull(modelView);

        assertEquals(4, modelView.getModel().size());
    }

}
