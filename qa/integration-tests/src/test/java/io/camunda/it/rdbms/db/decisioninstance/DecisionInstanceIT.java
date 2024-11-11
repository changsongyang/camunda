/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.it.rdbms.db.decisioninstance;

import static io.camunda.it.rdbms.db.fixtures.DecisionInstanceFixtures.createAndSaveDecisionInstance;
import static org.assertj.core.api.Assertions.assertThat;

import io.camunda.db.rdbms.RdbmsService;
import io.camunda.db.rdbms.read.service.DecisionInstanceReader;
import io.camunda.db.rdbms.write.RdbmsWriter;
import io.camunda.it.rdbms.db.fixtures.DecisionInstanceFixtures;
import io.camunda.it.rdbms.db.util.CamundaRdbmsInvocationContextProviderExtension;
import io.camunda.it.rdbms.db.util.CamundaRdbmsTestApplication;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import org.assertj.core.data.TemporalUnitWithinOffset;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;

@Tag("rdbms")
@ExtendWith(CamundaRdbmsInvocationContextProviderExtension.class)
public class DecisionInstanceIT {

  public static final Long PARTITION_ID = 0L;
  public static final OffsetDateTime NOW = OffsetDateTime.now();

  @TestTemplate
  public void shouldSaveAndFindDecisionInstanceByKey(
      final CamundaRdbmsTestApplication testApplication) {
    final RdbmsService rdbmsService = testApplication.getRdbmsService();
    final RdbmsWriter rdbmsWriter = rdbmsService.createWriter(PARTITION_ID);
    final DecisionInstanceReader decisionInstanceReader = rdbmsService.getDecisionInstanceReader();

    final var original = DecisionInstanceFixtures.createRandomized(b -> b);
    createAndSaveDecisionInstance(rdbmsWriter, original);
    final var actual = decisionInstanceReader.findOne(original.decisionInstanceKey()).orElseThrow();

    assertThat(actual).isNotNull();
    assertThat(actual.key()).isEqualTo(original.decisionInstanceKey());
    assertThat(actual.bpmnProcessId()).isEqualTo(original.processDefinitionId());
    assertThat(actual.processDefinitionKey()).isEqualTo(original.processDefinitionKey());
    assertThat(actual.decisionId()).isEqualTo(original.decisionDefinitionId());
    assertThat(actual.decisionDefinitionId()).isEqualTo(original.decisionDefinitionId());
    assertThat(actual.state()).isEqualTo(original.state());
    assertThat(actual.evaluationDate())
        .isCloseTo(original.evaluationDate(), new TemporalUnitWithinOffset(1, ChronoUnit.MILLIS));
    assertThat(actual.evaluationFailure()).isEqualTo(original.evaluationFailure());
    assertThat(actual.result()).isEqualTo(original.result());
//    assertThat(actual.decisionType()).isEqualTo(original.decisionType()); TODO
  }



/*
  @TestTemplate
  public void shouldFindDecisionInstanceByBpmnProcessId(
      final CamundaRdbmsTestApplication testApplication) {
    final RdbmsService rdbmsService = testApplication.getRdbmsService();
    final RdbmsWriter rdbmsWriter = rdbmsService.createWriter(PARTITION_ID);
    final DecisionInstanceReader decisionInstanceReader = rdbmsService.getDecisionInstanceReader();

    final Long decisionInstanceKey = DecisionInstanceFixtures.nextKey();
    createAndSaveDecisionInstance(
        rdbmsWriter,
        DecisionInstanceFixtures.createRandomized(
            b ->
                b.decisionInstanceKey(decisionInstanceKey)
                    .processDefinitionId("test-process-unique")
                    .processDefinitionKey(1338L)
                    .state(DecisionInstanceState.ACTIVE)
                    .startDate(NOW)
                    .parentDecisionInstanceKey(-1L)
                    .parentElementInstanceKey(-1L)
                    .version(1)));

    final var searchResult =
        decisionInstanceReader.search(
            DecisionInstanceQuery.of(
                b ->
                    b.filter(f -> f.processDefinitionIds("test-process-unique"))
                        .sort(s -> s)
                        .page(p -> p.from(0).size(10))));

    assertThat(searchResult).isNotNull();
    assertThat(searchResult.total()).isEqualTo(1);
    assertThat(searchResult.items()).hasSize(1);

    final var instance = searchResult.items().getFirst();

    assertThat(instance.key()).isEqualTo(decisionInstanceKey);
    assertThat(instance.bpmnProcessId()).isEqualTo("test-process-unique");
    assertThat(instance.processDefinitionKey()).isEqualTo(1338L);
    assertThat(instance.state()).isEqualTo(DecisionInstanceState.ACTIVE);
    assertThat(instance.startDate())
        .isCloseTo(NOW, new TemporalUnitWithinOffset(1, ChronoUnit.MILLIS));
    assertThat(instance.parentDecisionInstanceKey()).isEqualTo(-1L);
    assertThat(instance.parentFlowNodeInstanceKey()).isEqualTo(-1L);
    assertThat(instance.processVersion()).isEqualTo(1);
  }

  @TestTemplate
  public void shouldFindAllDecisionInstancePaged(final CamundaRdbmsTestApplication testApplication) {
    final RdbmsService rdbmsService = testApplication.getRdbmsService();
    final RdbmsWriter rdbmsWriter = rdbmsService.createWriter(PARTITION_ID);
    final DecisionInstanceReader decisionInstanceReader = rdbmsService.getDecisionInstanceReader();

    final String processDefinitionId = DecisionInstanceFixtures.nextStringId();
    createAndSaveRandomDecisionInstances(
        rdbmsWriter, b -> b.processDefinitionId(processDefinitionId));

    final var searchResult =
        decisionInstanceReader.search(
            DecisionInstanceQuery.of(
                b ->
                    b.filter(f -> f.processDefinitionIds(processDefinitionId))
                        .sort(s -> s.startDate().asc().processDefinitionName().asc())
                        .page(p -> p.from(0).size(5))));

    assertThat(searchResult).isNotNull();
    assertThat(searchResult.total()).isEqualTo(20);
    assertThat(searchResult.items()).hasSize(5);

    final var lastInstance = searchResult.items().getLast();
    assertThat(searchResult.sortValues()).hasSize(3);
    assertThat(searchResult.sortValues())
        .containsExactly(lastInstance.startDate(), lastInstance.processName(), lastInstance.key());
  }

  @TestTemplate
  public void shouldFindDecisionInstanceWithFullFilter(
      final CamundaRdbmsTestApplication testApplication) {
    final RdbmsService rdbmsService = testApplication.getRdbmsService();
    final RdbmsWriter rdbmsWriter = rdbmsService.createWriter(PARTITION_ID);
    final DecisionInstanceReader decisionInstanceReader = rdbmsService.getDecisionInstanceReader();

    final Long decisionInstanceKey = DecisionInstanceFixtures.nextKey();
    createAndSaveRandomDecisionInstances(rdbmsWriter);
    createAndSaveDecisionInstance(
        rdbmsWriter,
        DecisionInstanceFixtures.createRandomized(
            b ->
                b.decisionInstanceKey(decisionInstanceKey)
                    .processDefinitionId("test-process")
                    .processDefinitionKey(1337L)
                    .state(DecisionInstanceState.ACTIVE)
                    .startDate(NOW)
                    .endDate(NOW)
                    .parentDecisionInstanceKey(-1L)
                    .parentElementInstanceKey(-1L)
                    .version(1)));

    final var searchResult =
        decisionInstanceReader.search(
            DecisionInstanceQuery.of(
                b ->
                    b.filter(
                            f ->
                                f.decisionInstanceKeys(decisionInstanceKey)
                                    .processDefinitionIds("test-process")
                                    .processDefinitionKeys(1337L)
                                    .states(DecisionInstanceState.ACTIVE.name())
                                    .parentDecisionInstanceKeys(-1L)
                                    .parentFlowNodeInstanceKeys(-1L))
                        .sort(s -> s)
                        .page(p -> p.from(0).size(5))));

    assertThat(searchResult.total()).isEqualTo(1);
    assertThat(searchResult.items()).hasSize(1);
    assertThat(searchResult.items().getFirst().key()).isEqualTo(decisionInstanceKey);
  }

  @TestTemplate
  public void shouldFindDecisionInstanceWithSearchAfter(
      final CamundaRdbmsTestApplication testApplication) {
    final RdbmsService rdbmsService = testApplication.getRdbmsService();
    final RdbmsWriter rdbmsWriter = rdbmsService.createWriter(PARTITION_ID);
    final DecisionInstanceReader decisionInstanceReader = rdbmsService.getDecisionInstanceReader();

    final var processDefinition =
        ProcessDefinitionFixtures.createAndSaveProcessDefinition(rdbmsWriter, b -> b);
    createAndSaveRandomDecisionInstances(
        rdbmsWriter,
        b ->
            b.processDefinitionKey(processDefinition.processDefinitionKey())
                .processDefinitionId(processDefinition.processDefinitionId()));
    final var sort =
        DecisionInstanceSort.of(
            s ->
                s.processDefinitionName()
                    .asc()
                    .processDefinitionVersion()
                    .asc()
                    .startDate()
                    .desc());
    final var searchResult =
        decisionInstanceReader.search(
            DecisionInstanceQuery.of(
                b ->
                    b.filter(f -> f.processDefinitionIds(processDefinition.processDefinitionId()))
                        .sort(sort)
                        .page(p -> p.from(0).size(20))));

    final var instanceAfter = searchResult.items().get(9);
    final var nextPage =
        decisionInstanceReader.search(
            DecisionInstanceQuery.of(
                b ->
                    b.filter(f -> f.processDefinitionIds(processDefinition.processDefinitionId()))
                        .sort(sort)
                        .page(
                            p ->
                                p.size(5)
                                    .searchAfter(
                                        new Object[]{
                                            instanceAfter.processName(),
                                            instanceAfter.processVersion(),
                                            instanceAfter.startDate(),
                                            instanceAfter.key()
                                        }))));

    assertThat(nextPage.total()).isEqualTo(20);
    assertThat(nextPage.items()).hasSize(5);
    assertThat(nextPage.items()).isEqualTo(searchResult.items().subList(10, 15));
  }*/
}
