import './togglable-term.scss';

angular
.module('app.common.components')
.component('togglableTerm', {
  bindings: {
    isActive: '<',
    onClick: '&',
    label: '<',
    itemsAffectedByFacet: '<',
    itemsAffectedByTerm: '<',
    fdaCount: '<',
    worldCount: '<',
    isFda: '<',
    isWorld: '<'
  },
  transclude: true,
  controller: function () {},
  controllerAs: 'vm',
  template: `
    <div
      ng-class="{
        'is-active': vm.isActive,
        't_facets__facet__terms__active__term__label': vm.isActive,
        't_facets__facet__terms__inactive__term': !vm.isActive,
      }"
      ng-click="vm.onClick(); track('term', {action: 'toggle', label: vm.label})"
      style="padding: 0; position: relative;"
    >
        <ng-transclude></ng-transclude>
        <span
          class="term__extent-bar"
          ng-style="{ width: vm.itemsAffectedByTerm / vm.itemsAffectedByFacet * 100 + '%'}"
        ></span>
        <span
          class="term__label t_facets__facet__terms__inactive__term__label"
        > 
          <i
            class="term__label__icon"
            ng-class="{
              'icon-ok': vm.isActive,
              'icon-check-empty': !vm.isActive,
            }"
          ></i>
          <span>{{ vm.label }}</span>
        </span>
        <span  ng-if="vm.isFda" class="term__count t_facets__facet__terms__active__term__count">
          {{ vm.fdaCount }}
        </span>
        <span  ng-if="vm.isWorld" class="term__count t_facets__facet__terms__active__term__count">
          {{ vm.worldCount }}
        </span>
    </div>
  `
});
