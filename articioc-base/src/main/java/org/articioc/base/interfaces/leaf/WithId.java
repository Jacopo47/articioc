package org.articioc.base.interfaces.leaf;

public interface WithId<E> {
  E getId();

  WithId<E> setId(E id);
}
