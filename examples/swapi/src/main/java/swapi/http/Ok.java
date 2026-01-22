package swapi.http;

public class Ok<E> {
  private String message;
  private Properties<E> result;

  public Ok() {}

  public String getMessage() {
    return message;
  }

  public Ok<E> setMessage(String message) {
    this.message = message;
    return this;
  }

  public Properties<E> getResult() {
    return result;
  }

  public Ok<E> setResult(Properties<E> result) {
    this.result = result;
    return this;
  }
}
