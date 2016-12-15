import fuzzy from 'fuzzy';

const ngModule = angular.module('file-finder', []);


ngModule.component('fileFinder', {
  replace: true,
  template: `
    <div>
      <table class="table">
        <tr ng-repeat="result in vm.results">
          <td style="font-size: 1.1rem;">
            <i class="icon-file"></i>
            <a ng-href="/releases/{{result.original.name}}" ng-bind-html="result.string"></a>
          </td>
        </tr>
        <tr ng-if="!vm.results.length">
          <td class="text-center">
            <strong>No matching files found.</strong>
          </td>
        </tr>
      </table>
    </div>
  `,
  controller: function (FileService) {
    this.allFiles = [];
    this.results = [];

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
      }).slice(0, 20);
    };

    this.$onChanges = onUpdate;
  },
  controllerAs: 'vm',
  bindings: {
    query: '<',
  },
});
