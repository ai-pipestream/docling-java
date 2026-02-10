package ai.docling.serve.grpc.v1;

import ai.docling.serve.api.chunk.request.HierarchicalChunkDocumentRequest;
import ai.docling.serve.api.chunk.request.HybridChunkDocumentRequest;
import ai.docling.serve.api.convert.request.ConvertDocumentRequest;
import ai.docling.serve.api.task.response.TaskStatusPollResponse;

interface AsyncTaskSubmitter {
  TaskStatusPollResponse submitConvertSource(ConvertDocumentRequest request);

  TaskStatusPollResponse submitChunkHierarchicalSource(HierarchicalChunkDocumentRequest request);

  TaskStatusPollResponse submitChunkHybridSource(HybridChunkDocumentRequest request);
}
