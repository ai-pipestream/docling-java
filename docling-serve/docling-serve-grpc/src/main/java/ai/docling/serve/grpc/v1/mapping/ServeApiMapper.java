package ai.docling.serve.grpc.v1.mapping;

// Java API types (the canonical domain types)

import ai.docling.serve.api.chunk.request.HierarchicalChunkDocumentRequest;
import ai.docling.serve.api.chunk.request.HybridChunkDocumentRequest;
import ai.docling.serve.api.chunk.request.options.HierarchicalChunkerOptions;
import ai.docling.serve.api.chunk.request.options.HybridChunkerOptions;
import ai.docling.serve.api.chunk.response.ChunkDocumentResponse;
import ai.docling.serve.api.chunk.response.Document;
import ai.docling.serve.api.chunk.response.ExportDocumentResponse;
import ai.docling.serve.api.clear.request.ClearResultsRequest;
import ai.docling.serve.api.clear.response.ClearResponse;
import ai.docling.serve.api.convert.request.ConvertDocumentRequest;
import ai.docling.serve.api.convert.request.options.ConvertDocumentOptions;
import ai.docling.serve.api.convert.request.options.ImageRefMode;
import ai.docling.serve.api.convert.request.options.InputFormat;
import ai.docling.serve.api.convert.request.options.OcrEngine;
import ai.docling.serve.api.convert.request.options.OutputFormat;
import ai.docling.serve.api.convert.request.options.PdfBackend;
import ai.docling.serve.api.convert.request.options.ProcessingPipeline;
import ai.docling.serve.api.convert.request.options.TableFormerMode;
import ai.docling.serve.api.convert.request.options.VlmModelType;
import ai.docling.serve.api.convert.request.source.FileSource;
import ai.docling.serve.api.convert.request.source.HttpSource;
import ai.docling.serve.api.convert.request.source.S3Source;
import ai.docling.serve.api.convert.request.source.Source;
import ai.docling.serve.api.convert.request.target.InBodyTarget;
import ai.docling.serve.api.convert.request.target.PutTarget;
import ai.docling.serve.api.convert.request.target.Target;
import ai.docling.serve.api.convert.request.target.ZipTarget;
import ai.docling.serve.api.convert.response.ConvertDocumentResponse;
import ai.docling.serve.api.convert.response.DocumentResponse;
import ai.docling.serve.api.convert.response.ErrorItem;
import ai.docling.serve.api.health.HealthCheckResponse;
import ai.docling.serve.api.task.request.TaskResultRequest;
import ai.docling.serve.api.task.request.TaskStatusPollRequest;
import ai.docling.serve.api.task.response.TaskStatus;
import ai.docling.serve.api.task.response.TaskStatusMetadata;
import ai.docling.serve.api.task.response.TaskStatusPollResponse;
import ai.docling.serve.v1.*;

import java.net.URI;
import java.time.Duration;

/**
 * Bidirectional mapper between proto serve types (ai.docling.serve.v1.*)
 * and Java API models (ai.docling.serve.api.*).
 * <p>
 * Proto → Java: for incoming gRPC requests that need to call the REST client.
 * Java → Proto: for REST client responses that need to be returned as gRPC responses.
 */
public class ServeApiMapper {

  // ==================== Proto → Java (Request Mapping) ====================

  /**
   * Maps proto ConvertDocumentRequest → Java ConvertDocumentRequest.
   */
  public static ConvertDocumentRequest toJava(
      ai.docling.serve.v1.ConvertDocumentRequest proto) {
    ConvertDocumentRequest.Builder builder = ConvertDocumentRequest.builder();

    if (proto == null) {
      return builder.build();
    }

    // Sources
    if (proto.getSourcesList() != null) {
      for (ai.docling.serve.v1.Source protoSource : proto.getSourcesList()) {
        Source javaSource = toJavaSource(protoSource);
        if (javaSource != null) {
          builder.source(javaSource);
        }
      }
    }

    // Options
    if (proto.hasOptions()) {
      builder.options(toJavaOptions(proto.getOptions()));
    }

    // Target
    if (proto.hasTarget()) {
      Target javaTarget = toJavaTarget(proto.getTarget());
      if (javaTarget != null) {
        builder.target(javaTarget);
      }
    }

    return builder.build();
  }

