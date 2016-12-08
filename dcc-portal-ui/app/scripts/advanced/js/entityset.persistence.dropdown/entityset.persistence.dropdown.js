import createEntitysetDefinition from './createEntitysetDefinition';
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

    const getFiltersFromEntityIds = (entityType, ids) => ({
      [entityType]: {
        id: {
          is: ids
        }
      }
    });

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

      const entitysetDefinition = createEntitysetDefinition({
        filters: getFiltersFromEntityIds(entityType, selectedEntityIds),
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
      ? getFiltersFromEntityIds(entityType, selectedEntityIds)
      : FilterService.filters();

      const entitysetDefinition = createEntitysetDefinition({
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
