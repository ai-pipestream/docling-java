import com.google.protobuf.gradle.id

plugins {
  id("docling-java-shared")
  id("docling-lombok")
  id("docling-release")
  id("com.google.protobuf") version "0.9.4"
}

description = "Docling Serve gRPC API"

val grpcVersion = "1.72.0"
val protocVersion = "4.29.3"
val javaxAnnotationVersion = "1.3.2"

dependencies {
  api(project(":docling-serve-api"))
  api("io.grpc:grpc-stub:$grpcVersion")
  api("io.grpc:grpc-protobuf:$grpcVersion")
  api("com.google.protobuf:protobuf-java:$protocVersion")
  api(libs.slf4j.api)
  compileOnly("javax.annotation:javax.annotation-api:$javaxAnnotationVersion")
  api(platform(libs.jackson.bom))
  api(libs.jackson.databind)

  testImplementation("org.mockito:mockito-core:5.17.0")
  testImplementation("org.mockito:mockito-junit-jupiter:5.17.0")
  testImplementation("io.grpc:grpc-testing:$grpcVersion")
  testImplementation("io.grpc:grpc-inprocess:$grpcVersion")
  testRuntimeOnly(libs.slf4j.simple)

  // Integration test dependencies
  testImplementation(platform(libs.testcontainers.bom))
  testImplementation(libs.testcontainers.junit.jupiter)
  testImplementation(project(":docling-testcontainers"))
  testImplementation(project(":docling-serve-client"))
  testImplementation(platform(libs.jackson2.bom))
  testImplementation(libs.jackson2.databind)
}

protobuf {
  protoc {
    artifact = "com.google.protobuf:protoc:$protocVersion"
  }

  plugins {
    id("grpc") {
      artifact = "io.grpc:protoc-gen-grpc-java:$grpcVersion"
    }
  }

  generateProtoTasks {
    all().forEach { task ->
      task.plugins {
        id("grpc")
      }
    }
  }
}