  /**
   * Maps proto HierarchicalChunkRequest → Java HierarchicalChunkDocumentRequest.
   */
  public static HierarchicalChunkDocumentRequest toJava(
      ai.docling.serve.v1.HierarchicalChunkRequest proto) {
    var builder = HierarchicalChunkDocumentRequest.builder();

    if (proto == null) {
      return builder.build();
    }

    if (proto.getSourcesList() != null) {
      for (ai.docling.serve.v1.Source protoSource : proto.getSourcesList()) {
        Source javaSource = toJavaSource(protoSource);
        if (javaSource != null) {
          builder.source(javaSource);
        }
      }
    }

    if (proto.hasConvertOptions()) {
      builder.options(toJavaOptions(proto.getConvertOptions()));
    }

    if (proto.hasTarget()) {
      Target javaTarget = toJavaTarget(proto.getTarget());
      if (javaTarget != null) {
        builder.target(javaTarget);
      }
    }

    builder.includeConvertedDoc(proto.getIncludeConvertedDoc());

    if (proto.hasChunkingOptions()) {
      builder.chunkingOptions(toJavaHierarchicalOptions(proto.getChunkingOptions()));
    }

    return builder.build();
  }

  /**
   * Maps proto HybridChunkRequest → Java HybridChunkDocumentRequest.
   */
  public static HybridChunkDocumentRequest toJava(
      ai.docling.serve.v1.HybridChunkRequest proto) {
    var builder = HybridChunkDocumentRequest.builder();

    if (proto == null) {
      return builder.build();
    }

    if (proto.getSourcesList() != null) {
      for (ai.docling.serve.v1.Source protoSource : proto.getSourcesList()) {
        Source javaSource = toJavaSource(protoSource);
        if (javaSource != null) {
          builder.source(javaSource);
        }
      }
    }

    if (proto.hasConvertOptions()) {
      builder.options(toJavaOptions(proto.getConvertOptions()));
    }

    if (proto.hasTarget()) {
      Target javaTarget = toJavaTarget(proto.getTarget());
      if (javaTarget != null) {
        builder.target(javaTarget);
      }
    }

    builder.includeConvertedDoc(proto.getIncludeConvertedDoc());

    if (proto.hasChunkingOptions()) {
      builder.chunkingOptions(toJavaHybridOptions(proto.getChunkingOptions()));
    }

    return builder.build();
  }

  /**
   * Maps proto TaskStatusPollRequest → Java TaskStatusPollRequest.
   */
  public static TaskStatusPollRequest toJava(
      ai.docling.serve.v1.TaskStatusPollRequest proto) {
    if (proto == null) {
      return TaskStatusPollRequest.builder().build();
    }
    return TaskStatusPollRequest.builder()
        .taskId(proto.getTaskId())
        .waitTime(Duration.ofMillis((long) (proto.getWaitTime() * 1000)))
        .build();
  }

  /**
   * Maps proto TaskResultRequest → Java TaskResultRequest.
   */
  public static TaskResultRequest toJava(
      ai.docling.serve.v1.TaskResultRequest proto) {
    if (proto == null) {
      return TaskResultRequest.builder().build();
    }
    return TaskResultRequest.builder()
        .taskId(proto.getTaskId())
        .build();
  }

  /**
   * Maps proto ClearResultsRequest → Java ClearResultsRequest.
   */
  public static ClearResultsRequest toJava(
      ai.docling.serve.v1.ClearResultsRequest proto) {
    if (proto == null) {
      return ClearResultsRequest.builder().build();
    }
    return ClearResultsRequest.builder()
        .olderThen(Duration.ofMillis((long) (proto.getOlderThan() * 1000)))
        .build();
  }

  // ==================== Java → Proto (Response Mapping) ====================

  /**
   * Maps Java HealthCheckResponse → proto HealthResponse.
   */
  public static ai.docling.serve.v1.HealthResponse toProto(HealthCheckResponse java) {
    var builder = ai.docling.serve.v1.HealthResponse.newBuilder();
    if (java != null) {
      if (java.getStatus() != null) {
        builder.setStatus(java.getStatus());
      }
    }
    return builder.build();
  }

