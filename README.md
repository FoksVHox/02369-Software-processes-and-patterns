# 02369 Group 6 Project
Song queue for Spotify

# Run Project

The project requires that [Maven](https://maven.apache.org/) is installed on the computer, but then to install dependencies and run the SpringBoot web server simply run this command:
```sh
mvn spring-boot:run
```

Once the server is running the website can be accessed at [http://localhost:8080](http://localhost:8080)

> [!warning]
> For the integration with Spotify to work the project requires a .env file that is filled out with the [Spotify API](https://developer.spotify.com/) client id and secret. To get these you must log into the Spotify API dashboard, create a new app and add `http://127.0.0.1:8080/callback/spotify` as a redirect url.
> Rename the .env.example file to .env and fill out the information that was copied.

# Run tests
To run the unit tests run this command, but make sure to stop the running project above before:
```sh
mvn verify
```

> [!note]
> Some tests requires access to spotify, so make sure there is a TEST_ACCESS_TOKEN entry in the .env file with an active spotify access token (This is printed in the console when logging in while debugging)