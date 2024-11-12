/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.camunda.search.clients.ProcessInstanceSearchClient;
import io.camunda.search.entities.DecisionRequirementsEntity;
import io.camunda.search.entities.ProcessInstanceEntity;
import io.camunda.search.exception.CamundaSearchException;
import io.camunda.search.exception.NotFoundException;
import io.camunda.search.query.ProcessInstanceQuery;
import io.camunda.search.query.SearchQueryBuilders;
import io.camunda.search.query.SearchQueryResult;
import io.camunda.security.auth.Authentication;
import io.camunda.security.auth.SecurityContext;
import io.camunda.security.configuration.SecurityConfiguration;
import io.camunda.security.impl.AuthorizationChecker;
import io.camunda.service.security.SecurityContextProvider;
import io.camunda.zeebe.broker.client.api.BrokerClient;
import io.camunda.zeebe.protocol.record.value.AuthorizationResourceType;
import io.camunda.zeebe.protocol.record.value.PermissionType;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

public final class ProcessInstanceServiceTest {

  private ProcessInstanceServices services;
  private ProcessInstanceSearchClient client;
  private SecurityConfiguration securityConfiguration;

  @BeforeEach
  public void before() {
    client = mock(ProcessInstanceSearchClient.class);
    when(client.withSecurityContext(any())).thenReturn(client);
    securityConfiguration = new SecurityConfiguration();
    final var securityContextProvider =
        new SecurityContextProvider(securityConfiguration, mock(AuthorizationChecker.class));
    services =
        new ProcessInstanceServices(
            mock(BrokerClient.class), securityContextProvider, client, null);
  }

  @Test
  public void shouldReturnProcessInstance() {
    // given
    final var result = mock(SearchQueryResult.class);
    when(client.searchProcessInstances(any())).thenReturn(result);

    final ProcessInstanceQuery searchQuery =
        SearchQueryBuilders.processInstanceSearchQuery().build();

    // when
    final SearchQueryResult<ProcessInstanceEntity> searchQueryResult = services.search(searchQuery);

    // then
    assertThat(searchQueryResult).isEqualTo(result);
  }

  @Test
  public void shouldReturnProcessInstanceByKey() {
    // given
    final var key = 123L;
    final var entity = mock(ProcessInstanceEntity.class);
    when(entity.key()).thenReturn(key);
    when(client.searchProcessInstances(any()))
        .thenReturn(new SearchQueryResult(1, List.of(entity), null));

    // when
    final var searchQueryResult = services.getByKey(key);

    // then
    assertThat(searchQueryResult.key()).isEqualTo(key);
  }

  @Test
  public void shouldThrownExceptionIfNotFoundByKey() {
    // given
    final var key = 100L;
    when(client.searchProcessInstances(any()))
        .thenReturn(new SearchQueryResult(0, List.of(), null));

    // when / then
    final var exception =
        assertThrowsExactly(NotFoundException.class, () -> services.getByKey(key));
    assertThat(exception.getMessage()).isEqualTo("Process Instance with key 100 not found");
  }

  @Test
  public void shouldThrownExceptionIfDuplicateFoundByKey() {
    // given
    final var key = 200L;
    final var entity1 = mock(DecisionRequirementsEntity.class);
    final var entity2 = mock(DecisionRequirementsEntity.class);
    when(client.searchProcessInstances(any()))
        .thenReturn(new SearchQueryResult(2, List.of(entity1, entity2), null));

    // when / then
    final var exception =
        assertThrowsExactly(CamundaSearchException.class, () -> services.getByKey(key));
    assertThat(exception.getMessage())
        .isEqualTo("Found Process Instance with key 200 more than once");
  }

  @Test
  public void shouldAddAuthenticationWhenEnabled() {
    // given
    securityConfiguration.getAuthorizations().setEnabled(true);
    final var authentication = mock(Authentication.class);
    final ProcessInstanceQuery searchQuery =
        SearchQueryBuilders.processInstanceSearchQuery().build();

    // when
    services.withAuthentication(authentication).search(searchQuery);

    // then
    final ArgumentCaptor<SecurityContext> securityContextArgumentCaptor =
        ArgumentCaptor.forClass(SecurityContext.class);
    verify(client).withSecurityContext(securityContextArgumentCaptor.capture());
    assertThat(securityContextArgumentCaptor.getValue())
        .isEqualTo(
            SecurityContext.of(
                s ->
                    s.withAuthentication(authentication)
                        .withAuthorization(
                            a ->
                                a.permissionType(PermissionType.READ_PROCESS_INSTANCE)
                                    .resourceType(AuthorizationResourceType.PROCESS_DEFINITION))));
  }

  @Test
  public void shouldNotAddAuthenticationWhenDisabled() {
    // given
    securityConfiguration.getAuthorizations().setEnabled(false);
    final var authentication = mock(Authentication.class);
    final ProcessInstanceQuery searchQuery =
        SearchQueryBuilders.processInstanceSearchQuery().build();

    // when
    services.withAuthentication(authentication).search(searchQuery);

    // then
    final ArgumentCaptor<SecurityContext> securityContextArgumentCaptor =
        ArgumentCaptor.forClass(SecurityContext.class);
    verify(client).withSecurityContext(securityContextArgumentCaptor.capture());
    assertThat(securityContextArgumentCaptor.getValue())
        .isEqualTo(SecurityContext.of(s -> s.withAuthentication(authentication)));
  }
}
