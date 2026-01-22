package org.articioc.examples.api;

import io.vavr.Value;
import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.articioc.Articioc;
import org.articioc.examples.swapi.pipeline.Steps.CharactersInMovieSteps;

import javax.annotation.processing.Completions;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import io.vavr.collection.Stream;

import static org.articioc.examples.swapi.pipeline.Steps.*;

@Path("/pipelines")
public class PipelinesApi {

    @Inject
    Articioc<StarWarsCharacterInMovie, CharactersInMovieSteps> swapiMovieCharactersPipeline;

    @POST
    @Path("/fire-and-forget")
    @Produces(MediaType.APPLICATION_JSON)
    public CompletableFuture<List<StarWarsCharacterInMovie>> fireAndForget() {
        return swapiMovieCharactersPipeline.trigger()
                .thenApply(Value::toJavaList);
    }

    @POST
    @Path("/sync")
    @Produces(MediaType.APPLICATION_JSON)
    public CompletableFuture<List<StarWarsCharacterInMovie>> sync(SyncRequest body) {
        var pipeline = swapiMovieCharactersPipeline.pipeline();

        var movies = Optional.ofNullable(body)
                .map(SyncRequest::movies)
                .stream()
                .flatMap(Collection::stream)
                .map(SyncRequestItem::title)
                .map(StarWarsCharacterInMovie::new);

        return pipeline.apply(CompletableFuture.completedFuture(Stream.ofAll(movies)))
                .thenApply(Value::toJavaList);
    }

    public record SyncRequest(List<SyncRequestItem> movies) {}
    public record SyncRequestItem(String title) {}
}
