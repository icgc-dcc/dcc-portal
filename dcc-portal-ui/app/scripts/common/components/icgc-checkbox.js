const ngModule = angular.module('app.common.components', []);

ngModule.component('icgcCheckbox', {
    template: `
      <i
        ng-class="{
          'icon-check': $ctrl.isChecked,
          'icon-check-empty': !$ctrl.isChecked,
        }"
      ></i>
    `,
    bindings: {
      isChecked: '<',
      onClickExportCsv: '&',
    }
  });