package ai.docling.serve.grpc.v1.mapping;

import java.util.function.Consumer;

/**
 * Shared utilities for proto ↔ Java mapping.
 */
final class ProtoMapping {

  private ProtoMapping() {
  }

  /**
   * Applies the setter only if the value is non-null.
   * Eliminates repetitive {@code if (val != null) builder.setFoo(val)} patterns
   * across mapper classes.
   *
   * @param value  the possibly-null value
   * @param setter the proto builder setter (e.g. {@code builder::setFoo})
   * @param <T>    the value type
   */
  static <T> void ifNonNull(T value, Consumer<T> setter) {
    if (value != null) {
      setter.accept(value);
    }
  }
}