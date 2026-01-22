package org.articioc.examples.swapi.http;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class FilmResponse {

    private String title;
    private Integer episode_id;
    private String opening_crawl;
    private String director;
    private String producer;
    private LocalDate release_date; 
    private List<String> characters;
    private LocalDate created;
    private LocalDate edited;

    public String getTitle() {
        return title;
    }

    public FilmResponse setTitle(String title) {
        this.title = title;
        return this;
    }

    public Integer getEpisodeId() {
        return episode_id;
    }

    public FilmResponse setEpisode_id(Integer episode_id) {
        this.episode_id = episode_id;
        return this;
    }

    public String getOpening_crawl() {
        return opening_crawl;
    }

    public FilmResponse setOpening_crawl(String opening_crawl) {
        this.opening_crawl = opening_crawl;
        return this;
    }

    public String getDirector() {
        return director;
    }

    public FilmResponse setDirector(String director) {
        this.director = director;
        return this;
    }

    public String getProducer() {
        return producer;
    }

    public FilmResponse setProducer(String producer) {
        this.producer = producer;
        return this;
    }

    public LocalDate getReleaseDate() {
        return release_date;
    }

    public FilmResponse setRelease_date(LocalDate release_date) {
        this.release_date = release_date;
        return this;
    }

    public LocalDate getCreated() {
        return created;
    }

    public FilmResponse setCreated(LocalDate created) {
        this.created = created;
        return this;
    }

    public LocalDate getEdited() {
        return edited;
    }

    public FilmResponse setEdited(LocalDate edited) {
        this.edited = edited;
        return this;
    }

    public List<String> getCharactersIds() {
        return Optional.ofNullable(characters)
                .orElseGet(List::of)
                .stream()
                .map(s -> s.split("/"))
                .map(s -> s[s.length - 1])
                .toList();
    }

    public FilmResponse setCharacters(List<String> characters) {
        this.characters = characters;
        return this;
    }
}
