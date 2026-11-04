package dev.ampolatech.adapter.in.web.rest;

import dev.ampolatech.adapter.in.web.rest.model.ProblemDto;
import java.net.URI;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Writes every problem-details body (RFC 9457) that Spring MVC answers as the spec's {@code
 * Problem}. The generated interfaces type only the success response, so the HTTP-seam tests hold
 * these bodies to the spec (ADR 0006).
 */
@RestControllerAdvice
class ProblemDetailsAdvice extends ResponseEntityExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(ProblemDetailsAdvice.class);

  /**
   * Answers an exception nothing else handles with a 500 that carries no detail: the exception's
   * message can name a user's or a pharmacy's id.
   */
  @ExceptionHandler(Exception.class)
  @Nullable ResponseEntity<Object> handleUnexpected(Exception exception, WebRequest request) {
    log.error("Unexpected error, answered with a 500", exception);
    return handleExceptionInternal(
        exception,
        ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR),
        new HttpHeaders(),
        HttpStatus.INTERNAL_SERVER_ERROR,
        request);
  }

  @Override
  protected ResponseEntity<Object> createResponseEntity(
      @Nullable Object body, HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {
    if (!(body instanceof ProblemDetail problemDetail)) {
      throw new IllegalStateException("Expected a ProblemDetail body, got " + body);
    }

    var problem =
        new ProblemDto(
            uriReference(problemDetail.getType()),
            problemDetail.getTitle(),
            problemDetail.getStatus(),
            problemDetail.getDetail(),
            uriReference(problemDetail.getInstance()));
    return ResponseEntity.status(statusCode)
        .headers(headers)
        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
        .body(problem);
  }

  private static @Nullable String uriReference(@Nullable URI uri) {
    return uri == null ? null : uri.toString();
  }
}