  /**
   * Maps Java ConvertDocumentResponse → proto ConvertDocumentResponse.
   */
  public static ai.docling.serve.v1.ConvertDocumentResponse toProto(
      ConvertDocumentResponse java) {
    ai.docling.serve.v1.ConvertDocumentResponse.Builder builder =
        ai.docling.serve.v1.ConvertDocumentResponse.newBuilder();

    if (java == null) {
      return builder.build();
    }

    if (java.getDocument() != null) {
      builder.setDocument(toProtoDocumentResponse(java.getDocument()));
    }
    if (java.getErrors() != null) {
      java.getErrors().forEach(error -> builder.addErrors(toProtoErrorItem(error)));
    }
    if (java.getProcessingTime() != null) {
      builder.setProcessingTime(java.getProcessingTime());
    }
    if (java.getStatus() != null) {
      builder.setStatus(java.getStatus());
    }
    if (java.getTimings() != null) {
      java.getTimings().forEach((key, value) -> {
        if (value instanceof Number) {
          builder.putTimings(key, ((Number) value).doubleValue());
        }
      });
    }

    return builder.build();
  }

  /**
   * Maps Java ChunkDocumentResponse → proto ChunkDocumentResponse.
   */
  public static ai.docling.serve.v1.ChunkDocumentResponse toProto(
      ChunkDocumentResponse java) {
    ai.docling.serve.v1.ChunkDocumentResponse.Builder builder =
        ai.docling.serve.v1.ChunkDocumentResponse.newBuilder();

    if (java == null) {
      return builder.build();
    }

    if (java.getChunks() != null) {
      java.getChunks().forEach(chunk -> builder.addChunks(toProtoChunk(chunk)));
    }
    if (java.getDocuments() != null) {
      java.getDocuments().forEach(doc -> builder.addDocuments(toProtoDocument(doc)));
    }
    if (java.getProcessingTime() != null) {
      builder.setProcessingTime(java.getProcessingTime());
    }

    return builder.build();
  }

  /**
   * Maps Java TaskStatusPollResponse → proto TaskStatusPollResponse.
   */
  public static ai.docling.serve.v1.TaskStatusPollResponse toProto(
      TaskStatusPollResponse java) {
    var builder = ai.docling.serve.v1.TaskStatusPollResponse.newBuilder();

    if (java == null) {
      return builder.build();
    }

    if (java.getTaskId() != null) {
      builder.setTaskId(java.getTaskId());
    }
    if (java.getTaskType() != null) {
      builder.setTaskType(java.getTaskType());
    }
    if (java.getTaskStatus() != null) {
      builder.setTaskStatus(toProtoTaskStatus(java.getTaskStatus()));
    }
    if (java.getTaskPosition() != null) {
      builder.setTaskPosition(java.getTaskPosition());
    }
    if (java.getTaskStatusMetadata() != null) {
      builder.setTaskMeta(toProtoTaskStatusMetadata(java.getTaskStatusMetadata()));
    }

    return builder.build();
  }

  /**
   * Maps Java ClearResponse → proto ClearResponse.
   */
  public static ai.docling.serve.v1.ClearResponse toProto(ClearResponse java) {
    var builder = ai.docling.serve.v1.ClearResponse.newBuilder();
    if (java != null) {
      if (java.getStatus() != null) {
        builder.setStatus(java.getStatus());
      }
    }
    return builder.build();
  }

  // ==================== Private Helpers: Proto → Java ====================

  private static Source toJavaSource(ai.docling.serve.v1.Source proto) {
    return switch (proto.getSourceCase()) {
      case FILE -> FileSource.builder()
          .base64String(proto.getFile().getBase64String())
          .filename(proto.getFile().getFilename())
          .build();
      case HTTP -> {
        HttpSource.Builder httpBuilder = HttpSource.builder()
            .url(URI.create(proto.getHttp().getUrl()));
        if (proto.getHttp().getHeadersMap() != null) {
          proto.getHttp().getHeadersMap().forEach(
              httpBuilder::header);
        }
        yield httpBuilder.build();
      }
      case S3 -> {
        S3Source.Builder s3Builder = S3Source.builder()
            .endpoint(proto.getS3().getEndpoint())
            .accessKey(proto.getS3().getAccessKey())
            .secretKey(proto.getS3().getSecretKey())
            .bucket(proto.getS3().getBucket())
            .verifySsl(proto.getS3().getVerifySsl());
        if (proto.getS3().hasKeyPrefix()) {
          s3Builder.keyPrefix(proto.getS3().getKeyPrefix());
        }
        yield s3Builder.build();
      }
      case SOURCE_NOT_SET -> null;
    };
  }

