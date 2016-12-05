const ngModule = angular.module('entityset.persistence.dropdown', []);

import './entityset.persistence.dropdown.scss';

ngModule.component('entitysetPersistenceDropdown', {
  replace: true,
  template: require('!raw!./entityset.persistence.dropdown.html'),
  controller: function () {
    const guardBindings = () => {
      this.selectedEntityIds = this.selectedEntityIds || [];
    };
    this.$onInit = guardBindings;
    this.$onChanges = guardBindings;
  },
  bindings: {
    selectedEntityIds: '<',
    entityType: '<',
    setLimit: '<',
    setTotalCount: '<',
    onClickSaveNew: '&',
    onClickAddToSet: '&',
    onClickRemoveFromSet: '&',
  },
  controllerAs: 'vm',
});