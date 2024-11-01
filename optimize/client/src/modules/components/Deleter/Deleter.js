/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */

import React from 'react';
import {Button, Stack} from '@carbon/react';

import {withErrorHandling} from 'HOC';
import {Modal, Loading} from 'components';
import {showError} from 'notifications';
import {deleteEntity} from 'services';
import {t} from 'translation';

const sectionOrder = ['report', 'dashboard', 'alert', 'collection'];

export default withErrorHandling(
  class Deleter extends React.Component {
    static defaultProps = {
      deleteEntity: ({entityType, id}) => deleteEntity(entityType, id),
      getName: ({name}) => name,
      onDelete: () => {},
    };

    cancelButton = React.createRef();

    state = {
      conflicts: {},
      loading: false,
    };

    componentDidUpdate(prevProps, prevState) {
      const {entity, checkConflicts} = this.props;
      if (prevProps.entity !== entity && entity) {
        if (checkConflicts) {
          this.setState({loading: true});
          this.props.mightFail(
            checkConflicts(entity),
            (response) => {
              if (typeof response === 'boolean') {
                if (response) {
                  this.props.onConflict?.();
                }
              } else {
                this.setState({
                  conflicts: response.conflictedItems.reduce((obj, conflict) => {
                    obj[conflict.type] = obj[conflict.type] || [];
                    obj[conflict.type].push(conflict);
                    return obj;
                  }, {}),
                });
              }
            },
            (error) => {
              showError(error);
              this.setState({conflicts: {}});
            },
            () => this.setState({loading: false})
          );
        } else {
          this.setState({conflicts: {}, loading: false});
          this.cancelButton.current.focus();
        }
      }

      if (prevState.loading && !this.state.loading) {
        this.cancelButton.current?.focus();
      }
    }

    delete = () => {
      const {entity, onDelete, deleteEntity} = this.props;

      this.setState({loading: true});
      this.props.mightFail(
        deleteEntity(entity),
        (...args) => {
          onDelete(...args);
          this.close();
        },
        (error) => {
          showError(error);
        },
        () => this.setState({loading: false})
      );
    };

    close = () => {
      this.setState({conflicts: {}, loading: false}, this.props.onClose);
    };

    render() {
      const {entity, getName, type, descriptionText, deleteText, deleteButtonText} = this.props;
      const {conflicts, loading} = this.state;

      if (!entity) {
        return null;
      }

      const translatedType = t(`common.deleter.types.${type}`);

      return (
        <Modal open onClose={this.close} className="Deleter">
          <Modal.Header title={deleteText || t('common.deleteEntity', {entity: translatedType})} />
          <Modal.Content>
            <Stack gap={4}>
              {loading ? (
                <Loading />
              ) : (
                <>
                  <p>
                    {descriptionText ||
                      t('common.deleter.permanent', {
                        name: getName(entity),
                        type: translatedType,
                      })}
                  </p>
                  {Object.keys(conflicts)
                    .sort((a, b) => sectionOrder.indexOf(a) - sectionOrder.indexOf(b))
                    .map((conflictType) => (
                      <div key={conflictType}>
                        {t(`common.deleter.affectedMessage.${type}.${conflictType}`)}
                        <ul>
                          {conflicts[conflictType].map(({id, name}) => (
                            <li key={id}>'{name || id}'</li>
                          ))}
                        </ul>
                      </div>
                    ))}
                  <p>{!this.props.isReversableAction && <b>{t('common.deleter.noUndo')}</b>}</p>
                </>
              )}
            </Stack>
          </Modal.Content>
          <Modal.Footer>
            <Button
              disabled={loading}
              className="close"
              onClick={this.close}
              ref={this.cancelButton}
              kind="secondary"
            >
              {t('common.cancel')}
            </Button>
            <Button kind="danger" disabled={loading} className="confirm" onClick={this.delete}>
              {deleteButtonText || deleteText || t('common.deleteEntity', {entity: translatedType})}
            </Button>
          </Modal.Footer>
        </Modal>
      );
    }
  }
);
