import fuzzy from 'fuzzy';

const ngModule = angular.module('file-finder', []);

ngModule.component('fileFinder', {
  replace: true,
  template: `
    <div>
      <table class="table">
        <tr ng-if="!vm.allFiles.length">
          <td class="text-center"><i class="icon-spinner icon-spin"></i>Loading</td>
        </tr>
        <tr ng-repeat="result in vm.results | limitTo: vm.getLimit()">
          <td style="font-size: 1.1rem;">
            <i
              ng-class="{
                'icon-file': result.original.type === 'f',
                'icon-folder-open': result.original.type === 'd',
              }"
            ></i>
            <a
              ng-if="result.original.type === 'f'"
              target="_self"
              href="{{:: vm.ApiBaseurl}}/download?fn={{:: result.original.name}}"
              ng-bind-html=":: result.string"
            ></a>
            <a
              ng-if="result.original.type === 'd'"
              href="/releases{{:: result.original.name}}"
              ng-bind-html=":: result.string"
            ></a>
          </td>
        </tr>
        <tr ng-if="vm.allFiles.length && !vm.results.length">
          <td class="text-center">
            <strong>No matching files found.</strong>
          </td>
        </tr>
        <tr>
          <td
            ng-if="vm.results.length > vm.defaultLimit && vm.shouldShowAll === false"
            ng-click="vm.shouldShowAll = true"
            style="cursor: pointer; padding-left: 0.8em;"
            tooltip="{{vm.results.length > vm.dangerZone ? 'Are you sure? The page might freeze for a few seconds' : '' }}"
          >
            <i
              class="fa fa-arrow-down"
              ng-class="{
                'fa-arrow-down': !(vm.results.length > vm.dangerZone),
                'fa-exclamation-triangle': vm.results.length > vm.dangerZone,
              }"
              ng-style="{
                color: vm.results.length > vm.dangerZone ? '#d18686' : 'inherit'
              }"
            ></i>
            Show All ({{vm.results.length.toLocaleString()}})
          </td>
        </tr>
      </table>
    </div>
  `,
  controller: function (FileService, API) {
    this.defaultLimit = 20;
    this.dangerZone = 500;
    this.allFiles = [];
    this.results = [];
    this.ApiBaseurl = API.BASE_URL;
    this.shouldShowAll = false;

    this.getLimit = () => this.shouldShowAll ? Infinity : this.defaultLimit;

    FileService.getAllFiles().then((files) => {
      this.allFiles = files;
      if (this.query) {
        onUpdate();
      }
    });
    
    const onUpdate = () => {
      this.results = fuzzy.filter(this.query, this.allFiles, {
        pre: '<strong>',
        post: '</strong>',
        extract: item => item.name,
      });
      this.shouldShowAll = false;
    };

    this.$onChanges = onUpdate;
    this.shouldShowAll = false;
  },
  controllerAs: 'vm',
  bindings: {
    query: '<',
  },
});
