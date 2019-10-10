package org.flexiblepower.raml.example.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum Gender {
  @JsonProperty("male")
  MALE("male"),

  @JsonProperty("female")
  FEMALE("female"),

  @JsonProperty("other")
  OTHER("other");

  private String name;

  Gender(String name) {
    this.name = name;
  }
}
