package swapi;

import static swapi.pipeline.Steps.*;

import io.vavr.collection.Stream;

import org.articioc.Articioc;
import org.articioc.base.interfaces.Provider;
import org.articioc.base.providers.InMemoryProvider;
import swapi.http.StarWarsCharacter;
import swapi.pipeline.Steps;

import java.util.concurrent.CompletableFuture;

public class Main {

  public static void main(String[] args) {

    System.out.println("Starting..");

    var input =
        CompletableFuture.completedFuture(Stream.of(new StarWarsCharacterInMovie("A New Hope")));

    Provider<StarWarsCharacterInMovie> provider = new InMemoryProvider<>();
    var articioc = new Articioc.Builder<>(provider, MovieStep.WITH_MOVIE_TITLE);

    var steps = new Steps();

    var pipeline = articioc
        /*
         * This is a one-to-one operation.
         * That means that given a single record in input it will produce a single record in output.
         * It could be the exact same object without changes, modified or a completely fresh new object. */
        .addStep(steps::fetchFilmMetadata)
        /*
         * .checkpoint(..) generates a side-effect forcing the library to write records into the provider.
         * This means that, dependently to the provider implementation, records will be stored (in memory, serialized into a datastore, etc.)
         *   and since that point will be treated independently.
         * Independently means that to the .checkpoint(..) will reach by a stream of records (0, 1 o hypothetically infinite),
         *   the stream of records will be written to the provider and then dependently to the modality in which data is read from the provider
         *   records will be retrieved and processed.
         * It's VERY IMPORTANT understand that inside a local pipeline the records enters independently but then,
         *   if they explode and an operation one-to-many generates from a single record 2 or more records they will be treated together
         *   into the local pipeline, until they'll be written into the provider, at the point. the next operation will read them one by one
         *   executing a local pipeline on each record read from the provider. */
        .checkpoint(MovieStep.WITH_DETAILS)
        /* ono-to-one operation, no transformation to the input data, it's just uses it in order to print details about it. */
        .addStep(steps::printFilmDetails)
        .checkpoint(MovieStep.WITH_ACTORS)
        /* This is a one-to-many operation.
         * Starting from a single record it explodes it into a stream of records for each character into the input. */
        .addStep(steps::oneToAsManyCharactersInFilm)
        /*
         * This is a one-to-many operation… but "many" in this case could even mean 0.
         * Each step could produce 0, 1 or N records.
         * In this way each step could work as:
         * - map: 1-1 mapping of a value into a new one;
         * - explode: 1-N a single record could be mapped into a list of records;
         * - filter: 1-0 if a records needs to be stopped or filtered out it's just the matter or return an empty collection. */
        .addStep(steps::keepOnlyCharactersWithYellowEyeColor)
        /*
         * This is a many-to-many operation.
         * This kind of step will receive a collection of records so the logic can manipulate them together.
         * This is helpful for all the operations that needs to know the whole context in order to operate, like:
         *  sorting, distinct, filtering, grouping, reduce, etc.
         *
         * A point of attention MUST be explained.
         * If this step is called as first one after a checkpoint it will receive a collection of a single record.
         * When reading from a provider then records are treat independently.
         *   That means that each one will be the applied to the local pipeline
         *   as a collection of a single element.
         * Then the pipeline in it various steps could explode this single input into multiple records,
         *   and in this case the collection that this kind of steps will receive
         *   will contain more the one element.
         * */
        .addStep(steps::orderByName)
        /* Creates the object that will provide methods for interact with the pipeline. */
        .end()
        /* Returns the pipeline as a function that takes and input, applies all the steps in memory,
         *   without writing to the provider, and the returns the output.
         * This is useful for unit tests, or testing scenario.
         * Or even production cases in which scalability is not a point and the whole pipeline could be executed inside a single process. */
        .pipeline();

    var result = pipeline.apply(input).join().toList();

    /*
     * Expecting output:
     *
     * Total number: 2
     * Here the list:StarWarsCharacter[name='C-3PO', birth year='112BBY', eye color='yellow', gender='n/a', hair color='n/a', height='167', mass='75']
     * StarWarsCharacter[name='Darth Vader', birth year='41.9BBY', eye color='yellow', gender='male', hair color='none', height='202', mass='136']
     */
    System.out.println("Total number: " + result.size());
    System.out.println("Here the list:"
        + result
            .map(StarWarsCharacterInMovie::getCharacter)
            .map(StarWarsCharacter::toString)
            .reduce((acc, next) -> acc + "\n" + next));
  }
}
