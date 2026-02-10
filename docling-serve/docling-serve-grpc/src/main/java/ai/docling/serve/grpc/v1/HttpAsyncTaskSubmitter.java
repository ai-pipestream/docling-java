package ai.docling.serve.grpc.v1;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import ai.docling.serve.api.chunk.request.HierarchicalChunkDocumentRequest;
import ai.docling.serve.api.chunk.request.HybridChunkDocumentRequest;
import ai.docling.serve.api.convert.request.ConvertDocumentRequest;
import ai.docling.serve.api.task.response.TaskStatusPollResponse;

final class HttpAsyncTaskSubmitter implements AsyncTaskSubmitter {
  private static final String API_KEY_HEADER_NAME = "X-Api-Key";

  private final URI baseUrl;
  private final String apiKey;
  private final HttpClient httpClient;
  private final JsonMapper jsonMapper;

  HttpAsyncTaskSubmitter(URI baseUrl, String apiKey) {
    Objects.requireNonNull(baseUrl, "baseUrl");
    this.baseUrl = baseUrl.toString().endsWith("/") ? baseUrl : URI.create(baseUrl + "/");
    this.apiKey = apiKey;
    this.httpClient = HttpClient.newHttpClient();
    this.jsonMapper = JsonMapper.builder()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .build();
  }

  @Override
  public TaskStatusPollResponse submitConvertSource(ConvertDocumentRequest request) {
    return submit("/v1/convert/source/async", request);
  }

  @Override
  public TaskStatusPollResponse submitChunkHierarchicalSource(HierarchicalChunkDocumentRequest request) {
    return submit("/v1/chunk/hierarchical/source/async", request);
  }

  @Override
  public TaskStatusPollResponse submitChunkHybridSource(HybridChunkDocumentRequest request) {
    return submit("/v1/chunk/hybrid/source/async", request);
  }

  private TaskStatusPollResponse submit(String path, Object request) {
    String payload;
    try {
      payload = this.jsonMapper.writeValueAsString(request);
    } catch (JacksonException e) {
      throw new RuntimeException("Failed to serialize async request payload", e);
    }

    HttpRequest.Builder requestBuilder = HttpRequest.newBuilder(this.baseUrl.resolve(path))
        .header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8));

    if (this.apiKey != null && !this.apiKey.isBlank()) {
      requestBuilder.header(API_KEY_HEADER_NAME, this.apiKey);
    }

    HttpResponse<String> response;
    try {
      response = this.httpClient.send(
          requestBuilder.build(),
          HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException("Async submit interrupted", e);
    } catch (IOException e) {
      throw new RuntimeException("Failed to submit async request", e);
    }

    if (response.statusCode() < 200 || response.statusCode() >= 300) {
      throw new RuntimeException("Async submit failed with status %s: %s"
          .formatted(response.statusCode(), response.body()));
    }

    try {
      return this.jsonMapper.readValue(response.body(), TaskStatusPollResponse.class);
    } catch (JacksonException e) {
      throw new RuntimeException("Failed to parse async submit response", e);
    }
  }
}
