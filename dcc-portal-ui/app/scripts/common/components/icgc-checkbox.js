import _ from 'lodash';
const ngModule = angular.module('app.common.components', []);

ngModule.component('icgcCheckbox', {
    template: `
      <i
        class="{{vm.classes}}"
      ></i>
    `,
    controller: function () {
      const validTypes = ['checkbox', 'radio'];
      const onBindingChange = () => {
        invariant(!this.type || validTypes.includes(this.type), `type must be one of ${JSON.stringify(validTypes)}`);
        this.type = this.type || 'checkbox';
        this.classes = Object.keys(_.pickBy(getClasses())).join(' ');
      };
      this.$onInit = onBindingChange;
      this.$onChanges = onBindingChange;

      const typeClasses = {
        checkbox: {
          checked: 'icon-check',
          empty: 'icon-check-empty',
        },
        radio: {
          checked: 'icon-dot-circled',
          empty: 'icon-circle-empty',
        }
      };

      const getClasses = () => ({
        [typeClasses[this.type].checked]: this.isChecked,
        [typeClasses[this.type].empty]: !this.isChecked,
      });
    },
    controllerAs: 'vm',
    bindings: {
      isChecked: '<',
      onClickExportCsv: '&',
      type: '<',
    }
  });