angular.module('icgc.analysis.setTools', [])
  .component('setTools', {
    template: `
      <span style="font-size:1.25rem" ng-show="vm.set.state === 'FINISHED'">
        <span class="t_tools__tool">
          <i
            class="icon-file"
            data-tooltip="{{'Export as TSV' | translate}}"
            data-ng-click="SetService.exportSet(vm.set.id)"
          ></i>
        </span>
        <span
          class="t_tools__tool"
          data-ng-if="vm.downloadEnabled && vm.set.type === 'donor'"
        >
          <i
            class="icon-download"
            data-tooltip="{{'Download Donor Data' | translate}}"
            data-ng-click="vm.downloadDonorData(vm.set.id)"
          ></i>
        </span>
        <a
          class="t_tools__tool"
          data-ng-if="vm.set.type === 'donor' || vm.set.type === 'file'"
          data-tooltip="{{'View in [[:: dataRepoTitle ]]' | translate | subDelimiters:{dataRepoTitle: vm.dataRepoTitle} }}"
          href="{{vm.set.repoLink}}"
        >
          <i class="icon-download-cloud"></i>
        </a>
        <span data-tooltip="{{'Share Saved Set' | translate}}">
          <share-icon
            data-custom-popup-disclaimer=""
            data-share-params="vm.getEntitySetShareParams(vm.set)" 
            class="t_tools__tool"
          ></share-icon>
        </span>
      </span>
    `,
    bindings: {
      set: '<',
    },
    controller: function ($scope, SetService, Settings) {
      $scope.SetService = SetService;
      this.downloadEnabled = false; 
      Settings.get().then((settings) => { this.downloadEnabled = !!settings.downloadEnabled; });

    },
    controllerAs: 'vm'
  })