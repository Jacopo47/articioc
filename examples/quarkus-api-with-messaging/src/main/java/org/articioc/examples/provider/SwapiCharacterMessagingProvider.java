package org.articioc.examples.provider;

import io.smallrye.mutiny.Multi;
import jakarta.enterprise.context.ApplicationScoped;
import org.articioc.base.LeafCarrier;
import org.articioc.base.interfaces.ProviderAsExecutor;
import org.articioc.base.utils.Futures;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Incoming;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import io.vavr.collection.Stream;

import static org.articioc.examples.swapi.pipeline.Steps.*;

@ApplicationScoped
public class SwapiCharacterMessagingProvider implements ProviderAsExecutor<StarWarsCharacterInMovie> {

    private Function<Stream<LeafCarrier<StarWarsCharacterInMovie>>, CompletableFuture<Stream<StarWarsCharacterInMovie>>> pipeline;

    @Channel("swapi-pipeline-dest")
    Emitter<StarWarsCharacterInMovie> emitter;

    @Override
    public CompletableFuture<Stream<LeafCarrier<StarWarsCharacterInMovie>>> read() {
        return CompletableFuture.failedFuture(new Exception("Method not implemented since this provider is an autonomous executor"));
    }

    @Incoming("swapi-pipeline-source")
    void process(StarWarsCharacterInMovie message) {
        Optional.ofNullable(message)
                .map(LeafCarrier::from)
                .map(Stream::of)
                .ifPresent(pipeline::apply);
    }

    @Override
    public CompletableFuture<StarWarsCharacterInMovie> write(StarWarsCharacterInMovie starWarsCharacterInMovie) {
        return emitter.send(starWarsCharacterInMovie)
                .thenApply(ignore -> starWarsCharacterInMovie)
                .toCompletableFuture();
    }

    @Override
    public CompletableFuture<Stream<StarWarsCharacterInMovie>> write(Stream<StarWarsCharacterInMovie> stream) {
        return Futures.whenAllAsStream(stream.map(this::write));
    }

    @Override
    public void close() throws Exception { }


    @Override
    public void setPipeline(Function<Stream<LeafCarrier<StarWarsCharacterInMovie>>, CompletableFuture<Stream<StarWarsCharacterInMovie>>> pipeline) {
        this.pipeline = pipeline;
    }
}
