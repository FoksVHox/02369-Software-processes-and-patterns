# 02369 Group 6 Project
Song queue for Spotify

# Run Project
To install dependencies and run SpringBoot server:
```sh
mvn spring-boot:run
```

To run tests:
```sh
mvn verify
```

> ![note] Spotify authentication
> Some tests requires access to spotify, so make sure there is a TEST_ACCESS_TOKEN entry in the .env file with an active spotify access token (printed in console when logging in while debugging)