  private static Target toJavaTarget(ai.docling.serve.v1.Target proto) {
    return switch (proto.getTargetCase()) {
      case INBODY -> InBodyTarget.builder().build();
      case PUT -> PutTarget.builder()
          .url(URI.create(proto.getPut().getUrl()))
          .build();
      case S3 -> {
        ai.docling.serve.v1.S3Target s3 = proto.getS3();
        ai.docling.serve.api.convert.request.target.S3Target.Builder s3Builder =
            ai.docling.serve.api.convert.request.target.S3Target.builder()
                .endpoint(s3.getEndpoint())
                .accessKey(s3.getAccessKey())
                .secretKey(s3.getSecretKey())
                .bucket(s3.getBucket())
                .verifySsl(s3.getVerifySsl());
        if (s3.hasKeyPrefix()) {
          s3Builder.keyPrefix(s3.getKeyPrefix());
        }
        yield s3Builder.build();
      }
      case ZIP -> ZipTarget.builder().build();
      case TARGET_NOT_SET -> null;
    };
  }

  private static ConvertDocumentOptions toJavaOptions(
      ai.docling.serve.v1.ConvertDocumentOptions proto) {
    ConvertDocumentOptions.Builder builder = ConvertDocumentOptions.builder();

    // Input/Output formats
    if (proto.getFromFormatsList() != null) {
      proto.getFromFormatsList().forEach(f -> {
        InputFormat jf = toJavaInputFormat(f);
        if (jf != null) builder.fromFormat(jf);
      });
    }
    if (proto.getToFormatsList() != null) {
      proto.getToFormatsList().forEach(f -> {
        OutputFormat jf = toJavaOutputFormat(f);
        if (jf != null) builder.toFormat(jf);
      });
    }

    if (proto.hasImageExportMode()) {
      builder.imageExportMode(toJavaImageRefMode(proto.getImageExportMode()));
    }
    if (proto.hasDoOcr()) builder.doOcr(proto.getDoOcr());
    if (proto.hasForceOcr()) builder.forceOcr(proto.getForceOcr());
    if (proto.hasOcrEngine()) builder.ocrEngine(toJavaOcrEngine(proto.getOcrEngine()));
    if (proto.getOcrLangList() != null) {
      proto.getOcrLangList().forEach(builder::ocrLang);
    }
    if (proto.hasPdfBackend()) builder.pdfBackend(toJavaPdfBackend(proto.getPdfBackend()));
    if (proto.hasTableMode()) builder.tableMode(toJavaTableFormerMode(proto.getTableMode()));
    if (proto.hasTableCellMatching()) builder.tableCellMatching(proto.getTableCellMatching());
    if (proto.hasPipeline()) builder.pipeline(toJavaPipeline(proto.getPipeline()));
    if (proto.getPageRangeList() != null) {
      proto.getPageRangeList().forEach(builder::pageRange);
    }
    if (proto.hasDocumentTimeout()) {
      builder.documentTimeout(Duration.ofMillis((long) (proto.getDocumentTimeout() * 1000)));
    }
    if (proto.hasAbortOnError()) builder.abortOnError(proto.getAbortOnError());
    if (proto.hasDoTableStructure()) builder.doTableStructure(proto.getDoTableStructure());
    if (proto.hasIncludeImages()) builder.includeImages(proto.getIncludeImages());
    if (proto.hasImagesScale()) builder.imagesScale(proto.getImagesScale());
    if (proto.hasMdPageBreakPlaceholder()) {
      builder.mdPageBreakPlaceholder(proto.getMdPageBreakPlaceholder());
    }
    if (proto.hasDoCodeEnrichment()) builder.doCodeEnrichment(proto.getDoCodeEnrichment());
    if (proto.hasDoFormulaEnrichment()) builder.doFormulaEnrichment(proto.getDoFormulaEnrichment());
    if (proto.hasDoPictureClassification()) {
      builder.doPictureClassification(proto.getDoPictureClassification());
    }
    if (proto.hasDoPictureDescription()) {
      builder.doPictureDescription(proto.getDoPictureDescription());
    }
    if (proto.hasPictureDescriptionAreaThreshold()) {
      builder.pictureDescriptionAreaThreshold(proto.getPictureDescriptionAreaThreshold());
    }
    if (proto.hasVlmPipelineModel()) {
      builder.vlmPipelineModel(toJavaVlmModelType(proto.getVlmPipelineModel()));
    }
    if (proto.hasVlmPipelineModelLocal()) {
      builder.vlmPipelineModelLocal(proto.getVlmPipelineModelLocal());
    }
    if (proto.hasVlmPipelineModelApi()) {
      builder.vlmPipelineModelApi(proto.getVlmPipelineModelApi());
    }

    return builder.build();
  }

