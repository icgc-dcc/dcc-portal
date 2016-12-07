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
            {{ vm.operation.toLowerCase() === 'add' ? 'Add to' : 'Remove from' }}
            <translate> existing {{ vm.initialEntitysetDefinition.type.toLowerCase() }} set:</translate>
          </h3>
          <button class="pull-right t_button" ng-click="vm.handleClickClose();">
            <i class="icon-cancel"></i>
          </button>
      </div>
      <div class="modal-body light" style="max-height:80vh; overflow: auto;">
        <div
          class="saved-set"
          ng-repeat="entityset in vm.eligibleEntitysets"
          ng-click="vm.handleClickSavedEntityset(entityset)"
          ng-class="{
            'is-selected': vm.selectedEntitysets.includes(entityset)
          }"
        >
          {{entityset.name}}
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
      this.close()();
    };

    this.handleClickSave = () => {
      this.close()();
      SetService.modifySet(this.selectedEntitysets[0], this.initialEntitysetDefinition, this.operation);
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
    operation: '<'
  },
  controllerAs: 'vm',
});
