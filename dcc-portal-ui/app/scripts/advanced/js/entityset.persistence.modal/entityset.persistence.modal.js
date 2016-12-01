const ngModule = angular.module('entityset.persistence.modal', []);

// import './entityset.persistence.dropdown.scss';

ngModule.component('entitysetPersistenceModal', {
  replace: true,
  template: `
    <div class="modal-content">
      <div class="modal-header clearfix">
          <h3 class="pull-left"><translate>ASDFSADF</translate></h3>
          <button class="pull-right t_button" data-ng-click="vm.handleClickClose();">
            <i class="icon-cancel"></i>
          </button>
      </div>
      <div class="modal-body light" style="max-height:none">
        body
      </div>
      <div class="modal-footer">
          <button class="t_button" data-ng-click="vm.handleClickClose()"><translate>Cancel</translate></button>
          <button class="t_button">
            STUFF
          </button>
      </div>
    </div>
  `,
  controller: function () {
    this.handleClickClose = () => {
      this.close()();
    }
  },
  bindings: {
    close: '&',
    dismiss: '&',
    resolve: '<',
  },
  controllerAs: 'vm',
});
