/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.db.rdbms.read.service;

import io.camunda.db.rdbms.read.domain.DecisionInstanceDbQuery;
import io.camunda.db.rdbms.sql.DecisionInstanceMapper;
import io.camunda.db.rdbms.sql.DecisionInstanceMapper.DecisionInstanceSearchColumn;
import io.camunda.search.entities.DecisionInstanceEntity;
import io.camunda.search.query.DecisionInstanceQuery;
import io.camunda.search.query.SearchQueryResult;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DecisionInstanceReader extends AbstractEntityReader<DecisionInstanceEntity> {

  private static final Logger LOG = LoggerFactory.getLogger(DecisionInstanceReader.class);

  private final DecisionInstanceMapper decisionInstanceMapper;

  public DecisionInstanceReader(final DecisionInstanceMapper decisionInstanceMapper) {
    super(DecisionInstanceSearchColumn::findByProperty);
    this.decisionInstanceMapper = decisionInstanceMapper;
  }

  public Optional<DecisionInstanceEntity> findOne(final long decisionInstanceKey) {
    LOG.trace("[RDBMS DB] Search for decision instance with key {}", decisionInstanceKey);
    final var result = search(DecisionInstanceQuery.of(
        b -> b.filter(f -> f.decisionDefinitionKeys(decisionInstanceKey))));
    if (result.items() == null || result.items().isEmpty()) {
      return Optional.empty();
    } else {
      return Optional.of(result.items().getFirst());
    }
  }

  public SearchQueryResult<DecisionInstanceEntity> search(final DecisionInstanceQuery query) {
    final var dbSort = convertSort(query.sort(),
        DecisionInstanceSearchColumn.DECISION_INSTANCE_KEY);
    final var dbQuery =
        DecisionInstanceDbQuery.of(
            b -> b.filter(query.filter()).sort(dbSort).page(convertPaging(dbSort, query.page())));

    LOG.trace("[RDBMS DB] Search for process instance with filter {}", dbQuery);
    final var totalHits = decisionInstanceMapper.count(dbQuery);
    final var hits = decisionInstanceMapper.search(dbQuery);
    return new SearchQueryResult<>(totalHits.intValue(), hits, extractSortValues(hits, dbSort));
  }
}
