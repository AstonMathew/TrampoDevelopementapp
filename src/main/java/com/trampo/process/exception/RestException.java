package com.trampo.process.exception;

public class RestException extends Exception {

  private static final long serialVersionUID = 4233651016945333460L;

  public RestException() {
    super("Rest error occured!");
  }
}
