package ai.docling.serve.grpc.v1.mapping;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.time.Duration;
import java.util.List;

import ai.docling.core.DoclingDocument;
import ai.docling.serve.api.chunk.response.Chunk;
import ai.docling.serve.api.chunk.response.ChunkDocumentResponse;
import ai.docling.serve.api.chunk.response.Document;
import ai.docling.serve.api.chunk.response.ExportDocumentResponse;
import ai.docling.serve.api.convert.request.options.InputFormat;
import ai.docling.serve.api.convert.request.options.OutputFormat;
import ai.docling.serve.api.convert.request.options.OcrEngine;
import ai.docling.serve.api.convert.request.options.PdfBackend;
import ai.docling.serve.api.convert.request.options.TableFormerMode;
import ai.docling.serve.api.convert.request.options.ProcessingPipeline;
import ai.docling.serve.api.convert.request.options.ImageRefMode;
import ai.docling.serve.api.convert.request.options.VlmModelType;
import ai.docling.serve.api.convert.request.source.FileSource;
import ai.docling.serve.api.convert.request.source.HttpSource;
import ai.docling.serve.api.convert.request.source.S3Source;
import ai.docling.serve.api.convert.request.target.InBodyTarget;
import ai.docling.serve.api.convert.request.target.PutTarget;
import ai.docling.serve.api.convert.request.target.ZipTarget;
import ai.docling.serve.api.convert.response.ConvertDocumentResponse;
import ai.docling.serve.api.convert.response.DocumentResponse;
import ai.docling.serve.api.convert.response.ErrorItem;
import ai.docling.serve.api.health.HealthCheckResponse;
import ai.docling.serve.api.clear.response.ClearResponse;
import ai.docling.serve.api.task.response.TaskStatus;
import ai.docling.serve.api.task.response.TaskStatusMetadata;
import ai.docling.serve.api.task.response.TaskStatusPollResponse;
import ai.docling.serve.v1.*;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ServeApiMapperTest {

    // ==================== Proto → Java (Request Mapping) ====================

    @Nested
    class ConvertDocumentRequestMapping {

        @Test
        void mapsFileSource() {
            var proto = ConvertDocumentRequest.newBuilder()
                .addSources(Source.newBuilder()
                    .setFile(ai.docling.serve.v1.FileSource.newBuilder()
                        .setBase64String("dGVzdA==")
                        .setFilename("test.pdf")
                        .build())
                    .build())
                .build();

            var java = ServeApiMapper.toJava(proto);

            assertThat(java.getSources()).hasSize(1);
            assertThat(java.getSources().get(0)).isInstanceOf(FileSource.class);
            var fileSource = (FileSource) java.getSources().get(0);
            assertThat(fileSource.getBase64String()).isEqualTo("dGVzdA==");
            assertThat(fileSource.getFilename()).isEqualTo("test.pdf");
        }

        @Test
        void mapsHttpSource() {
            var proto = ConvertDocumentRequest.newBuilder()
                .addSources(Source.newBuilder()
                    .setHttp(ai.docling.serve.v1.HttpSource.newBuilder()
                        .setUrl("https://example.com/doc.pdf")
                        .putHeaders("Authorization", "Bearer token")
                        .build())
                    .build())
                .build();

            var java = ServeApiMapper.toJava(proto);

            assertThat(java.getSources()).hasSize(1);
            assertThat(java.getSources().get(0)).isInstanceOf(HttpSource.class);
            var httpSource = (HttpSource) java.getSources().get(0);
            assertThat(httpSource.getUrl()).isEqualTo(URI.create("https://example.com/doc.pdf"));
            assertThat(httpSource.getHeaders()).containsEntry("Authorization", "Bearer token");
        }

        @Test
        void mapsS3Source() {
            var proto = ConvertDocumentRequest.newBuilder()
                .addSources(Source.newBuilder()
                    .setS3(ai.docling.serve.v1.S3Source.newBuilder()
                        .setEndpoint("https://s3.amazonaws.com")
                        .setAccessKey("AKID")
                        .setSecretKey("secret")
                        .setBucket("my-bucket")
                        .setKeyPrefix("docs/")
                        .setVerifySsl(true)
                        .build())
                    .build())
                .build();

            var java = ServeApiMapper.toJava(proto);

            assertThat(java.getSources()).hasSize(1);
            assertThat(java.getSources().get(0)).isInstanceOf(S3Source.class);
            var s3Source = (S3Source) java.getSources().get(0);
            assertThat(s3Source.getEndpoint()).isEqualTo("https://s3.amazonaws.com");
            assertThat(s3Source.getAccessKey()).isEqualTo("AKID");
            assertThat(s3Source.getSecretKey()).isEqualTo("secret");
            assertThat(s3Source.getBucket()).isEqualTo("my-bucket");
            assertThat(s3Source.getKeyPrefix()).isEqualTo("docs/");
            assertThat(s3Source.isVerifySsl()).isTrue();
        }

        @Test
        void mapsInBodyTarget() {
            var proto = ConvertDocumentRequest.newBuilder()
                .setTarget(ai.docling.serve.v1.Target.newBuilder()
                    .setInbody(ai.docling.serve.v1.InBodyTarget.newBuilder().build())
                    .build())
                .build();

            var java = ServeApiMapper.toJava(proto);
            assertThat(java.getTarget()).isInstanceOf(InBodyTarget.class);
        }

        @Test
        void mapsPutTarget() {
            var proto = ConvertDocumentRequest.newBuilder()
                .setTarget(ai.docling.serve.v1.Target.newBuilder()
                    .setPut(ai.docling.serve.v1.PutTarget.newBuilder()
                        .setUrl("https://example.com/upload")
                        .build())
                    .build())
                .build();

            var java = ServeApiMapper.toJava(proto);
            assertThat(java.getTarget()).isInstanceOf(PutTarget.class);
            assertThat(((PutTarget) java.getTarget()).getUrl())
                .isEqualTo(URI.create("https://example.com/upload"));
        }

        @Test
        void mapsZipTarget() {
            var proto = ConvertDocumentRequest.newBuilder()
                .setTarget(ai.docling.serve.v1.Target.newBuilder()
                    .setZip(ai.docling.serve.v1.ZipTarget.newBuilder().build())
                    .build())
                .build();

            var java = ServeApiMapper.toJava(proto);
            assertThat(java.getTarget()).isInstanceOf(ZipTarget.class);
        }

        @Test
        void mapsConvertOptions() {
            var proto = ConvertDocumentRequest.newBuilder()
                .setOptions(ConvertDocumentOptions.newBuilder()
                    .addFromFormats(ai.docling.serve.v1.InputFormat.INPUT_FORMAT_PDF)
                    .addFromFormats(ai.docling.serve.v1.InputFormat.INPUT_FORMAT_DOCX)
                    .addToFormats(ai.docling.serve.v1.OutputFormat.OUTPUT_FORMAT_JSON)
                    .addToFormats(ai.docling.serve.v1.OutputFormat.OUTPUT_FORMAT_MD)
                    .setDoOcr(true)
                    .setForceOcr(false)
                    .setOcrEngine(ai.docling.serve.v1.OcrEngine.OCR_ENGINE_EASYOCR)
                    .addOcrLang("en")
                    .addOcrLang("de")
                    .setPdfBackend(ai.docling.serve.v1.PdfBackend.PDF_BACKEND_DLPARSE_V2)
                    .setTableMode(ai.docling.serve.v1.TableFormerMode.TABLE_FORMER_MODE_ACCURATE)
                    .setPipeline(ai.docling.serve.v1.ProcessingPipeline.PROCESSING_PIPELINE_STANDARD)
                    .setImageExportMode(ai.docling.serve.v1.ImageRefMode.IMAGE_REF_MODE_EMBEDDED)
                    .setAbortOnError(true)
                    .setDoTableStructure(true)
                    .setIncludeImages(false)
                    .setImagesScale(2.0)
                    .setDocumentTimeout(30.0)
                    .setDoCodeEnrichment(true)
                    .setDoFormulaEnrichment(false)
                    .setDoPictureClassification(true)
                    .setDoPictureDescription(false)
                    .setPictureDescriptionAreaThreshold(0.5)
                    .setVlmPipelineModel(ai.docling.serve.v1.VlmModelType.VLM_MODEL_TYPE_SMOLDOCLING)
                    .build())
                .build();

            var java = ServeApiMapper.toJava(proto);
            var opts = java.getOptions();

            assertThat(opts).isNotNull();
            assertThat(opts.getFromFormats()).containsExactly(InputFormat.PDF, InputFormat.DOCX);
            assertThat(opts.getToFormats()).containsExactly(OutputFormat.JSON, OutputFormat.MARKDOWN);
            assertThat(opts.getDoOcr()).isTrue();
            assertThat(opts.getForceOcr()).isFalse();
            assertThat(opts.getOcrEngine()).isEqualTo(OcrEngine.EASYOCR);
            assertThat(opts.getOcrLang()).containsExactly("en", "de");
            assertThat(opts.getPdfBackend()).isEqualTo(PdfBackend.DLPARSE_V2);
            assertThat(opts.getTableMode()).isEqualTo(TableFormerMode.ACCURATE);
            assertThat(opts.getPipeline()).isEqualTo(ProcessingPipeline.STANDARD);
            assertThat(opts.getImageExportMode()).isEqualTo(ImageRefMode.EMBEDDED);
            assertThat(opts.getAbortOnError()).isTrue();
            assertThat(opts.getDoTableStructure()).isTrue();
            assertThat(opts.getIncludeImages()).isFalse();
            assertThat(opts.getImagesScale()).isEqualTo(2.0);
            assertThat(opts.getDocumentTimeout()).isEqualTo(Duration.ofSeconds(30));
            assertThat(opts.getDoCodeEnrichment()).isTrue();
            assertThat(opts.getDoFormulaEnrichment()).isFalse();
            assertThat(opts.getDoPictureClassification()).isTrue();
            assertThat(opts.getDoPictureDescription()).isFalse();
            assertThat(opts.getPictureDescriptionAreaThreshold()).isEqualTo(0.5);
            assertThat(opts.getVlmPipelineModel()).isEqualTo(VlmModelType.SMOLDOCLING);
        }

        @Test
        void mapsMultipleSources() {
            var proto = ConvertDocumentRequest.newBuilder()
                .addSources(Source.newBuilder()
                    .setFile(ai.docling.serve.v1.FileSource.newBuilder()
                        .setBase64String("YQ==")
                        .setFilename("a.pdf")
                        .build())
                    .build())
                .addSources(Source.newBuilder()
                    .setHttp(ai.docling.serve.v1.HttpSource.newBuilder()
                        .setUrl("https://example.com/b.pdf")
                        .build())
                    .build())
                .build();

            var java = ServeApiMapper.toJava(proto);
            assertThat(java.getSources()).hasSize(2);
            assertThat(java.getSources().get(0)).isInstanceOf(FileSource.class);
            assertThat(java.getSources().get(1)).isInstanceOf(HttpSource.class);
        }

        @Test
        void mapsEmptyRequest() {
            var proto = ConvertDocumentRequest.getDefaultInstance();
            var java = ServeApiMapper.toJava(proto);
            assertThat(java.getSources()).isEmpty();
            // Options defaults to an empty instance (not null) due to @Builder.Default
            assertThat(java.getOptions()).isNotNull();
            assertThat(java.getOptions().getFromFormats()).isEmpty();
            assertThat(java.getTarget()).isNull();
        }
    }

    @Nested
    class ChunkRequestMapping {

        @Test
        void mapsHierarchicalChunkRequest() {
            var proto = HierarchicalChunkRequest.newBuilder()
                .addSources(Source.newBuilder()
                    .setFile(ai.docling.serve.v1.FileSource.newBuilder()
                        .setBase64String("dGVzdA==")
                        .setFilename("test.pdf")
                        .build())
                    .build())
                .setIncludeConvertedDoc(true)
                .setChunkingOptions(ai.docling.serve.v1.HierarchicalChunkerOptions.newBuilder()
                    .setUseMarkdownTables(true)
                    .setIncludeRawText(false)
                    .build())
                .build();

            var java = ServeApiMapper.toJava(proto);

            assertThat(java.getSources()).hasSize(1);
            assertThat(java.isIncludeConvertedDoc()).isTrue();
            assertThat(java.getChunkingOptions()).isNotNull();
            assertThat(java.getChunkingOptions().isUseMarkdownTables()).isTrue();
            assertThat(java.getChunkingOptions().isIncludeRawText()).isFalse();
        }

        @Test
        void mapsHybridChunkRequest() {
            var proto = HybridChunkRequest.newBuilder()
                .addSources(Source.newBuilder()
                    .setFile(ai.docling.serve.v1.FileSource.newBuilder()
                        .setBase64String("dGVzdA==")
                        .setFilename("test.pdf")
                        .build())
                    .build())
                .setIncludeConvertedDoc(false)
                .setChunkingOptions(ai.docling.serve.v1.HybridChunkerOptions.newBuilder()
                    .setUseMarkdownTables(true)
                    .setIncludeRawText(true)
                    .setMaxTokens(512)
                    .setTokenizer("sentence-transformers/all-MiniLM-L6-v2")
                    .setMergePeers(true)
                    .build())
                .build();

            var java = ServeApiMapper.toJava(proto);

            assertThat(java.getSources()).hasSize(1);
            assertThat(java.isIncludeConvertedDoc()).isFalse();
            assertThat(java.getChunkingOptions()).isNotNull();
            assertThat(java.getChunkingOptions().isUseMarkdownTables()).isTrue();
            assertThat(java.getChunkingOptions().isIncludeRawText()).isTrue();
            assertThat(java.getChunkingOptions().getMaxTokens()).isEqualTo(512);
            assertThat(java.getChunkingOptions().getTokenizer())
                .isEqualTo("sentence-transformers/all-MiniLM-L6-v2");
            assertThat(java.getChunkingOptions().getMergePeers()).isTrue();
        }
    }

    @Nested
    class TaskRequestMapping {

        @Test
        void mapsTaskStatusPollRequest() {
            var proto = TaskStatusPollRequest.newBuilder()
                .setTaskId("task-123")
                .setWaitTime(5.0f)
                .build();

            var java = ServeApiMapper.toJava(proto);

            assertThat(java.getTaskId()).isEqualTo("task-123");
            assertThat(java.getWaitTime()).isEqualTo(Duration.ofMillis(5000));
        }

        @Test
        void mapsTaskResultRequest() {
            var proto = TaskResultRequest.newBuilder()
                .setTaskId("task-456")
                .build();

            var java = ServeApiMapper.toJava(proto);

            assertThat(java.getTaskId()).isEqualTo("task-456");
        }

        @Test
        void mapsClearResultsRequest() {
            var proto = ClearResultsRequest.newBuilder()
                .setOlderThan(60.0f)
                .build();

            var java = ServeApiMapper.toJava(proto);

            assertThat(java.getOlderThen()).isEqualTo(Duration.ofMillis(60000));
        }
    }

    // ==================== Java → Proto (Response Mapping) ====================

    @Nested
    class HealthResponseMapping {

        @Test
        void mapsHealthCheckResponse() {
            var java = HealthCheckResponse.builder()
                .status("ok")
                .build();

            var proto = ServeApiMapper.toProto(java);
            assertThat(proto.getStatus()).isEqualTo("ok");
        }

        @Test
        void handlesNullHealthCheckResponse() {
            var proto = ServeApiMapper.toProto((HealthCheckResponse) null);
            assertThat(proto.getStatus()).isEmpty();
        }
    }

    @Nested
    class ConvertDocumentResponseMapping {

        @Test
        void mapsConvertDocumentResponse() {
            var java = ConvertDocumentResponse.builder()
                .document(DocumentResponse.builder()
                    .filename("test.pdf")
                    .markdownContent("# Hello")
                    .htmlContent("<h1>Hello</h1>")
                    .textContent("Hello")
                    .doctagsContent("<doc>Hello</doc>")
                    .build())
                .error(ErrorItem.builder()
                    .componentType("ocr")
                    .errorMessage("OCR failed")
                    .moduleName("tesseract")
                    .build())
                .processingTime(1.5)
                .status("partial")
                .timing("parse", 0.5)
                .timing("ocr", 1.0)
                .build();

            var proto = ServeApiMapper.toProto(java);

            assertThat(proto.getDocument().getFilename()).isEqualTo("test.pdf");
            assertThat(proto.getDocument().getMdContent()).isEqualTo("# Hello");
            assertThat(proto.getDocument().getHtmlContent()).isEqualTo("<h1>Hello</h1>");
            assertThat(proto.getDocument().getTextContent()).isEqualTo("Hello");
            assertThat(proto.getDocument().getDoctagsContent()).isEqualTo("<doc>Hello</doc>");
            assertThat(proto.getErrorsCount()).isEqualTo(1);
            assertThat(proto.getErrors(0).getComponentType()).isEqualTo("ocr");
            assertThat(proto.getErrors(0).getErrorMessage()).isEqualTo("OCR failed");
            assertThat(proto.getErrors(0).getModuleName()).isEqualTo("tesseract");
            assertThat(proto.getProcessingTime()).isEqualTo(1.5);
            assertThat(proto.getStatus()).isEqualTo("partial");
            assertThat(proto.getTimingsMap()).containsEntry("parse", 0.5);
            assertThat(proto.getTimingsMap()).containsEntry("ocr", 1.0);
        }

        @Test
        void mapsResponseWithJsonContent() {
            var doclingDoc = DoclingDocument.builder()
                .name("test-doc")
                .body(DoclingDocument.GroupItem.builder()
                    .selfRef("#/body")
                    .name("body")
                    .label(DoclingDocument.GroupLabel.UNSPECIFIED)
                    .build())
                .build();

            var java = ConvertDocumentResponse.builder()
                .document(DocumentResponse.builder()
                    .filename("test.pdf")
                    .jsonContent(doclingDoc)
                    .build())
                .build();

            var proto = ServeApiMapper.toProto(java);

            assertThat(proto.getDocument().hasJsonContent()).isTrue();
            assertThat(proto.getDocument().getJsonContent().getName()).isEqualTo("test-doc");
        }
    }

    @Nested
    class ChunkDocumentResponseMapping {

        @Test
        void mapsChunkDocumentResponse() {
            var java = ChunkDocumentResponse.builder()
                .chunk(Chunk.builder()
                    .filename("test.pdf")
                    .chunkIndex(0)
                    .text("## Heading\nThis is chunk text")
                    .rawText("This is chunk text")
                    .numTokens(5)
                    .heading("Heading")
                    .caption("Figure 1")
                    .docItem("item-1")
                    .pageNumber(1)
                    .metadata("key1", "value1")
                    .build())
                .document(Document.builder()
                    .kind("pdf")
                    .content(ExportDocumentResponse.builder()
                        .filename("test.pdf")
                        .markdownContent("# Test")
                        .build())
                    .status("success")
                    .build())
                .processingTime(0.8)
                .build();

            var proto = ServeApiMapper.toProto(java);

            assertThat(proto.getChunksCount()).isEqualTo(1);
            var chunk = proto.getChunks(0);
            assertThat(chunk.getFilename()).isEqualTo("test.pdf");
            assertThat(chunk.getChunkIndex()).isZero();
            assertThat(chunk.getText()).isEqualTo("## Heading\nThis is chunk text");
            assertThat(chunk.getRawText()).isEqualTo("This is chunk text");
            assertThat(chunk.getNumTokens()).isEqualTo(5);
            assertThat(chunk.getHeadingsList()).containsExactly("Heading");
            assertThat(chunk.getCaptionsList()).containsExactly("Figure 1");
            assertThat(chunk.getDocItemsList()).containsExactly("item-1");
            assertThat(chunk.getPageNumbersList()).containsExactly(1);
            assertThat(chunk.getMetadataMap()).containsEntry("key1", "value1");

            assertThat(proto.getDocumentsCount()).isEqualTo(1);
            var doc = proto.getDocuments(0);
            assertThat(doc.getKind()).isEqualTo("pdf");
            assertThat(doc.getContent().getFilename()).isEqualTo("test.pdf");
            assertThat(doc.getContent().getMdContent()).isEqualTo("# Test");
            assertThat(doc.getStatus()).isEqualTo("success");

            assertThat(proto.getProcessingTime()).isEqualTo(0.8);
        }
    }

    @Nested
    class TaskStatusPollResponseMapping {

        @Test
        void mapsTaskStatusPollResponse() {
            var java = TaskStatusPollResponse.builder()
                .taskId("task-789")
                .taskType("convert")
                .taskStatus(TaskStatus.SUCCESS)
                .taskPosition(0L)
                .taskStatusMetadata(TaskStatusMetadata.builder()
                    .numDocs(10L)
                    .numProcessed(10L)
                    .numSucceeded(8L)
                    .numFailed(2L)
                    .build())
                .build();

            var proto = ServeApiMapper.toProto(java);

            assertThat(proto.getTaskId()).isEqualTo("task-789");
            assertThat(proto.getTaskType()).isEqualTo("convert");
            assertThat(proto.getTaskStatus())
                .isEqualTo(ai.docling.serve.v1.TaskStatus.TASK_STATUS_SUCCESS);
            assertThat(proto.getTaskPosition()).isZero();
            assertThat(proto.getTaskMeta().getNumDocs()).isEqualTo(10L);
            assertThat(proto.getTaskMeta().getNumProcessed()).isEqualTo(10L);
            assertThat(proto.getTaskMeta().getNumSucceeded()).isEqualTo(8L);
            assertThat(proto.getTaskMeta().getNumFailed()).isEqualTo(2L);
        }

        @Test
        void mapsAllTaskStatuses() {
            for (TaskStatus status : TaskStatus.values()) {
                var java = TaskStatusPollResponse.builder()
                    .taskStatus(status)
                    .build();
                var proto = ServeApiMapper.toProto(java);
                assertThat(proto.getTaskStatus()).isNotEqualTo(
                    ai.docling.serve.v1.TaskStatus.UNRECOGNIZED);
            }
        }
    }

    @Nested
    class ClearResponseMapping {

        @Test
        void mapsClearResponse() {
            var java = ClearResponse.builder()
                .status("cleared")
                .build();

            var proto = ServeApiMapper.toProto(java);
            assertThat(proto.getStatus()).isEqualTo("cleared");
        }

        @Test
        void handlesNullClearResponse() {
            var proto = ServeApiMapper.toProto((ClearResponse) null);
            assertThat(proto.getStatus()).isEmpty();
        }
    }

    // ==================== Enum Mapping Tests ====================

    @Nested
    class EnumMappingTests {

        @Test
        void mapsAllInputFormats() {
            // Build a request with all input formats and verify round-trip
            var protoFormats = List.of(
                ai.docling.serve.v1.InputFormat.INPUT_FORMAT_ASCIIDOC,
                ai.docling.serve.v1.InputFormat.INPUT_FORMAT_AUDIO,
                ai.docling.serve.v1.InputFormat.INPUT_FORMAT_CSV,
                ai.docling.serve.v1.InputFormat.INPUT_FORMAT_DOCX,
                ai.docling.serve.v1.InputFormat.INPUT_FORMAT_HTML,
                ai.docling.serve.v1.InputFormat.INPUT_FORMAT_IMAGE,
                ai.docling.serve.v1.InputFormat.INPUT_FORMAT_JSON_DOCLING,
                ai.docling.serve.v1.InputFormat.INPUT_FORMAT_MD,
                ai.docling.serve.v1.InputFormat.INPUT_FORMAT_METS_GBS,
                ai.docling.serve.v1.InputFormat.INPUT_FORMAT_PDF,
                ai.docling.serve.v1.InputFormat.INPUT_FORMAT_PPTX,
                ai.docling.serve.v1.InputFormat.INPUT_FORMAT_XLSX,
                ai.docling.serve.v1.InputFormat.INPUT_FORMAT_XML_JATS,
                ai.docling.serve.v1.InputFormat.INPUT_FORMAT_XML_USPTO
            );

            var expectedJava = List.of(
                InputFormat.ASCIIDOC,
                InputFormat.AUDIO,
                InputFormat.CSV,
                InputFormat.DOCX,
                InputFormat.HTML,
                InputFormat.IMAGE,
                InputFormat.JSON_DOCLING,
                InputFormat.MARKDOWN,
                InputFormat.METS_GBS,
                InputFormat.PDF,
                InputFormat.PPTX,
                InputFormat.XLSX,
                InputFormat.XML_JATS,
                InputFormat.XML_USPTO
            );

            var builder = ConvertDocumentOptions.newBuilder();
            protoFormats.forEach(builder::addFromFormats);

            var proto = ConvertDocumentRequest.newBuilder()
                .setOptions(builder.build())
                .build();
            var java = ServeApiMapper.toJava(proto);

            assertThat(java.getOptions().getFromFormats())
                .containsExactlyElementsOf(expectedJava);
        }

        @Test
        void mapsAllOutputFormats() {
            var builder = ConvertDocumentOptions.newBuilder()
                .addToFormats(ai.docling.serve.v1.OutputFormat.OUTPUT_FORMAT_DOCTAGS)
                .addToFormats(ai.docling.serve.v1.OutputFormat.OUTPUT_FORMAT_HTML)
                .addToFormats(ai.docling.serve.v1.OutputFormat.OUTPUT_FORMAT_HTML_SPLIT_PAGE)
                .addToFormats(ai.docling.serve.v1.OutputFormat.OUTPUT_FORMAT_JSON)
                .addToFormats(ai.docling.serve.v1.OutputFormat.OUTPUT_FORMAT_MD)
                .addToFormats(ai.docling.serve.v1.OutputFormat.OUTPUT_FORMAT_TEXT);

            var proto = ConvertDocumentRequest.newBuilder()
                .setOptions(builder.build())
                .build();
            var java = ServeApiMapper.toJava(proto);

            assertThat(java.getOptions().getToFormats()).containsExactly(
                OutputFormat.DOCTAGS,
                OutputFormat.HTML,
                OutputFormat.HTML_SPLIT_PAGE,
                OutputFormat.JSON,
                OutputFormat.MARKDOWN,
                OutputFormat.TEXT
            );
        }

        @Test
        void mapsAllOcrEngines() {
            var engines = List.of(
                ai.docling.serve.v1.OcrEngine.OCR_ENGINE_AUTO,
                ai.docling.serve.v1.OcrEngine.OCR_ENGINE_EASYOCR,
                ai.docling.serve.v1.OcrEngine.OCR_ENGINE_OCRMAC,
                ai.docling.serve.v1.OcrEngine.OCR_ENGINE_RAPIDOCR,
                ai.docling.serve.v1.OcrEngine.OCR_ENGINE_TESSEROCR,
                ai.docling.serve.v1.OcrEngine.OCR_ENGINE_TESSERACT
            );
            var expected = List.of(
                OcrEngine.AUTO, OcrEngine.EASYOCR, OcrEngine.OCRMAC,
                OcrEngine.RAPIDOCR, OcrEngine.TESSEROCR, OcrEngine.TESSERACT
            );

            for (int i = 0; i < engines.size(); i++) {
                var proto = ConvertDocumentRequest.newBuilder()
                    .setOptions(ConvertDocumentOptions.newBuilder()
                        .setOcrEngine(engines.get(i))
                        .build())
                    .build();
                var java = ServeApiMapper.toJava(proto);
                assertThat(java.getOptions().getOcrEngine()).isEqualTo(expected.get(i));
            }
        }

        @Test
        void mapsAllPdfBackends() {
            var backends = List.of(
                ai.docling.serve.v1.PdfBackend.PDF_BACKEND_DLPARSE_V1,
                ai.docling.serve.v1.PdfBackend.PDF_BACKEND_DLPARSE_V2,
                ai.docling.serve.v1.PdfBackend.PDF_BACKEND_DLPARSE_V4,
                ai.docling.serve.v1.PdfBackend.PDF_BACKEND_PYPDFIUM2
            );
            var expected = List.of(
                PdfBackend.DLPARSE_V1, PdfBackend.DLPARSE_V2,
                PdfBackend.DLPARSE_V4, PdfBackend.PYPDFIUM2
            );

            for (int i = 0; i < backends.size(); i++) {
                var proto = ConvertDocumentRequest.newBuilder()
                    .setOptions(ConvertDocumentOptions.newBuilder()
                        .setPdfBackend(backends.get(i))
                        .build())
                    .build();
                var java = ServeApiMapper.toJava(proto);
                assertThat(java.getOptions().getPdfBackend()).isEqualTo(expected.get(i));
            }
        }

        @Test
        void mapsAllVlmModelTypes() {
            var models = List.of(
                ai.docling.serve.v1.VlmModelType.VLM_MODEL_TYPE_SMOLDOCLING,
                ai.docling.serve.v1.VlmModelType.VLM_MODEL_TYPE_SMOLDOCLING_VLLM,
                ai.docling.serve.v1.VlmModelType.VLM_MODEL_TYPE_GRANITE_VISION,
                ai.docling.serve.v1.VlmModelType.VLM_MODEL_TYPE_GRANITE_VISION_VLLM,
                ai.docling.serve.v1.VlmModelType.VLM_MODEL_TYPE_GRANITE_VISION_OLLAMA,
                ai.docling.serve.v1.VlmModelType.VLM_MODEL_TYPE_GOT_OCR_2
            );
            var expected = List.of(
                VlmModelType.SMOLDOCLING, VlmModelType.SMOLDOCLING_VLLM,
                VlmModelType.GRANITE_VISION, VlmModelType.GRANITE_VISION_VLLM,
                VlmModelType.GRANITE_VISION_OLLAMA, VlmModelType.GOT_OCR_2
            );

            for (int i = 0; i < models.size(); i++) {
                var proto = ConvertDocumentRequest.newBuilder()
                    .setOptions(ConvertDocumentOptions.newBuilder()
                        .setVlmPipelineModel(models.get(i))
                        .build())
                    .build();
                var java = ServeApiMapper.toJava(proto);
                assertThat(java.getOptions().getVlmPipelineModel()).isEqualTo(expected.get(i));
            }
        }
    }
}