  private static HierarchicalChunkerOptions toJavaHierarchicalOptions(
      ai.docling.serve.v1.HierarchicalChunkerOptions proto) {
    return HierarchicalChunkerOptions.builder()
        .useMarkdownTables(proto.getUseMarkdownTables())
        .includeRawText(proto.getIncludeRawText())
        .build();
  }

  private static HybridChunkerOptions toJavaHybridOptions(
      ai.docling.serve.v1.HybridChunkerOptions proto) {
    HybridChunkerOptions.Builder builder = HybridChunkerOptions.builder()
        .useMarkdownTables(proto.getUseMarkdownTables())
        .includeRawText(proto.getIncludeRawText());
    if (proto.hasMaxTokens()) builder.maxTokens(proto.getMaxTokens());
    if (proto.hasTokenizer()) builder.tokenizer(proto.getTokenizer());
    if (proto.hasMergePeers()) builder.mergePeers(proto.getMergePeers());
    return builder.build();
  }

  // ==================== Private Helpers: Java → Proto ====================

  private static ai.docling.serve.v1.DocumentResponse toProtoDocumentResponse(
      DocumentResponse java) {
    var builder = ai.docling.serve.v1.DocumentResponse.newBuilder();

    if (java.getFilename() != null) {
      builder.setFilename(java.getFilename());
    }
    if (java.getJsonContent() != null) {
      builder.setJsonContent(ai.docling.serve.grpc.v1.mapping.DoclingDocumentMapper.map(java.getJsonContent()));
    }
    if (java.getMarkdownContent() != null) {
      builder.setMdContent(java.getMarkdownContent());
    }
    if (java.getHtmlContent() != null) {
      builder.setHtmlContent(java.getHtmlContent());
    }
    if (java.getTextContent() != null) {
      builder.setTextContent(java.getTextContent());
    }
    if (java.getDoctagsContent() != null) {
      builder.setDoctagsContent(java.getDoctagsContent());
    }

    return builder.build();
  }

  private static ai.docling.serve.v1.ErrorItem toProtoErrorItem(ErrorItem java) {
    ai.docling.serve.v1.ErrorItem.Builder builder =
        ai.docling.serve.v1.ErrorItem.newBuilder();
    if (java.getComponentType() != null) {
      builder.setComponentType(java.getComponentType());
    }
    if (java.getErrorMessage() != null) {
      builder.setErrorMessage(java.getErrorMessage());
    }
    if (java.getModuleName() != null) {
      builder.setModuleName(java.getModuleName());
    }
    return builder.build();
  }

  private static ai.docling.serve.v1.Chunk toProtoChunk(
      ai.docling.serve.api.chunk.response.Chunk java) {
    var builder = ai.docling.serve.v1.Chunk.newBuilder();

    if (java.getFilename() != null) {
      builder.setFilename(java.getFilename());
    }
    builder.setChunkIndex(java.getChunkIndex());
    if (java.getText() != null) {
      builder.setText(java.getText());
    }
    if (java.getRawText() != null) {
      builder.setRawText(java.getRawText());
    }
    if (java.getNumTokens() != null) {
      builder.setNumTokens(java.getNumTokens());
    }
    if (java.getHeadings() != null) {
      builder.addAllHeadings(java.getHeadings());
    }
    if (java.getCaptions() != null) {
      builder.addAllCaptions(java.getCaptions());
    }
    if (java.getDocItems() != null) {
      builder.addAllDocItems(java.getDocItems());
    }
    if (java.getPageNumbers() != null) {
      builder.addAllPageNumbers(java.getPageNumbers());
    }
    if (java.getMetadata() != null) {
      java.getMetadata().forEach((key, value) ->
          builder.putMetadata(key, String.valueOf(value))
      );
    }

    return builder.build();
  }

