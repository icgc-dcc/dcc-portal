const ngModule = angular.module('entityset.persistence.dropdown', []);

import './entityset.persistence.dropdown.scss';

ngModule.component('entitysetPersistenceDropdown', {
  replace: true,
  template: require('!raw!./entityset.persistence.dropdown.html'),
  controller: function (SetService, FilterService, $modal) {
    const guardBindings = () => {
      this.selectedEntityIds = this.selectedEntityIds || [];
    };
    this.$onInit = guardBindings;
    this.$onChanges = guardBindings;

    // (vm.entityType, [vm.setTotalCount, vm.setLimit].sort()[0])

    this.handleClickSaveNew = () => {
      const {selectedEntityIds, entityType} = this;
      if (!selectedEntityIds || !selectedEntityIds.length) {
        $modal.open({
          templateUrl: '/scripts/sets/views/sets.upload.html',
          controller: 'SetUploadController',
          resolve: {
            setType: () => entityType,
            setLimit: () => this.setTotalCount,
            setUnion: () => undefined,
            selectedIds: () => undefined,
          }
        });
        return;
      }

      const filters = {
        [entityType]: {
          id: {
            is: selectedEntityIds
          }
        }
      };

      const entitysetDefinition = require('./createEntitysetDefinition')({
        filters,
        name: selectedEntityIds.join(' / '),
        type: String.prototype.toUpperCase.apply(entityType),
        size: selectedEntityIds.length,
      });

      $modal.open({
        template: `
          <save-new-set-modal
            close="vm.$close"
            initial-entityset-definition="vm.entitysetDefinition"
          ></save-new-set-modal>
        `,
        controller: function () {
          this.entitysetDefinition = entitysetDefinition;
        },
        controllerAs: 'vm',
        bindToController: true,
      });
    };

    this.handleClickAddToSet = () => {
      const {selectedEntityIds, entityType} = this;
      const filters = selectedEntityIds.length
      ? {
          [entityType]: {
            id: {
              is: selectedEntityIds
            }
          }
        }
      : FilterService.filters();

      const entitysetDefinition = require('./createEntitysetDefinition')({
        filters,
        name: 'temp set',
        type: String.prototype.toUpperCase.apply(entityType),
        size: selectedEntityIds.length ? selectedEntityIds.length : Math.min(this.setLimit, this.setTotalCount),
      });

      $modal.open({
        template: `
          <modify-existing-set-modal
            close="vm.$close"
            initial-entityset-definition="vm.entitysetDefinition"
            operation="'add'"
          ></modify-existing-set-modal>
        `,
        controller: function () {
          this.entitysetDefinition = entitysetDefinition;
        },
        controllerAs: 'vm',
        bindToController: true,
      });
    };
  },
  bindings: {
    selectedEntityIds: '<',
    entityType: '<',
    setLimit: '<',
    setTotalCount: '<',
  },
  controllerAs: 'vm',
});
