import createEntitysetDefinition from './createEntitysetDefinition';
const ngModule = angular.module('entityset.persistence.dropdown', []);

import './entityset.persistence.dropdown.scss';

ngModule.component('entitysetPersistenceDropdown', {
  replace: true,
  template: require('!raw!./entityset.persistence.dropdown.html'),
  controller: function (
    SetService,
    SetNameService,
    FilterService,
    $modal,
    FiltersUtil
  ) {
    const guardBindings = () => {
      this.selectedEntityIds = this.selectedEntityIds || [];
    };
    this.$onInit = guardBindings;
    this.$onChanges = guardBindings;

    this.hasSets = () => !!SetService.getAll().length;

    const getFiltersFromEntityIds = (entityType, ids) => ({
      [entityType]: {
        id: {
          is: ids
        }
      }
    });

    this.handleClickSaveNew = async () => {
      const {selectedEntityIds, entityType, onOperationSuccess} = this;
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

      const filters = await getFiltersFromEntityIds(entityType, selectedEntityIds);
      const entitysetDefinition = createEntitysetDefinition({
        filters,
        name: await SetNameService.getSetName(FiltersUtil.buildUIFilters(filters)),
        type: String.prototype.toUpperCase.apply(entityType),
        size: selectedEntityIds.length,
      });

      $modal.open({
        template: `
          <save-new-set-modal
            close="vm.$close()"
            initial-entityset-definition="vm.entitysetDefinition"
            on-operation-success="vm.onOperationSuccess()"
          ></save-new-set-modal>
        `,
        controller: function () {
          this.entitysetDefinition = entitysetDefinition;
          this.onOperationSuccess = onOperationSuccess;
        },
        controllerAs: 'vm',
        bindToController: true,
      });
    };

    this.handleClickModifySet = (operation) => {
      if (!this.hasSets()) {
        return;
      }
      const {selectedEntityIds, entityType, onOperationSuccess} = this;
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
            close="vm.$close()"
            initial-entityset-definition="vm.entitysetDefinition"
            operation="vm.operation"
            on-operation-success="vm.onOperationSuccess()"
          ></modify-existing-set-modal>
        `,
        controller: function () {
          this.entitysetDefinition = entitysetDefinition;
          this.operation = operation;
          this.onOperationSuccess = onOperationSuccess;
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
    onOperationSuccess: '&',
  },
  controllerAs: 'vm',
});