  private static ai.docling.serve.v1.Document toProtoDocument(Document java) {
    var builder = ai.docling.serve.v1.Document.newBuilder();

    if (java.getKind() != null) {
      builder.setKind(java.getKind());
    }
    if (java.getContent() != null) {
      builder.setContent(toProtoExportDocumentResponse(java.getContent()));
    }
    if (java.getStatus() != null) {
      builder.setStatus(java.getStatus());
    }
    if (java.getErrors() != null) {
      java.getErrors().forEach(error -> builder.addErrors(toProtoErrorItem(error)));
    }

    return builder.build();
  }

  private static ai.docling.serve.v1.ExportDocumentResponse toProtoExportDocumentResponse(
      ExportDocumentResponse java) {
    var builder = ai.docling.serve.v1.ExportDocumentResponse.newBuilder();

    if (java.getFilename() != null) {
      builder.setFilename(java.getFilename());
    }
    if (java.getJsonContent() != null) {
      builder.setJsonContent(ai.docling.serve.grpc.v1.mapping.DoclingDocumentMapper.map(java.getJsonContent()));
    }
    if (java.getMarkdownContent() != null) {
      builder.setMdContent(java.getMarkdownContent());
    }
    if (java.getHtmlContent() != null) {
      builder.setHtmlContent(java.getHtmlContent());
    }
    if (java.getTextContent() != null) {
      builder.setTextContent(java.getTextContent());
    }
    if (java.getDoctagsContent() != null) {
      builder.setDoctagsContent(java.getDoctagsContent());
    }

    return builder.build();
  }

  private static ai.docling.serve.v1.TaskStatus toProtoTaskStatus(TaskStatus java) {
    if (java == null) {
      return ai.docling.serve.v1.TaskStatus.TASK_STATUS_UNSPECIFIED;
    }
    return switch (java) {
      case PENDING -> ai.docling.serve.v1.TaskStatus.TASK_STATUS_PENDING;
      case STARTED -> ai.docling.serve.v1.TaskStatus.TASK_STATUS_STARTED;
      case SUCCESS -> ai.docling.serve.v1.TaskStatus.TASK_STATUS_SUCCESS;
      case FAILURE -> ai.docling.serve.v1.TaskStatus.TASK_STATUS_FAILURE;
    };
  }

  private static ai.docling.serve.v1.TaskStatusMetadata toProtoTaskStatusMetadata(
      TaskStatusMetadata java) {
    ai.docling.serve.v1.TaskStatusMetadata.Builder builder =
        ai.docling.serve.v1.TaskStatusMetadata.newBuilder();
    builder.setNumDocs(java.getNumDocs());
    builder.setNumProcessed(java.getNumProcessed());
    builder.setNumSucceeded(java.getNumSucceeded());
    builder.setNumFailed(java.getNumFailed());
    return builder.build();
  }

  // ==================== Enum Mapping: Proto → Java ====================

  private static InputFormat toJavaInputFormat(ai.docling.serve.v1.InputFormat proto) {
    return switch (proto) {
      case INPUT_FORMAT_ASCIIDOC -> InputFormat.ASCIIDOC;
      case INPUT_FORMAT_AUDIO -> InputFormat.AUDIO;
      case INPUT_FORMAT_CSV -> InputFormat.CSV;
      case INPUT_FORMAT_DOCX -> InputFormat.DOCX;
      case INPUT_FORMAT_HTML -> InputFormat.HTML;
      case INPUT_FORMAT_IMAGE -> InputFormat.IMAGE;
      case INPUT_FORMAT_JSON_DOCLING -> InputFormat.JSON_DOCLING;
      case INPUT_FORMAT_MD -> InputFormat.MARKDOWN;
      case INPUT_FORMAT_METS_GBS -> InputFormat.METS_GBS;
      case INPUT_FORMAT_PDF -> InputFormat.PDF;
      case INPUT_FORMAT_PPTX -> InputFormat.PPTX;
      case INPUT_FORMAT_XLSX -> InputFormat.XLSX;
      case INPUT_FORMAT_XML_JATS -> InputFormat.XML_JATS;
      case INPUT_FORMAT_XML_USPTO -> InputFormat.XML_USPTO;
      default -> null;
    };
  }

