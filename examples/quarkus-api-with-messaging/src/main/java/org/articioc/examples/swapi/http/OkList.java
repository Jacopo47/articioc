package org.articioc.examples.swapi.http;

import java.util.List;

public class OkList<E> {
    private String message;
    private List<Properties<E>> result;

    public OkList() {}

    public String getMessage() {
        return message;
    }

    public OkList<E> setMessage(String message) {
        this.message = message;
        return this;
    }

    public List<Properties<E>> getResult() {
        return result;
    }

    public OkList<E> setResult(List<Properties<E>> result) {
        this.result = result;
        return this;
    }
}
