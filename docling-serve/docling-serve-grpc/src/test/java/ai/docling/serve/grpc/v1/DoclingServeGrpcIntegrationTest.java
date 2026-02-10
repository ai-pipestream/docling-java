package ai.docling.serve.grpc.v1;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.net.URI;
import java.time.Duration;
import java.util.Optional;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.extension.TestWatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ai.docling.serve.api.DoclingServeApi;
import ai.docling.serve.v1.Chunk;
import ai.docling.serve.v1.ChunkHierarchicalSourceRequest;
import ai.docling.serve.v1.ChunkHybridSourceRequest;
import ai.docling.serve.v1.ClearConvertersRequest;
import ai.docling.serve.v1.ClearResultsRequest;
import ai.docling.serve.v1.ConvertDocumentOptions;
import ai.docling.serve.v1.ConvertDocumentRequest;
import ai.docling.serve.v1.ConvertSourceRequest;
import ai.docling.serve.v1.DoclingServeServiceGrpc;
import ai.docling.serve.v1.HealthRequest;
import ai.docling.serve.v1.HierarchicalChunkRequest;
import ai.docling.serve.v1.HierarchicalChunkerOptions;
import ai.docling.serve.v1.HttpSource;
import ai.docling.serve.v1.HybridChunkRequest;
import ai.docling.serve.v1.HybridChunkerOptions;
import ai.docling.serve.v1.OutputFormat;
import ai.docling.serve.v1.Source;
import ai.docling.testcontainers.serve.DoclingServeContainer;
import ai.docling.testcontainers.serve.config.DoclingServeContainerConfig;

import io.grpc.ManagedChannel;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;

/**
 * Integration tests for the gRPC service backed by a real docling-serve container.
 * <p>
 * Stack: gRPC BlockingStub → DoclingServeGrpcService → DoclingServeJackson2Client (REST) → Docker Container (docling-serve)
 * <p>
 * These tests validate that proto ↔ Java mapping produces correct results against a real server.
 */
class DoclingServeGrpcIntegrationTest {

    private static final Logger LOG = LoggerFactory.getLogger(DoclingServeGrpcIntegrationTest.class);

    static final DoclingServeContainer container = new DoclingServeContainer(
        DoclingServeContainerConfig.builder()
            .image(DoclingServeContainerConfig.DOCLING_IMAGE)
            .build()
    );

    static {
        container.start();
    }

    // Log container output on test failure for debugging
    @RegisterExtension
    TestWatcher watcher = new TestWatcher() {
        @Override
        public void testFailed(ExtensionContext context, @Nullable Throwable cause) {
            LOG.error("""
                Test {}.{} failed with message: {}
                Container logs:
                {}
                """,
                getClass().getName(),
                context.getTestMethod().map(Method::getName).orElse(""),
                Optional.ofNullable(cause).map(Throwable::getMessage).orElse(""),
                container.getLogs());
        }
    };

    private static DoclingServeApi restClient;
    private static ManagedChannel channel;
    private static DoclingServeServiceGrpc.DoclingServeServiceBlockingStub stub;

    @BeforeAll
    static void setUp() throws Exception {
        // Create REST client pointing at the container
        restClient = DoclingServeApi.builder()
            .logRequests()
            .logResponses()
            .prettyPrint()
            .baseUrl(container.getApiUrl())
            .connectTimeout(Duration.ofSeconds(10))
            .readTimeout(Duration.ofMinutes(5))
            .build();

        // Start in-process gRPC server wrapping the REST client
        String serverName = InProcessServerBuilder.generateName();
        InProcessServerBuilder.forName(serverName)
            .directExecutor()
            .addService(new DoclingServeGrpcService(restClient, URI.create(container.getApiUrl())))
            .build()
            .start();

        channel = InProcessChannelBuilder.forName(serverName)
            .directExecutor()
            .build();

        stub = DoclingServeServiceGrpc.newBlockingStub(channel)
            .withDeadlineAfter(5, java.util.concurrent.TimeUnit.MINUTES);
    }

    @AfterAll
    static void tearDown() {
        if (channel != null) {
            channel.shutdownNow();
        }
    }

    // ==================== Health ====================

    @Nested
    class HealthTests {

        @Test
        void health() {
            var response = stub.health(
                HealthRequest.getDefaultInstance());

            assertThat(response.getStatus()).isEqualTo("ok");
        }
    }

    // ==================== Convert ====================

    @Nested
    class ConvertTests {

        @Test
        void convertSourceWithHttpUrl() {
            var request = ConvertSourceRequest.newBuilder()
                .setRequest(ai.docling.serve.v1.ConvertDocumentRequest.newBuilder()
                    .addSources(ai.docling.serve.v1.Source.newBuilder()
                        .setHttp(ai.docling.serve.v1.HttpSource.newBuilder()
                            .setUrl("https://docs.arconia.io/arconia-cli/latest/development/dev/")
                            .build())
                        .build())
                    .build())
                .build();

            var response = stub.convertSource(request);

            assertThat(response.getResponse().getStatus()).isNotEmpty();
            assertThat(response.getResponse().getDocument().getFilename()).isNotEmpty();
            assertThat(response.getResponse().getDocument().getMdContent()).isNotEmpty();

            if (response.getResponse().getProcessingTime() > 0) {
                assertThat(response.getResponse().getProcessingTime()).isPositive();
            }
        }

