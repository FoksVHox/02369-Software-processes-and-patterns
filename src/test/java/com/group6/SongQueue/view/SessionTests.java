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
public class SessionTests {

	@Value("${TEST_ACCESS_TOKEN:internalToken}")
    private String accessToken;

	private MockHttpSession hostSession;
    private MockHttpSession guestSession;

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void setup() {
        hostSession = new MockHttpSession();
        hostSession.setAttribute("spotify_access_token", accessToken);
        guestSession = new MockHttpSession();
    }

    @Test
    void testCreateSession() throws Exception {
   		mockMvc.perform(post("/dashboard/create-songqueue").session(hostSession))
     		.andExpect(status().isFound());

       	// Test that host created session
        {
	        MvcResult result = mockMvc.perform(get("/").session(hostSession))
	                .andExpect(status().isOk())
	                .andReturn();

	        ModelAndView modelView = result.getModelAndView();
	        assertNotNull(modelView);
	        var model = modelView.getModel();

	        assertTrue(model.containsKey("name"));
	        assertNotNull(model.get("songqueue_size"));
	        assertEquals(0, ((Number) model.get("songqueue_size")).intValue());
			assertTrue((Boolean)model.getOrDefault("isInQueue", false));
			assertTrue((Boolean)model.getOrDefault("isHost", false));
	    }
		// Test that guest is not in session
        {
	        MvcResult result = mockMvc.perform(get("/").session(guestSession))
	                .andExpect(status().isOk())
	                .andReturn();

	        ModelAndView modelView = result.getModelAndView();
	        assertNotNull(modelView);
	        var model = modelView.getModel();

	        assertFalse(model.containsKey("name"));
	        assertNull(model.get("songqueue_size"));
			assertFalse((Boolean)model.getOrDefault("isInQueue", false));
        }
    }

    @Test
    void testJoinSession() throws Exception {
  		testCreateSession();

    	// Get join code
     	String joinCode;
    	{
	        MvcResult result = mockMvc.perform(get("/").session(hostSession))
	                .andExpect(status().isOk())
	                .andReturn();

	        ModelAndView modelView = result.getModelAndView();
	        assertNotNull(modelView);
	        var model = modelView.getModel();

	        assertTrue(model.containsKey("name"));
	        assertNotNull(model.get("songqueue_size"));
	        assertEquals(0, ((Number) model.get("songqueue_size")).intValue());
			assertTrue((Boolean)model.getOrDefault("isInQueue", false));
			assertTrue((Boolean)model.getOrDefault("isHost", false));
			joinCode = (String)model.get("activeJoinCode");
	    }

   		mockMvc.perform(post("/join").param("join-code", joinCode).session(guestSession))
    		.andExpect(status().isFound());

		// Test that guest is now in session
        {
	        MvcResult result = mockMvc.perform(get("/").session(guestSession))
	                .andExpect(status().isOk())
	                .andReturn();

	        ModelAndView modelView = result.getModelAndView();
	        assertNotNull(modelView);
	        var model = modelView.getModel();

			assertTrue((Boolean)model.getOrDefault("isInQueue", false));
			assertFalse((Boolean)model.getOrDefault("isHost", true));
        }
    }

    @Test
    void testLeaveSession() throws Exception {
  		testJoinSession();

   		mockMvc.perform(post("/leave").session(guestSession))
    		.andExpect(status().isFound());

		// Test that guest is no longer in session
        {
	        MvcResult result = mockMvc.perform(get("/").session(guestSession))
	                .andExpect(status().isOk())
	                .andReturn();

	        ModelAndView modelView = result.getModelAndView();
	        assertNotNull(modelView);
	        var model = modelView.getModel();

			assertFalse((Boolean)model.getOrDefault("isInQueue", false));
        }
    }


}
