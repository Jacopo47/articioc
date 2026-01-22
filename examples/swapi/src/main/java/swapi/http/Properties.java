package swapi.http;

public class Properties<E> {
  private E properties;

  public E getProperties() {
    return properties;
  }

  public Properties<E> setProperties(E properties) {
    this.properties = properties;
    return this;
  }
}
