/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.operate.webapp.security.permission;

import io.camunda.authentication.entity.CamundaUser;
import io.camunda.operate.property.OperateProperties;
import io.camunda.operate.webapp.security.identity.IdentityPermission;
import io.camunda.security.auth.Authorization;
import io.camunda.security.auth.SecurityContext;
import io.camunda.security.impl.AuthorizationChecker;
import io.camunda.zeebe.gateway.rest.TenantAttributeHolder;
import io.camunda.zeebe.protocol.record.value.AuthorizationResourceType;
import io.camunda.zeebe.protocol.record.value.PermissionType;
import jakarta.annotation.PostConstruct;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class PermissionsService {

  private static final Logger LOGGER = LoggerFactory.getLogger(PermissionsService.class);

  private final OperateProperties operateProperties;
  private final AuthorizationChecker authorizationChecker;

  public PermissionsService(
      final OperateProperties operateProperties, final AuthorizationChecker authorizationChecker) {
    this.operateProperties = operateProperties;
    this.authorizationChecker = authorizationChecker;
  }

  @PostConstruct
  public void logCreated() {
    LOGGER.debug("PermissionsService bean created.");
  }

  /**
   * getProcessDefinitionPermissions
   *
   * @param bpmnProcessId bpmnProcessId
   * @return permissions the user has for the given bpmnProcessId
   */
  public Set<String> getProcessDefinitionPermissions(final String bpmnProcessId) {
    return getResourcePermissions(bpmnProcessId, AuthorizationResourceType.PROCESS_DEFINITION);
  }

  /**
   * getDecisionDefinitionPermissions
   *
   * @param decisionId decisionId
   * @return permissions the user has for the given decisionId
   */
  public Set<String> getDecisionDefinitionPermissions(final String decisionId) {
    return getResourcePermissions(decisionId, AuthorizationResourceType.DECISION_DEFINITION);
  }

  /**
   * getResourcePermissions
   *
   * @param resourceId resourceId
   * @param resourceType resourceType
   * @return permissions the user has for the given resource
   */
  public Set<String> getResourcePermissions(
      final String resourceId, final AuthorizationResourceType resourceType) {
    final Set<String> permissions = new HashSet<>();
    if (permissionsEnabled() && isAuthenticated()) {
      for (PermissionType permissionType : PermissionType.values()) {
        if (isAuthorized(resourceId, resourceType, permissionType)) {
          permissions.add(permissionType.name());
        }
      }
    }
    return permissions;
  }

  /**
   * hasPermissionForProcess
   *
   * @return true if the user has the given permission for the process
   */
  public boolean hasPermissionForProcess(
      final String bpmnProcessId, final IdentityPermission identityPermission) {
    return hasPermissionForResource(
        bpmnProcessId, AuthorizationResourceType.PROCESS_DEFINITION, identityPermission);
  }

  /**
   * hasPermissionForDecision
   *
   * @return true if the user has the given permission for the decision
   */
  public boolean hasPermissionForDecision(
      final String decisionId, final IdentityPermission identityPermission) {
    return hasPermissionForResource(
        decisionId, AuthorizationResourceType.DECISION_DEFINITION, identityPermission);
  }

  /**
   * hasPermissionForResource
   *
   * @return true if the user has the given permission for the resource
   */
  public boolean hasPermissionForResource(
      final String resourceId,
      final AuthorizationResourceType resourceType,
      final IdentityPermission identityPermission) {
    if (!permissionsEnabled()) {
      return true;
    }
    if (!isAuthenticated()) {
      return false;
    }
    if (identityPermission == null) {
      throw new IllegalStateException("Identity permission cannot be null");
    }
    final PermissionType permissionType = getPermission(identityPermission);

    return isAuthorized(resourceId, resourceType, permissionType);
  }

  /**
   * getProcessesWithPermission
   *
   * @return processes for which the user has the given permission; the result matches either all
   *     processes, or a list of bpmnProcessId
   */
  public ResourcesAllowed getProcessesWithPermission(final IdentityPermission identityPermission) {
    return getResourcesWithPermission(
        AuthorizationResourceType.PROCESS_DEFINITION, identityPermission);
  }

  /**
   * getDecisionsWithPermission
   *
   * @return decisions for which the user has the given permission; the result matches either all
   *     decisions, or a list of decisionId
   */
  public ResourcesAllowed getDecisionsWithPermission(final IdentityPermission identityPermission) {
    return getResourcesWithPermission(
        AuthorizationResourceType.DECISION_DEFINITION, identityPermission);
  }

  /**
   * getResourcesWithPermission
   *
   * @return resources for which the user has the given permission; the result matches either all
   *     resources, or a list of resourceIds
   */
  public ResourcesAllowed getResourcesWithPermission(
      final AuthorizationResourceType resourceType, final IdentityPermission identityPermission) {
    if (!permissionsEnabled()) {
      return ResourcesAllowed.all();
    }
    if (!isAuthenticated()) {
      return ResourcesAllowed.withIds(Set.of());
    }
    if (identityPermission == null) {
      throw new IllegalStateException("Identity permission cannot be null");
    }
    final PermissionType permissionType = getPermission(identityPermission);
    final Authorization authorization = new Authorization(resourceType, permissionType);
    final SecurityContext securityContext = getSecurityContext(authorization);
    final List<String> ids = authorizationChecker.retrieveAuthorizedResourceKeys(securityContext);

    return ResourcesAllowed.withIds(new LinkedHashSet<>(ids));
  }

  private boolean isAuthorized(
      String resourceId, AuthorizationResourceType resourceType, PermissionType permissionType) {
    final Authorization authorization = new Authorization(resourceType, permissionType);
    final SecurityContext securityContext = getSecurityContext(authorization);
    return authorizationChecker.isAuthorized(resourceId, securityContext);
  }

  private SecurityContext getSecurityContext(Authorization authorization) {
    return new SecurityContext(getAuthentication(), authorization);
  }

  private io.camunda.security.auth.Authentication getAuthentication() {
    final Long authenticatedUserKey = getAuthenticatedUserKey();
    final List<String> authorizedTenants = TenantAttributeHolder.tenantIds();
    // groups and roles will come later
    return new io.camunda.security.auth.Authentication.Builder()
        .user(authenticatedUserKey)
        .tenants(authorizedTenants)
        .build();
  }

  private Long getAuthenticatedUserKey() {
    final Authentication requestAuthentication =
        SecurityContextHolder.getContext().getAuthentication();
    if (requestAuthentication != null
        && requestAuthentication.getPrincipal()
            instanceof final CamundaUser authenticatedPrincipal) {
      return authenticatedPrincipal.getUserKey();
    }
    return null;
  }

  private boolean isAuthenticated() {
    return (getAuthenticatedUserKey() != null);
  }

  private boolean permissionsEnabled() {
    return operateProperties.getIdentity().isResourcePermissionsEnabled();
  }

  // FIX
  private PermissionType getPermission(IdentityPermission permission) {
    return null;
  }

  /** ResourcesAllowed */
  public static final class ResourcesAllowed {
    private final boolean all;
    private final Set<String> ids;

    private ResourcesAllowed(final boolean all, final Set<String> ids) {
      this.all = all;
      this.ids = ids;
    }

    public static ResourcesAllowed all() {
      return new ResourcesAllowed(true, null);
    }

    public static ResourcesAllowed withIds(final Set<String> ids) {
      return new ResourcesAllowed(false, ids);
    }

    @Override
    public int hashCode() {
      return Objects.hash(all, ids);
    }

    @Override
    public boolean equals(final Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      final ResourcesAllowed that = (ResourcesAllowed) o;
      return all == that.all && Objects.equals(ids, that.ids);
    }

    /**
     * isAll
     *
     * @return true if all resources are allowed, false if only the ids are allowed
     */
    public boolean isAll() {
      return all;
    }

    /**
     * getIds
     *
     * @return ids of resources allowed in case not all are allowed
     */
    public Set<String> getIds() {
      return ids;
    }
  }
}
