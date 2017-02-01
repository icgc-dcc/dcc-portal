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
      ng-click="vm.onClick()"
      style="padding: 0; position: relative;"
    >
        <ng-transclude></ng-transclude>
        <span
          class="term__extent-bar"
          ng-style="{ width: vm.itemsAffectedByTerm / vm.itemsAffectedByFacet * 100 + '%'}"
        ></span>
        <span
          class="term__label t_facets__facet__terms__inactive__term__label"
          data-tooltip="{{ vm.label }}"
          data-tooltip-placement="overflow"
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
        <span class="term__count t_facets__facet__terms__active__term__count">
          {{ vm.itemsAffectedByTerm }}
        </span>
    </div>
  `
});
