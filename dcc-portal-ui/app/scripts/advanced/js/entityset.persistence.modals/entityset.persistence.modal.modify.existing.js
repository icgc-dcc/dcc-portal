import invariant from 'invariant';

const ngModule = angular.module('entityset.persistence.modals.modify.existing', []);

ngModule.component('modifyExistingSetModal', {
  replace: true,
  template: `
    <div class="modal-content">
      <div class="modal-header clearfix">
          <h3 class="pull-left" style="text-transform: capitalize;"><translate>{{ vm.operation }} {{ vm.operation.toLowerCase() === 'add' ? 'to' : 'from' }} {{vm.initialEntitysetDefinition.type.toLowerCase()}} set</translate></h3>
          <button class="pull-right t_button" ng-click="vm.handleClickClose();">
            <i class="icon-cancel"></i>
          </button>
      </div>
      <div class="modal-body light" style="max-height:none">
        <pre ng-repeat="set in vm.savedEntitysets">
          {{ set | json }}
        </pre>
      </div>
      <div class="modal-footer">
          <button class="t_button" ng-click="vm.handleClickClose()"><translate>Cancel</translate></button>
          <button class="t_button" ng-click="vm.handleClickSave()">
            Save
          </button>
      </div>
    </div>
  `,
  controller: function (SetService) {
    const validOperations = ['add', 'remove'];
    invariant(validOperations.includes(this.operation), `The "operation" binding must be one of ${JSON.stringify(validOperations)}`);

    this.savedEntitysets = SetService.getAll();
    console.log(this.savedEntitysets);
    this.name = this.initialEntitysetDefinition.name;

    this.handleClickClose = () => {
      this.close()();
    };

    this.handleClickSave = () => {
      this.close()();
      // SetService.addSet(this.initialEntitysetDefinition.type, Object.assign({}, this.initialEntitysetDefinition, {
      //   name: this.name,
      // }));
    };
    
  },
  bindings: {
    close: '&',
    dismiss: '&',
    initialEntitysetDefinition: '<',
    operation: '<'
  },
  controllerAs: 'vm',
});
