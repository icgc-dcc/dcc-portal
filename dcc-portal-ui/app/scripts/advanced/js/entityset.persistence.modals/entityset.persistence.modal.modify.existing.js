import invariant from 'invariant';
import _ from 'lodash';

const ngModule = angular.module('entityset.persistence.modals.modify.existing', []);

import './entityset.persistence.modal.modify.existing.scss';

ngModule.component('modifyExistingSetModal', {
  replace: true,
  template: `
    <div class="modal-content modify-existing-set-modal">
      <div class="modal-header clearfix">
          <h3 class="pull-left">
            {{ vm.operation.toLowerCase() === 'add' ? 'Add' : 'Remove' }}
            {{ vm.initialEntitysetDefinition.size.toLocaleString() }}
            {{ vm.initialEntitysetDefinition.type.toLowerCase() | pluralize : vm.initialEntitysetDefinition.size}}
            {{ vm.operation.toLowerCase() === 'add' ? 'to' : 'from' }}
            <translate>existing set</translate>
          </h3>
          <button class="pull-right t_button" ng-click="vm.handleClickClose();">
            <i class="icon-cancel"></i>
          </button>
      </div>
      <div class="modal-body light" style="max-height:80vh; overflow: auto;">
        <table class="table table-info" ng-if="vm.eligibleEntitysets.length">
          <thead>
            <tr>
              <td></td>
              <td><translate>Name</translate></td>
              <td class="text-right"><translate># Items</translate></td>
            </tr>
          </thead>
          <tbody>
            <tr
              ng-repeat="entityset in vm.eligibleEntitysets"
              ng-click="vm.handleClickSavedEntityset(entityset)"
              ng-class="{
                'is-selected': vm.selectedEntitysets.includes(entityset)
              }"
            >
              <td style="text-align: center;">
                <icgc-checkbox
                  is-checked="vm.selectedEntitysets.includes(entityset)"
                  type="'radio'"
                ></icgc-checkbox>
              </td>
              <td>{{entityset.name}}</td>
              <td class="text-right">{{entityset.count.toLocaleString()}}</td>
            </tr>
          </tbody>
        </table>
        <div ng-if="!vm.eligibleEntitysets.length" class="empty">
          <translate>No saved {{vm.initialEntitysetDefinition.type.toLowerCase()}} sets are eligible</translate>
        </div>
      </div>
      <div class="modal-footer">
          <button class="t_button" ng-click="vm.handleClickClose()"><translate>Cancel</translate></button>
          <button class="t_button" ng-click="vm.handleClickSave()" ng-disabled="!vm.isValid()">
            Submit
          </button>
      </div>
    </div>
  `,
  controller: function (SetService) {
    const validOperations = ['add', 'remove'];
    invariant(validOperations.includes(this.operation), `The "operation" binding must be one of ${JSON.stringify(validOperations)}`);
    this.eligibleEntitysets = SetService.getAll().filter(entityset => entityset.type.toLowerCase() === this.initialEntitysetDefinition.type.toLowerCase());
    this.selectedEntitysets = [];

    this.handleClickClose = () => {
      this.close();
    };

    this.handleClickSave = () => {
      this.close();
      SetService.modifySet(this.selectedEntitysets[0], this.initialEntitysetDefinition, this.operation)
        .then(this.onOperationSuccess);
    };

    this.handleClickSavedEntityset = (entityset) => {
      this.selectedEntitysets = this.selectedEntitysets.includes(entityset) ? [] : [entityset];
    };

    this.isValid = () => this.selectedEntitysets.length !== 0;
    
  },
  bindings: {
    close: '&',
    dismiss: '&',
    initialEntitysetDefinition: '<',
    operation: '<',
    onOperationSuccess: '&',
  },
  controllerAs: 'vm',
});
