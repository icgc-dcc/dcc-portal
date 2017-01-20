const ngModule = angular.module('entityset.persistence.modals.save.new', []);

ngModule.component('saveNewSetModal', {
  replace: true,
  template: `
    <div class="modal-content">
      <div class="modal-header clearfix">
          <h3 class="pull-left"><translate>Save {{ vm.initialEntitysetDefinition.size.toLocaleString() }} {{ vm.initialEntitysetDefinition.type.toLowerCase() | pluralize : vm.initialEntitysetDefinition.size }} as new set</translate></h3>
          <button class="pull-right t_button" ng-click="vm.handleClickClose();">
            <i class="icon-cancel"></i>
          </button>
      </div>
      <div class="modal-body light" style="max-height:none">
        Name: 
        <input type="text" ng-model="vm.name" maxlength="64" select-on-click style="width:100%;" autofocus></input>
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
    this.name = this.initialEntitysetDefinition.name;
    this.handleClickClose = () => {
      this.close();
    };
    this.handleClickSave = () => {
      this.close();
      SetService.addSet(this.initialEntitysetDefinition.type, Object.assign({}, this.initialEntitysetDefinition, {
        name: this.name,
      }), this.initialEntitysetDefinition.type.toLowerCase() === 'file')
        .then(this.onOperationSuccess);
    };
  },
  bindings: {
    close: '&',
    dismiss: '&',
    initialEntitysetDefinition: '<',
    onOperationSuccess: '&',
  },
  controllerAs: 'vm',
});
