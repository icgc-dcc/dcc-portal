const ngModule = angular.module('entityset.persistence.dropdown', []);

import './entityset.persistence.dropdown.scss';

ngModule.component('entitysetPersistenceDropdown', {
  replace: true,
  template: require('./entityset.persistence.dropdown.html'),
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
    onClickSaveNew: '&',
    onClickAddToSet: '&',
    onClickRemoveFromSet: '&',
  },
  controllerAs: 'vm',
});