  private static OutputFormat toJavaOutputFormat(ai.docling.serve.v1.OutputFormat proto) {
    return switch (proto) {
      case OUTPUT_FORMAT_DOCTAGS -> OutputFormat.DOCTAGS;
      case OUTPUT_FORMAT_HTML -> OutputFormat.HTML;
      case OUTPUT_FORMAT_HTML_SPLIT_PAGE -> OutputFormat.HTML_SPLIT_PAGE;
      case OUTPUT_FORMAT_JSON -> OutputFormat.JSON;
      case OUTPUT_FORMAT_MD -> OutputFormat.MARKDOWN;
      case OUTPUT_FORMAT_TEXT -> OutputFormat.TEXT;
      default -> null;
    };
  }

  private static ImageRefMode toJavaImageRefMode(ai.docling.serve.v1.ImageRefMode proto) {
    return switch (proto) {
      case IMAGE_REF_MODE_EMBEDDED -> ImageRefMode.EMBEDDED;
      case IMAGE_REF_MODE_PLACEHOLDER -> ImageRefMode.PLACEHOLDER;
      case IMAGE_REF_MODE_REFERENCED -> ImageRefMode.REFERENCED;
      default -> null;
    };
  }

  private static OcrEngine toJavaOcrEngine(ai.docling.serve.v1.OcrEngine proto) {
    return switch (proto) {
      case OCR_ENGINE_AUTO -> OcrEngine.AUTO;
      case OCR_ENGINE_EASYOCR -> OcrEngine.EASYOCR;
      case OCR_ENGINE_OCRMAC -> OcrEngine.OCRMAC;
      case OCR_ENGINE_RAPIDOCR -> OcrEngine.RAPIDOCR;
      case OCR_ENGINE_TESSEROCR -> OcrEngine.TESSEROCR;
      case OCR_ENGINE_TESSERACT -> OcrEngine.TESSERACT;
      default -> null;
    };
  }

  private static PdfBackend toJavaPdfBackend(ai.docling.serve.v1.PdfBackend proto) {
    return switch (proto) {
      case PDF_BACKEND_DLPARSE_V1 -> PdfBackend.DLPARSE_V1;
      case PDF_BACKEND_DLPARSE_V2 -> PdfBackend.DLPARSE_V2;
      case PDF_BACKEND_DLPARSE_V4 -> PdfBackend.DLPARSE_V4;
      case PDF_BACKEND_PYPDFIUM2 -> PdfBackend.PYPDFIUM2;
      default -> null;
    };
  }

  private static TableFormerMode toJavaTableFormerMode(ai.docling.serve.v1.TableFormerMode proto) {
    return switch (proto) {
      case TABLE_FORMER_MODE_ACCURATE -> TableFormerMode.ACCURATE;
      case TABLE_FORMER_MODE_FAST -> TableFormerMode.FAST;
      default -> null;
    };
  }

  private static ProcessingPipeline toJavaPipeline(ai.docling.serve.v1.ProcessingPipeline proto) {
    return switch (proto) {
      case PROCESSING_PIPELINE_ASR -> ProcessingPipeline.ASR;
      case PROCESSING_PIPELINE_STANDARD -> ProcessingPipeline.STANDARD;
      case PROCESSING_PIPELINE_VLM -> ProcessingPipeline.VLM;
      default -> null;
    };
  }

  private static VlmModelType toJavaVlmModelType(ai.docling.serve.v1.VlmModelType proto) {
    return switch (proto) {
      case VLM_MODEL_TYPE_SMOLDOCLING -> VlmModelType.SMOLDOCLING;
      case VLM_MODEL_TYPE_SMOLDOCLING_VLLM -> VlmModelType.SMOLDOCLING_VLLM;
      case VLM_MODEL_TYPE_GRANITE_VISION -> VlmModelType.GRANITE_VISION;
      case VLM_MODEL_TYPE_GRANITE_VISION_VLLM -> VlmModelType.GRANITE_VISION_VLLM;
      case VLM_MODEL_TYPE_GRANITE_VISION_OLLAMA -> VlmModelType.GRANITE_VISION_OLLAMA;
      case VLM_MODEL_TYPE_GOT_OCR_2 -> VlmModelType.GOT_OCR_2;
      default -> null;
    };
  }
}