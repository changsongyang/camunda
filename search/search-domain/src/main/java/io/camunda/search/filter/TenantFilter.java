/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.search.filter;

import io.camunda.util.ObjectBuilder;

public record TenantFilter(Long tenantKey, String tenantId, String name) implements FilterBase {

  public static final class Builder implements ObjectBuilder<TenantFilter> {
    private Long tenantKey;
    private String tenantId;
    private String name;

    public Builder tenantKey(final Long value) {
      tenantKey = value;
      return this;
    }

    public Builder tenantId(final String value) {
      tenantId = value;
      return this;
    }

    public Builder name(final String value) {
      name = value;
      return this;
    }

    @Override
    public TenantFilter build() {
      return new TenantFilter(tenantKey, tenantId, name);
    }
  }
}
