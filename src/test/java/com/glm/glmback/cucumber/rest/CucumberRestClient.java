package com.glm.glmback.cucumber.rest;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA;

import java.util.Map;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

public class CucumberRestClient {

  private RestTestClient restClient;

  public CucumberRestClient(RestTestClient restClient) {
    this.restClient = restClient;
  }

  public void get(String uri) {
    restClient.get().uri(uri).exchange();
  }

  public void get(String uri, Map<String, ?> variables) {
    restClient.get().uri(uri, variables).exchange();
  }

  public void post(String uri, String content) {
    restClient.post().uri(uri).accept(APPLICATION_JSON).contentType(APPLICATION_JSON).body(content).exchange();
  }

  public void put(String uri, String content) {
    restClient.put().uri(uri).accept(APPLICATION_JSON).contentType(APPLICATION_JSON).body(content).exchange();
  }

  public void putFile(String uri, String part, byte[] content) {
    MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
    body.add(
      part,
      new ByteArrayResource(content) {
        @Override
        public String getFilename() {
          return part;
        }
      }
    );
    restClient.put().uri(uri).accept(APPLICATION_JSON).contentType(MULTIPART_FORM_DATA).body(body).exchange();
  }

  public void delete(String uri) {
    restClient.delete().uri(uri).exchange();
  }

  public void addRequestInterceptor(ClientHttpRequestInterceptor interceptor) {
    this.restClient = this.restClient.mutate().requestInterceptor(interceptor).build();
  }

  public void setupRestClient() {
    this.restClient = this.restClient.mutate()
      .requestInterceptors(interceptors -> interceptors.add(saveLastResultInterceptor()))
      .build();
  }

  private ClientHttpRequestInterceptor saveLastResultInterceptor() {
    return (request, body, execution) -> {
      ClientHttpResponse response = execution.execute(request, body);

      CucumberRestTestContext.addResponse(request, response, execution, body);

      return response;
    };
  }
}
