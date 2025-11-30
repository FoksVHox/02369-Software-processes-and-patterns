package com.group6.SongQueue.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
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

import java.util.List;

import org.springframework.web.servlet.ModelAndView;

import com.group6.SongQueue.controller.SongQueueController;
import com.group6.SongQueue.model.Song;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = ClassMode.BEFORE_EACH_TEST_METHOD)
public class VoteTests {

    private MockHttpSession session;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SongQueueController songQueueController;

    @BeforeEach
    void setup() {
        session = new MockHttpSession();
        session.setAttribute("spotify_access_token", "internal-token");
    }

    @Test
    void testEmptySongqueue() throws Exception {
   		mockMvc.perform(post("/dashboard/create-songqueue").session(session))
     		.andExpect(status().isFound());

        MvcResult result = mockMvc.perform(get("/vote").session(session))
                .andExpect(status().isOk())
                .andReturn();

        ModelAndView modelView = result.getModelAndView();
        assertNotNull(modelView);
        var model = modelView.getModel();

        assertTrue((Boolean)model.getOrDefault("isHost", false));
        assertTrue(model.containsKey("songqueue"));
        List<Song> queue = ((List<Song>)model.get("songqueue"));
        assertEquals(0, queue.size());
    }

    @Test
    void testFilledSongqueue() throws Exception {
		mockMvc.perform(post("/dashboard/create-songqueue").session(session))
			.andExpect(status().isFound());

		songQueueController.addSong(session, new Song("id1", "Song 1", "Artist", null));
		songQueueController.addSong(session, new Song("id2", "Song 2", "Artist", null));
		songQueueController.addSong(session, new Song("id3", "Song 3", "Artist", null));
		songQueueController.addSong(session, new Song("id4", "Song 4", "Artist 2", null));
		songQueueController.addSong(session, new Song("id5", "Song 5", "Artist 2", null));
		songQueueController.addSong(session, new Song("id6", "Song 6", "Artist 2", null));
		songQueueController.addSong(session, new Song("id7", "Song 7", "Artist 2", null));

        MvcResult result = mockMvc.perform(get("/vote").session(session))
                .andExpect(status().isOk())
                .andReturn();

        ModelAndView modelView = result.getModelAndView();
        assertNotNull(modelView);
        var model = modelView.getModel();

        assertTrue(model.containsKey("songqueue"));
        List<Song> queue = ((List<Song>)model.get("songqueue"));
        assertEquals(7, queue.size());
    }

	@Test
	void testVotedSongqueue() throws Exception {
		testFilledSongqueue();

		mockMvc.perform(post("/vote").session(session).param("songId", "id5").param("vote", "up"))
            .andExpect(status().isFound());

  		mockMvc.perform(post("/vote").session(session).param("songId", "id2").param("vote", "up"))
                .andExpect(status().isFound());
        mockMvc.perform(post("/vote").session(session).param("songId", "id2").param("vote", "up"))
                    .andExpect(status().isFound());

        mockMvc.perform(post("/vote").session(session).param("songId", "id3").param("vote", "down"))
            .andExpect(status().isFound());

		MvcResult result = mockMvc.perform(get("/vote").session(session))
                .andExpect(status().isOk())
                .andReturn();

        ModelAndView modelView = result.getModelAndView();
        assertNotNull(modelView);
        var model = modelView.getModel();

        assertTrue(model.containsKey("songqueue"));
        List<Song> queue = ((List<Song>)model.get("songqueue"));
        assertEquals(7, queue.size());
        assertEquals(queue.get(0).getId(), "id5");
        assertEquals(queue.get(0).getVotes(), 1);
        assertEquals(queue.get(6).getId(), "id3");
        assertEquals(queue.get(6).getVotes(), -1);
    }

    @Test
	void testVetoedSongqueue() throws Exception {
		testFilledSongqueue();

		mockMvc.perform(post("/vote").session(session).param("songId", "id5").param("vote", "up"))
               .andExpect(status().isFound());

        mockMvc.perform(post("/vote").session(session).param("songId", "id3").param("vote", "down"))
            .andExpect(status().isFound());
        mockMvc.perform(post("/vote/veto").session(session).param("songId", "id3"))
            .andExpect(status().isFound());

		MvcResult result = mockMvc.perform(get("/vote").session(session))
                   .andExpect(status().isOk())
                   .andReturn();

           ModelAndView modelView = result.getModelAndView();
           assertNotNull(modelView);
           var model = modelView.getModel();

           assertTrue(model.containsKey("songqueue"));
           List<Song> queue = ((List<Song>)model.get("songqueue"));
           assertEquals(7, queue.size());
           assertEquals(queue.get(0).getId(), "id5");
           assertEquals(queue.get(0).getVotes(), 1);
           assertEquals(queue.get(6).getId(), "id3");
           assertEquals(queue.get(6).getVetoCount(), 1);
       }
}
