package org.articioc.examples.swapi.pipeline;

import kong.unirest.GenericType;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.articioc.base.Leaf;
import org.articioc.base.Step;
import org.articioc.examples.swapi.http.*;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import io.vavr.collection.Stream;

public class Steps {
    public static class CharactersInMovieSteps extends Step {
        public static final CharactersInMovieSteps WITH_MOVIE_TITLE = new CharactersInMovieSteps("with-movie-title");
        public static final CharactersInMovieSteps WITH_DETAILS = new CharactersInMovieSteps("with-details");
        public static final CharactersInMovieSteps WITH_ACTORS = new CharactersInMovieSteps("with-actors");


        public CharactersInMovieSteps(String name) {
            super(name);
        }
    }

    public static class StarWarsCharacterInMovie extends Leaf<CharactersInMovieSteps> {
        private final String title;

        private Integer episode;
        private String director;
        private String producer;
        private LocalDate releaseDate;
        private List<String> charactersIds;
        private StarWarsCharacter character;

        public StarWarsCharacterInMovie(String title) {
            super(null, CharactersInMovieSteps.WITH_MOVIE_TITLE);
            this.title = title;
        }


        public String getTitle() {
            return title;
        }

        public Optional<Integer> getEpisode() {
            return Optional.ofNullable(episode);
        }

        public StarWarsCharacterInMovie setEpisode(Integer episode) {
            this.episode = episode;
            return this;
        }

        public Optional<String> getDirector() {
            return Optional.ofNullable(director);
        }

        public StarWarsCharacterInMovie setDirector(String director) {
            this.director = director;
            return this;
        }

        public Optional<String> getProducer() {
            return Optional.ofNullable(producer);
        }

        public StarWarsCharacterInMovie setProducer(String producer) {
            this.producer = producer;
            return this;
        }

        public Optional<LocalDate> getReleaseDate() {
            return Optional.ofNullable(releaseDate);
        }

        public StarWarsCharacterInMovie setReleaseDate(LocalDate releaseDate) {
            this.releaseDate = releaseDate;
            return this;
        }

        public List<String> getCharactersIds() {
            return Optional.ofNullable(charactersIds)
                    .orElseGet(List::of);
        }

        public StarWarsCharacterInMovie setCharactersIds(List<String> charactersIds) {
            this.charactersIds = charactersIds;
            return this;
        }

        public StarWarsCharacter getCharacter() {
            return character;
        }

        public StarWarsCharacterInMovie setCharacter(StarWarsCharacter character) {
            this.character = character;
            return this;
        }

        public StarWarsCharacterInMovie withCharacter(StarWarsCharacter character) {
            return new StarWarsCharacterInMovie(this.title)
                    .setEpisode(this.episode)
                    .setDirector(this.director)
                    .setProducer(this.producer)
                    .setReleaseDate(this.releaseDate)
                    .setCharacter(character);
        }

        @Override
        public String key() {
            return this.title;
        }
    }

    public CompletableFuture<StarWarsCharacterInMovie> fetchFilmMetadata(StarWarsCharacterInMovie movie) {
        return CompletableFuture.supplyAsync(() -> Unirest.get("https://www.swapi.tech/api/films?title=" + movie.getTitle()).asObject(new GenericType<OkList<FilmResponse>>() {}))
                .thenApply(HttpResponse::getBody)
                .thenApply(OkList::getResult)
                .thenApply(e -> e.stream()
                        .findFirst()
                        .map(Properties::getProperties))
                .thenCompose(e -> e
                        .map(CompletableFuture::completedFuture)
                        .orElseGet(() -> CompletableFuture.failedFuture(new Exception("No element found for film: " + movie.getTitle()))))
                .thenApply(response -> movie
                        .setEpisode(response.getEpisodeId())
                        .setDirector(response.getDirector())
                        .setProducer(response.getProducer())
                        .setReleaseDate(response.getReleaseDate())
                        .setCharactersIds(response.getCharactersIds()));
    }

    public StarWarsCharacterInMovie printFilmDetails(StarWarsCharacterInMovie movie) {
        System.out.printf("Title=%s, released=%s, director=%s, characters=%d\n", movie.getTitle(), movie.getReleaseDate(), movie.getDirector(), movie.getCharactersIds().size());

        return movie;
    }

    public Stream<StarWarsCharacterInMovie> printCharacters(Stream<StarWarsCharacterInMovie> input) {
        var copy = input.toList();
        System.out.println("Here the list:" + copy
                .map(StarWarsCharacterInMovie::getCharacter)
                .map(StarWarsCharacter::toString)
                .foldRight("", (acc, next) -> acc + "\n" + next));

        return input;
    }

    public Stream<CompletableFuture<StarWarsCharacterInMovie>> oneToAsManyCharactersInFilm(StarWarsCharacterInMovie movie) {
        Function<String, CompletableFuture<StarWarsCharacter>> fetchCharacter = id -> CompletableFuture
                .supplyAsync(() -> Unirest.get("https://www.swapi.tech/api/people/" + id).asObject(new GenericType<Ok<StarWarsCharacter>>() {}))
                .thenApply(HttpResponse::getBody)
                .thenApply(Ok::getResult)
                .thenApply(Properties::getProperties);

        return Stream.ofAll(movie.getCharactersIds())
                .map(fetchCharacter)
                .map(f -> f.thenApply(movie::withCharacter));
    }

    public Stream<StarWarsCharacterInMovie> keepOnlyCharactersWithYellowEyeColor(StarWarsCharacterInMovie input) {
        if (input.getCharacter().getEye_color().equals("yellow")) return Stream.of(input);

        return Stream.of();
    }

    public Stream<StarWarsCharacterInMovie> orderByName(Stream<StarWarsCharacterInMovie> input) {
        return input
                .sorted(Comparator.comparing(a -> a.getCharacter().getName()));
    }


}
