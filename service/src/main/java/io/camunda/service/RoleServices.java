/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.service;

import io.camunda.search.clients.RoleSearchClient;
import io.camunda.search.entities.RoleEntity;
import io.camunda.search.exception.NotFoundException;
import io.camunda.search.query.RoleQuery;
import io.camunda.search.query.SearchQueryBuilders;
import io.camunda.search.query.SearchQueryResult;
import io.camunda.security.auth.Authentication;
import io.camunda.security.auth.Authorization;
import io.camunda.service.search.core.SearchQueryService;
import io.camunda.service.security.SecurityContextProvider;
import io.camunda.zeebe.broker.client.api.BrokerClient;
import io.camunda.zeebe.gateway.impl.broker.request.BrokerRoleEntityRequest;
import io.camunda.zeebe.gateway.impl.broker.request.BrokerRoleUpdateRequest;
import io.camunda.zeebe.gateway.impl.broker.request.role.BrokerRoleCreateRequest;
import io.camunda.zeebe.protocol.impl.record.value.authorization.RoleRecord;
import io.camunda.zeebe.protocol.record.value.EntityType;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class RoleServices extends SearchQueryService<RoleServices, RoleQuery, RoleEntity> {

  private final RoleSearchClient roleSearchClient;

  public RoleServices(
      final BrokerClient brokerClient,
      final SecurityContextProvider securityContextProvider,
      final RoleSearchClient roleSearchClient,
      final Authentication authentication) {
    super(brokerClient, securityContextProvider, authentication);
    this.roleSearchClient = roleSearchClient;
  }

  @Override
  public SearchQueryResult<RoleEntity> search(final RoleQuery query) {
    return roleSearchClient
        .withSecurityContext(
            securityContextProvider.provideSecurityContext(
                authentication, Authorization.of(a -> a.role().read())))
        .searchRoles(query);
  }

  @Override
  public RoleServices withAuthentication(final Authentication authentication) {
    return new RoleServices(
        brokerClient, securityContextProvider, roleSearchClient, authentication);
  }

  public CompletableFuture<RoleRecord> createRole(final RoleDTO request) {
    return sendBrokerRequest(new BrokerRoleCreateRequest().setName(request.name()));
  }

  public CompletableFuture<RoleRecord> updateRole(final RoleDTO request) {
    return sendBrokerRequest(
        new BrokerRoleUpdateRequest(request.roleKey()).setName(request.name()));
  }

  public RoleEntity getByRoleKey(final Long roleKey) {
    final SearchQueryResult<RoleEntity> result =
        search(SearchQueryBuilders.roleSearchQuery().filter(f -> f.roleKey(roleKey)).build());
    if (result.total() < 1) {
      throw new NotFoundException(String.format("Role with roleKey %d not found", roleKey));
    } else {
      return result.items().stream().findFirst().orElseThrow();
    }
  }

  public void addMember(final Long roleKey, final EntityType entityType, final long entityKey) {
    sendBrokerRequest(
        BrokerRoleEntityRequest.createAddRequest()
            .setRoleKey(roleKey)
            .setEntity(entityType, entityKey));
  }

  public void removeMember(final Long roleKey, final EntityType entityType, final long entityKey) {
    sendBrokerRequest(
        BrokerRoleEntityRequest.createRemoveRequest()
            .setRoleKey(roleKey)
            .setEntity(entityType, entityKey));
  }

  public record RoleDTO(long roleKey, String name, Set<Long> assignedMemberKeys) {}
}