        @Test
        void convertSourceWithJsonOutput() {
            var request = ConvertSourceRequest.newBuilder()
                .setRequest(ai.docling.serve.v1.ConvertDocumentRequest.newBuilder()
                    .addSources(ai.docling.serve.v1.Source.newBuilder()
                        .setHttp(ai.docling.serve.v1.HttpSource.newBuilder()
                            .setUrl("https://docs.arconia.io/arconia-cli/latest/development/dev/")
                            .build())
                        .build())
                    .setOptions(ai.docling.serve.v1.ConvertDocumentOptions.newBuilder()
                        .addToFormats(ai.docling.serve.v1.OutputFormat.OUTPUT_FORMAT_JSON)
                        .build())
                    .build())
                .build();

            var response = stub.convertSource(request);

            assertThat(response.getResponse().getStatus()).isNotEmpty();
            assertThat(response.getResponse().getDocument().getFilename()).isNotEmpty();
            // JSON output means the DoclingDocument proto should be populated
            assertThat(response.getResponse().getDocument().hasJsonContent()).isTrue();
            assertThat(response.getResponse().getDocument().getJsonContent().getName()).isNotEmpty();
        }
    }

    // ==================== Chunk ====================

    @Nested
    class ChunkTests {

        @Test
        void chunkHierarchicalSource() {
            var request = ChunkHierarchicalSourceRequest.newBuilder()
                .setRequest(ai.docling.serve.v1.HierarchicalChunkRequest.newBuilder()
                    .addSources(ai.docling.serve.v1.Source.newBuilder()
                        .setHttp(ai.docling.serve.v1.HttpSource.newBuilder()
                            .setUrl("https://docs.arconia.io/arconia-cli/latest/development/dev/")
                            .build())
                        .build())
                    .setConvertOptions(ai.docling.serve.v1.ConvertDocumentOptions.newBuilder()
                        .addToFormats(ai.docling.serve.v1.OutputFormat.OUTPUT_FORMAT_JSON)
                        .build())
                    .setIncludeConvertedDoc(true)
                    .setChunkingOptions(ai.docling.serve.v1.HierarchicalChunkerOptions.newBuilder()
                        .setIncludeRawText(true)
                        .setUseMarkdownTables(true)
                        .build())
                    .build())
                .build();

            var response = stub.chunkHierarchicalSource(request);

            assertThat(response.getResponse().getChunksList()).isNotEmpty();
            assertThat(response.getResponse().getChunksList())
                .allMatch(chunk -> !chunk.getText().isEmpty());
            assertThat(response.getResponse().getProcessingTime()).isPositive();
        }

        @Test
        void chunkHybridSource() {
            var request = ChunkHybridSourceRequest.newBuilder()
                .setRequest(ai.docling.serve.v1.HybridChunkRequest.newBuilder()
                    .addSources(ai.docling.serve.v1.Source.newBuilder()
                        .setHttp(ai.docling.serve.v1.HttpSource.newBuilder()
                            .setUrl("https://docs.arconia.io/arconia-cli/latest/development/dev/")
                            .build())
                        .build())
                    .setConvertOptions(ai.docling.serve.v1.ConvertDocumentOptions.newBuilder()
                        .addToFormats(ai.docling.serve.v1.OutputFormat.OUTPUT_FORMAT_JSON)
                        .build())
                    .setIncludeConvertedDoc(true)
                    .setChunkingOptions(ai.docling.serve.v1.HybridChunkerOptions.newBuilder()
                        .setIncludeRawText(true)
                        .setUseMarkdownTables(true)
                        .setMaxTokens(10000)
                        .setTokenizer("sentence-transformers/all-MiniLM-L6-v2")
                        .build())
                    .build())
                .build();

            var response = stub.chunkHybridSource(request);

            assertThat(response.getResponse().getChunksList()).isNotEmpty();
            assertThat(response.getResponse().getChunksList())
                .allMatch(chunk -> !chunk.getText().isEmpty());
            assertThat(response.getResponse().getProcessingTime()).isPositive();
        }
    }

    // ==================== Clear ====================

    @Nested
    class ClearTests {

        @Test
        void clearConverters() {
            var response = stub.clearConverters(
                ClearConvertersRequest.getDefaultInstance());

            assertThat(response.getResponse().getStatus()).isEqualTo("ok");
        }

        @Test
        void clearResults() {
            var response = stub.clearResults(
                ClearResultsRequest.getDefaultInstance());

            assertThat(response.getResponse().getStatus()).isEqualTo("ok");
        }
    }
}
