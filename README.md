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

> Nogle tests kræver adgang til Spotify, så sørg for, at der er en TEST_ACCESS_TOKEN i .env filen med et aktivt Spotify adgangstoken (Bliver dumpet i consollen ved login)