package com.group6.SongQueue.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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

import java.util.List;

import org.springframework.web.servlet.ModelAndView;

import com.group6.SongQueue.model.Song;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = ClassMode.BEFORE_EACH_TEST_METHOD)
public class SearchTests {

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
	void searchForSongs() throws Exception {
   		MvcResult result = mockMvc.perform(get("/search").param("query", "Test search").session(session))
				.andExpect(status().isOk())
				.andReturn();

		ModelAndView modelView = result.getModelAndView();
		assertNotNull(modelView);
		var model = modelView.getModel();

		assertTrue(model.containsKey("songs"));
		List<Song> songs = (List<Song>)model.get("songs");
		assertEquals(50, songs.size());
	}

	@Test
	void addSongToQueue() throws Exception {
		mockMvc.perform(post("/dashboard/create-songqueue").session(session))
     		.andExpect(status().isFound());

		List<Song> songs;
		{
	   		MvcResult result = mockMvc.perform(get("/search").param("query", "Test").session(session))
					.andExpect(status().isOk())
					.andReturn();

			ModelAndView modelView = result.getModelAndView();
			assertNotNull(modelView);
			var model = modelView.getModel();

			assertTrue(model.containsKey("songs"));
			songs = (List<Song>)model.get("songs");
			assertEquals(50, songs.size());
		}

		Song song = songs.get(5);

		mockMvc.perform(post("/search/add").param("query", "Test").param("id", song.getId()).session(session))
			.andExpect(status().isFound());

		// Check that song was added
		{
			MvcResult result = mockMvc.perform(get("/live").session(session))
	            .andExpect(status().isOk())
	            .andReturn();

	        ModelAndView modelView = result.getModelAndView();
	        assertNotNull(modelView);
	        var model = modelView.getModel();

	        assertNotNull(model.get("songqueue"));
			List<Song> queue = (List<Song>)model.get("songqueue");
	        assertEquals(1, queue.size());
			assertEquals(song.getId(), queue.get(0).getId());
			assertEquals(song.getTitle(), queue.get(0).getTitle());
			assertEquals(song.getArtist(), queue.get(0).getArtist());
			assertEquals(song.getAlbumArtUrl(), queue.get(0).getAlbumArtUrl());
		}
	}

}
