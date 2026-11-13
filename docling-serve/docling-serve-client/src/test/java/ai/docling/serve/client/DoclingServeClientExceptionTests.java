package ai.docling.serve.client;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class DoclingServeClientExceptionTests {
  @Test
  void constructWithCause() {
    var cause = new RuntimeException("original");
    var exception = new DoclingServeClientException(cause);

    assertThat(exception.getMessage()).isEqualTo("original");
    assertThat(exception.getCause()).isSameAs(cause);
    assertThat(exception.getStatusCode()).isEqualTo(-1);
    assertThat(exception.getResponseBody()).isNull();
  }

  @Test
  void constructWithNullCause() {
    var exception = new DoclingServeClientException((Throwable) null);

    assertThat(exception.getMessage()).isEqualTo("An error occurred");
    assertThat(exception.getCause()).isNull();
    assertThat(exception.getStatusCode()).isEqualTo(-1);
    assertThat(exception.getResponseBody()).isNull();
  }

  @Test
  void constructWithCauseAndMessage() {
    var cause = new RuntimeException("root");
    var exception = new DoclingServeClientException(cause, "custom message");

    assertThat(exception.getMessage()).isEqualTo("custom message");
    assertThat(exception.getCause()).isSameAs(cause);
    assertThat(exception.getStatusCode()).isEqualTo(-1);
    assertThat(exception.getResponseBody()).isNull();
  }

  @Test
  void constructWithStatusCodeAndResponseBody() {
    var exception = new DoclingServeClientException("request failed", 503, "Service Unavailable");

    assertThat(exception.getMessage()).isEqualTo("request failed");
    assertThat(exception.getStatusCode()).isEqualTo(503);
    assertThat(exception.getResponseBody()).isEqualTo("Service Unavailable");
    assertThat(exception.getCause()).isNull();
  }

  @Test
  void constructWithStatusCodeAndNullResponseBody() {
    var exception = new DoclingServeClientException("not found", 404, null);

    assertThat(exception.getMessage()).isEqualTo("not found");
    assertThat(exception.getStatusCode()).isEqualTo(404);
    assertThat(exception.getResponseBody()).isNull();
  }
}
