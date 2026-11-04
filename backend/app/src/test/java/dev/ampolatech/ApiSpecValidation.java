package dev.ampolatech;

import com.atlassian.oai.validator.OpenApiInteractionValidator;
import com.atlassian.oai.validator.springweb.client.OpenApiValidationClientHttpRequestInterceptor;
import org.springframework.boot.resttestclient.autoconfigure.RestTestClientBuilderCustomizer;
import org.springframework.test.web.servlet.client.RestTestClient;

/**
 * Checks every exchange of the tests' {@link RestTestClient} against the API's spec, {@code
 * api/openapi.yaml}, which the build puts on the test classpath. A request or a response that
 * drifts from the spec, in its status, content type or body, fails the test that made it.
 */
public class ApiSpecValidation implements RestTestClientBuilderCustomizer {

  @Override
  public void customize(RestTestClient.Builder<?> builder) {
    OpenApiInteractionValidator validator =
        OpenApiInteractionValidator.createForSpecificationUrl("openapi.yaml").build();
    builder.requestInterceptor(new OpenApiValidationClientHttpRequestInterceptor(validator));
  }
}
