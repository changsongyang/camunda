/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.service;

import io.camunda.search.clients.UserSearchClient;
import io.camunda.search.entities.UserEntity;
import io.camunda.search.query.SearchQueryResult;
import io.camunda.search.query.UserQuery;
import io.camunda.security.auth.Authentication;
import io.camunda.security.auth.Authorization;
import io.camunda.service.search.core.SearchQueryService;
import io.camunda.service.security.SecurityContextProvider;
import io.camunda.zeebe.broker.client.api.BrokerClient;
import io.camunda.zeebe.gateway.impl.broker.request.BrokerUserCreateRequest;
import io.camunda.zeebe.gateway.impl.broker.request.BrokerUserDeleteRequest;
import io.camunda.zeebe.gateway.impl.broker.request.BrokerUserUpdateRequest;
import io.camunda.zeebe.protocol.impl.record.value.user.UserRecord;
import java.util.concurrent.CompletableFuture;

public class UserServices extends SearchQueryService<UserServices, UserQuery, UserEntity> {

  private final UserSearchClient userSearchClient;

  public UserServices(
      final BrokerClient brokerClient,
      final SecurityContextProvider securityContextProvider,
      final UserSearchClient userSearchClient,
      final Authentication authentication) {
    super(brokerClient, securityContextProvider, authentication);
    this.userSearchClient = userSearchClient;
  }

  @Override
  public SearchQueryResult<UserEntity> search(final UserQuery query) {
    return userSearchClient
        .withSecurityContext(
            securityContextProvider.provideSecurityContext(
                authentication, Authorization.of(a -> a.user().read())))
        .searchUsers(query);
  }

  @Override
  public UserServices withAuthentication(final Authentication authentication) {
    return new UserServices(
        brokerClient, securityContextProvider, userSearchClient, authentication);
  }

  public CompletableFuture<UserRecord> createUser(final UserDTO request) {
    return sendBrokerRequest(
        new BrokerUserCreateRequest()
            .setUsername(request.username())
            .setName(request.name())
            .setEmail(request.email())
            .setPassword(request.password()));
  }

  public CompletableFuture<UserRecord> updateUser(final UserDTO request) {
    return sendBrokerRequest(
        new BrokerUserUpdateRequest()
            .setUserKey(request.userKey())
            .setUsername(request.username())
            .setName(request.name())
            .setEmail(request.email())
            .setPassword(request.password()));
  }

  public CompletableFuture<UserRecord> updateUser(final long userKey, final String name) {
    return updateUser(new UserDTO(userKey, null, name, null, null));
  }

  public CompletableFuture<UserRecord> deleteUser(final long userKey) {
    return sendBrokerRequest(new BrokerUserDeleteRequest().setUserKey(userKey).setUsername(""));
  }

  public record UserDTO(
      Long userKey, String username, String name, String email, String password) {}
}
