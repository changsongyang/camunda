/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.it.rdbms.db.fixtures;

import io.camunda.db.rdbms.write.RdbmsWriter;
import io.camunda.db.rdbms.write.domain.DecisionInstanceDbModel;
import io.camunda.search.entities.DecisionInstanceEntity;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.function.Function;

public final class DecisionInstanceFixtures extends CommonFixtures {

  private DecisionInstanceFixtures() {
  }

  public static DecisionInstanceDbModel createRandomized(
      final Function<DecisionInstanceDbModel.Builder, DecisionInstanceDbModel.Builder>
          builderFunction) {
    final var builder =
        new DecisionInstanceDbModel.Builder()
            .decisionInstanceKey(nextKey())
            .processDefinitionKey(nextKey())
            .processDefinitionId("process-" + RANDOM.nextInt(1000))
            .decisionDefinitionKey(nextKey())
            .decisionDefinitionId("decision-" + RANDOM.nextInt(1000))
            .evaluationDate(NOW.plus(RANDOM.nextInt(), ChronoUnit.MILLIS))
            .result("result-" + RANDOM.nextInt(1000))
            .evaluationFailure("failure-" + RANDOM.nextInt(1000))
            .decisionType(randomEnum(DecisionInstanceEntity.DecisionDefinitionType.class))
            .state(randomEnum(DecisionInstanceEntity.DecisionInstanceState.class));

    return builderFunction.apply(builder).build();
  }

  public static void createAndSaveRandomDecisionInstances(final RdbmsWriter rdbmsWriter) {
    createAndSaveRandomDecisionInstances(rdbmsWriter, b -> b);
  }

  public static void createAndSaveRandomDecisionInstances(
      final RdbmsWriter rdbmsWriter,
      final Function<DecisionInstanceDbModel.Builder, DecisionInstanceDbModel.Builder>
          builderFunction) {
    for (int i = 0; i < 20; i++) {
      rdbmsWriter
          .getDecisionInstanceWriter()
          .create(DecisionInstanceFixtures.createRandomized(builderFunction));
    }

    rdbmsWriter.flush();
  }

  public static void createAndSaveDecisionInstance(
      final RdbmsWriter rdbmsWriter, final DecisionInstanceDbModel pecisionInstance) {
    createAndSaveDecisionInstances(rdbmsWriter, List.of(pecisionInstance));
  }

  public static void createAndSaveDecisionInstances(
      final RdbmsWriter rdbmsWriter, final List<DecisionInstanceDbModel> pecisionInstanceList) {
    for (final DecisionInstanceDbModel pecisionInstance : pecisionInstanceList) {
      rdbmsWriter.getDecisionInstanceWriter().create(pecisionInstance);
    }
    rdbmsWriter.flush();
  }
}
