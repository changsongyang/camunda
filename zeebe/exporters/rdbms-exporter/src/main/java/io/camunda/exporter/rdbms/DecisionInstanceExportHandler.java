/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.exporter.rdbms;

import io.camunda.db.rdbms.write.domain.DecisionInstanceDbModel;
import io.camunda.db.rdbms.write.domain.DecisionInstanceDbModel.EvaluatedInput;
import io.camunda.db.rdbms.write.domain.DecisionInstanceDbModel.EvaluatedOutput;
import io.camunda.db.rdbms.write.service.DecisionInstanceWriter;
import io.camunda.search.entities.DecisionInstanceEntity.DecisionInstanceState;
import io.camunda.zeebe.protocol.record.Record;
import io.camunda.zeebe.protocol.record.ValueType;
import io.camunda.zeebe.protocol.record.intent.DecisionEvaluationIntent;
import io.camunda.zeebe.protocol.record.value.DecisionEvaluationRecordValue;
import io.camunda.zeebe.protocol.record.value.EvaluatedDecisionValue;
import io.camunda.zeebe.protocol.record.value.EvaluatedInputValue;
import io.camunda.zeebe.protocol.record.value.MatchedRuleValue;
import io.camunda.zeebe.util.DateUtil;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class DecisionInstanceExportHandler implements
    RdbmsExportHandler<DecisionEvaluationRecordValue> {

  private final DecisionInstanceWriter decisionInstanceWriter;

  public DecisionInstanceExportHandler(final DecisionInstanceWriter decisionInstanceWriter) {
    this.decisionInstanceWriter = decisionInstanceWriter;
  }

  @Override
  public boolean canExport(final Record<DecisionEvaluationRecordValue> record) {
    return record.getValueType() == ValueType.DECISION_EVALUATION
        && record.getIntent() == DecisionEvaluationIntent.EVALUATED;
  }

  @Override
  public void export(final Record<DecisionEvaluationRecordValue> record) {
    final DecisionEvaluationRecordValue value = record.getValue();

    int index = 0;
    for (final EvaluatedDecisionValue evaluatedDecision : value.getEvaluatedDecisions()) {
      final var state = getState(record, value, index);
      final var decisionInstance = new DecisionInstanceDbModel.Builder()
          .decisionInstanceKey(record.getKey() + index) // TODO fix to real key?
          .decisionDefinitionKey(evaluatedDecision.getDecisionKey())
          .decisionDefinitionId(evaluatedDecision.getDecisionId())
          .evaluationDate(DateUtil.toOffsetDateTime(Instant.ofEpochMilli(record.getTimestamp())))
          .processDefinitionKey(value.getProcessDefinitionKey())
          .processDefinitionId(value.getBpmnProcessId())
          .processInstanceKey(value.getProcessInstanceKey())
          .decisionRequirementsKey(value.getDecisionRequirementsKey())
          .decisionRequirementsId(value.getDecisionRequirementsId())
          .flowNodeInstanceKey(value.getElementInstanceKey())
          .flowNodeId(value.getElementId())
          .rootDecisionDefinitionKey(evaluatedDecision.getDecisionKey())
          .result(evaluatedDecision.getDecisionOutput())
          .evaluatedInputs(createEvaluationInputs(evaluatedDecision.getEvaluatedInputs()))
          .evaluatedOutputs(createEvaluationOutputs(evaluatedDecision.getMatchedRules()))
          .state(state)
          .evaluationFailure(
              state == DecisionInstanceState.FAILED ? value.getEvaluationFailureMessage() : null)
          .build();

      decisionInstanceWriter.create(decisionInstance);
      index++;
    }
  }

  // TODO move to common exporter util module
  private DecisionInstanceState getState(
      final Record<DecisionEvaluationRecordValue> record,
      final DecisionEvaluationRecordValue decisionEvaluation,
      final int i) {
    if (record.getIntent().name().equals(DecisionEvaluationIntent.FAILED.name())
        && i == decisionEvaluation.getEvaluatedDecisions().size() - 1) {
      return DecisionInstanceState.FAILED;
    } else {
      return DecisionInstanceState.EVALUATED;
    }
  }


  private List<EvaluatedInput> createEvaluationInputs(
      final List<EvaluatedInputValue> evaluatedInputs) {
    return evaluatedInputs.stream()
        .map(
            input ->
                new EvaluatedInput.Builder()
                    .id(input.getInputId())
                    .name(input.getInputName())
                    .value(input.getInputValue())
                    .build())
        .collect(Collectors.toList());
  }

  private List<EvaluatedOutput> createEvaluationOutputs(
      final List<MatchedRuleValue> matchedRules) {
    final List<EvaluatedOutput> outputs = new ArrayList<>();
    matchedRules.forEach(
        rule ->
            outputs.addAll(
                rule.getEvaluatedOutputs().stream()
                    .map(
                        output ->
                            new EvaluatedOutput.Builder()
                                .ruleId(rule.getRuleId())
                                .ruleIndex(rule.getRuleIndex())
                                .id(output.getOutputId())
                                .name(output.getOutputName())
                                .value(output.getOutputValue())
                                .build())
                    .toList()));
    return outputs;
  }
}
