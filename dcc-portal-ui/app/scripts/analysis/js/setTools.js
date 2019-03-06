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
            data-ng-click="vm.downloadSetData(vm.set.id)"
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
    controller: function (
      $scope,
      SetService,
      Settings,
      $modal,
      Extensions,
      RouteInfoService,
      LocationService
    ) {
      $scope.SetService = SetService;
      this.downloadEnabled = false; 
      Settings.get().then((settings) => { this.downloadEnabled = !!settings.downloadEnabled });

      this.downloadSetData = (setId) => $modal.open({
        templateUrl: '/scripts/downloader/views/request.html',
        controller: 'DownloadRequestController',
        resolve: {
          filters: () => ({donor:{id:{is:[Extensions.ENTITY_PREFIX + setId]}}})
        }
      });

      this.dataRepoTitle = RouteInfoService.get('dataRepositories').title;

      this.getEntitySetShareParams = _.memoize((item) => {
        var base = item.type === 'file' ? 'repositories' : 'search';
        return {
          url: LocationService.buildURLFromPath(base + (item.advType !== '' ? ('/' +  item.advType) : '')),
          filters: JSON.stringify(item.advFilters),
        };
      });
    },
    controllerAs: 'vm'
  });