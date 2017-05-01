angular
.module('app.common.components')
.component('collapsibleWrapper', {
  bindings: {
    title: '<',
    isCollapsed: '<'
  },
  transclude: true,
  controller: function () {
  },
  controllerAs: 'vm',
  template: `
    <ul class="t_facets__facet">
      <li
        class="t_facets__facet__title"
        ng-click="vm.isCollapsed = !vm.isCollapsed"
      >
        <span class="t_facets__facet__title__label">
          <i
            data-ng-class="{
              'icon-caret-down': !vm.isCollapsed,
              'icon-caret-right': vm.isCollapsed,
          }"></i>
          {{vm.title}}
        </span>
      </li>
      <li
        class=""
        ng-transclude
        ng-show="!vm.isCollapsed"
      ></li>
    </ul>
  `
});
