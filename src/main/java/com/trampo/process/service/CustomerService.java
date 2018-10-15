package com.trampo.process.service;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trampo.process.domain.Customer;
import com.trampo.process.exception.RestException;
import com.trampo.process.util.MyResponseErrorHandler;

@Component
public class CustomerService {
  
  private static final Logger LOGGER = LoggerFactory.getLogger(CustomerService.class);

  private RestTemplate restTemplate;
  private ObjectMapper mapper;
  
  @Autowired
  public CustomerService(RestTemplateBuilder builder, @Value("${webapp.api.root}") String apiRoot){
    restTemplate = builder.rootUri(apiRoot).build();
    MyResponseErrorHandler errorHandler = new MyResponseErrorHandler();
    restTemplate.setErrorHandler(errorHandler);
    mapper = new ObjectMapper();
    mapper.findAndRegisterModules();
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
  }
  
  public String getCustomerEmail(long customerId) throws RestException, IOException {
    HttpHeaders headers = new HttpHeaders();
    headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
    headers.setContentType(MediaType.APPLICATION_JSON);
    Map<String, String> request = new HashMap<>();
    HttpEntity<String> postEntity =
        new HttpEntity<String>(mapper.writeValueAsString(request), headers);
    ResponseEntity<String> response = restTemplate.exchange("/customers/email/" + customerId,
        HttpMethod.GET, postEntity, String.class);
    checkError(response);
    return response.getBody();
  }
  
  public Customer getCustomer(long customerId) throws RestException, IOException {
    HttpHeaders headers = new HttpHeaders();
    headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
    headers.setContentType(MediaType.APPLICATION_JSON);
    Map<String, String> request = new HashMap<>();
    HttpEntity<String> postEntity =
        new HttpEntity<String>(mapper.writeValueAsString(request), headers);
    ResponseEntity<String> response = restTemplate.exchange("/customers/" + customerId,
        HttpMethod.GET, postEntity, String.class);
    checkError(response);
    return mapper.readValue(response.getBody(), Customer.class);
  }
  
  private void checkError(ResponseEntity<String> response) throws RestException {
    if (!response.getStatusCode().is2xxSuccessful()) {
      LOGGER.error("Rest Exception: Status Code: " + response.getStatusCode() + " body: "
          + response.getBody());
      throw new RestException();
    }
  }
}
