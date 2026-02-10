module ai.docling.serve.grpc {
  requires ai.docling.serve.api;
  requires io.grpc;
  requires io.grpc.stub;
  requires io.grpc.protobuf;
  requires com.google.protobuf;
  requires org.slf4j;
  requires java.annotation;
  requires java.net.http;

  requires static lombok;
  requires static org.jspecify;
  requires tools.jackson.databind;
  requires static com.google.errorprone.annotations;

  exports ai.docling.serve.grpc.v1;
  exports ai.docling.serve.grpc.v1.mapping;
}
