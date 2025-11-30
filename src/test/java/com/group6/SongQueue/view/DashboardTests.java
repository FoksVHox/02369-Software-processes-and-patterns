package com.group6.SongQueue.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.web.servlet.ModelAndView;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = ClassMode.BEFORE_EACH_TEST_METHOD)
public class DashboardTests {

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
    void testNoSongqueue() throws Exception {
        MvcResult result = mockMvc.perform(get("/").session(session))
                .andExpect(status().isOk())
                .andReturn();

        ModelAndView modelView = result.getModelAndView();
        assertNotNull(modelView);
        var model = modelView.getModel();

        assertFalse(model.containsKey("name"));
        assertNull(model.get("songqueue_size"));
    }

    @Test
    void testCreateSongqueueFromPlaylist() throws Exception {
   		mockMvc.perform(post("/dashboard/add-playlist")
        		.session(session)
        		.param("playlist-url", "https://open.spotify.com/playlist/2YZSzdHBhmmMDn6J43U3b5?si=355a4a75fac844f8")
     		).andExpect(status().isFound());

        MvcResult result = mockMvc.perform(get("/").session(session))
                .andExpect(status().isOk())
                .andReturn();

        ModelAndView modelView = result.getModelAndView();
        assertNotNull(modelView);
        var model = modelView.getModel();

        assertTrue(model.containsKey("name"));
        assertNotNull(model.get("songqueue_size"));
        assertEquals(58, ((Number) model.get("songqueue_size")).intValue());
    }

    @Test
    void testCreateEmptySongqueue() throws Exception {
   		mockMvc.perform(post("/dashboard/create-songqueue").session(session))
     		.andExpect(status().isFound());

        MvcResult result = mockMvc.perform(get("/").session(session))
                .andExpect(status().isOk())
                .andReturn();

        ModelAndView modelView = result.getModelAndView();
        assertNotNull(modelView);
        var model = modelView.getModel();

        assertTrue(model.containsKey("name"));
        assertNotNull(model.get("songqueue_size"));
        assertEquals(0, ((Number) model.get("songqueue_size")).intValue());
    }

    @Test
    void testAddingSongFromLink() throws Exception {
    	testCreateEmptySongqueue();

  		mockMvc.perform(post("/add-song/by-url")
       		.session(session)
       		.param("song-url", "https://open.spotify.com/track/1MUExGawtk7kNqKaMO28wD?si=c63ede26738b4b3d")
    		).andExpect(status().isFound());

        MvcResult result = mockMvc.perform(get("/").session(session))
                .andExpect(status().isOk())
                .andReturn();

        ModelAndView modelView = result.getModelAndView();
        assertNotNull(modelView);
        var model = modelView.getModel();

        assertTrue(model.containsKey("name"));
        assertNotNull(model.get("songqueue_size"));
        assertEquals(1, ((Number) model.get("songqueue_size")).intValue());
    }

    @Test
    void testEndingSession() throws Exception {
    	testCreateEmptySongqueue();

  		mockMvc.perform(post("/dashboard/close-session").session(session)).andExpect(status().isFound());

        MvcResult result = mockMvc.perform(get("/").session(session))
                .andExpect(status().isOk())
                .andReturn();

        ModelAndView modelView = result.getModelAndView();
        assertNotNull(modelView);
        var model = modelView.getModel();

        assertFalse(model.containsKey("name"));
        assertNull(model.get("songqueue_size"));
    }
}
