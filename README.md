# 02369 Group 6 Project
Song queue for Spotify

# Install and run project

1. Ensure that [Maven](https://maven.apache.org/) and [Java v21](https://www.oracle.com/java/technologies/downloads/#java21) is installed, as the project requires these to build and run.

2. Login to the [Spotify Developer Dashboard]([https://developer.spotify.com/](https://developer.spotify.com/dashboard)) and create a new application, with the redirect URI=`http://127.0.0.1:8080/callback/spotify` and Web API enabled.
<img width="1053" height="506" alt="image" src="https://github.com/user-attachments/assets/da5a70f5-a107-476d-bee0-2336c801e11a" />
<img width="1045" height="1114" alt="image" src="https://github.com/user-attachments/assets/4e0c8cb5-3ad6-4b6a-a5e6-8a7c94fff1ec" />

3. Rename the file .env.example and replace the example `CLIENT_ID` and `CLIENT_SECRET` fields with values from the Spotify Dashboard.
<img width="1042" height="305" alt="image" src="https://github.com/user-attachments/assets/c20bdf9c-f322-41d5-acfc-f890afd9662a" />

4. Start the web server with the command `mvn spring-boot:run`.

5. Open [http://127.0.0.1:8080/](http://127.0.0.1:8080/) in your browser and use the website.

# Run tests

1. Before running the unit tests make sure the project is setup as described in the previous section.
2. Run the server with the command `mvn spring-boot:run`.
3. Press "Login with Spotify" and login in with your Spotify account.
4. Copy the access token displayed in the spring boot console (the console where you ran the command in step 2) and replace the example token for `TEST_ACCESS_TOKEN` in the `.env` file with this newly created token.
5. Stop the running server and run the command `mvn verify` to run all the tests.
