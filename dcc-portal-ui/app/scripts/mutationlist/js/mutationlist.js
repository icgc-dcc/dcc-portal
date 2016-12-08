/*
 * Copyright 2016(c) The Ontario Institute for Cancer Research. All rights reserved.
 *
 * This program and the accompanying materials are made available under the terms of the GNU Public
 * License v3.0. You should have received a copy of the GNU General Public License along with this
 * program. If not, see <http://www.gnu.org/licenses/>.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY
 * WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

import deepmerge from 'deepmerge';

(function() {
  'use strict';

  angular.module ('icgc.mutationlist', [
    'icgc.mutationlist.controllers',
    'icgc.mutationlist.services'
  ]);
})();

(function() {
  'use strict';
  
  angular.module ('icgc.mutationlist.services', [])
  .service ('MutationSetVerificationService', function (Restangular, SetService, LocationService) {
    const _service = this;

    _service.readFileContent = (filepath) => {
      let data = new FormData();
      data.append('filepath', filepath);

      return Restangular.one('ui').customPOST(data, 'search/file', {}, {'Content-Type': undefined});
    }

    _service.addSet = (mutationIds) => {
      let params = {};
      const type = 'mutation';

      params.filters = {mutation: {id : {is : mutationIds }}};
      params.isTransient = true;
      params.type = type;
      params.size = mutationIds.length;
      params.name = 'Uploaded Mutation Set';

      return SetService.addSet(type, params);
    }
  });

})();

(function () {
  'use strict';

  const mutationIdRegEx = new RegExp(/MU([1-9])\d*/g);

  angular.module ('icgc.mutationlist.controllers', [])
    .controller ('MutationListController', function ($scope, $modalInstance, MutationSetVerificationService,
      LocationService) {

      const _controller = this;

      _controller.mutationService = MutationSetVerificationService;
      _controller.mutationIdsArray = [];

      $scope.params = {};

      $scope.cancel = function () {
        $modalInstance.dismiss('cancel');
      };

      _controller.fileUpload = () => {
        if($scope.params.uploadedFile){
          $scope.params.fileName = $scope.params.uploadedFile.name;
          _controller.mutationService.readFileContent($scope.params.uploadedFile).then((fileData) => {
            $scope.params.mutationIds = fileData.data;
          });
        }
      };

      $scope.submit = () => {
        if(!$scope.params.mutationIds) {return;}

        _controller.mutationIdsArray = _.words($scope.params.mutationIds, mutationIdRegEx);

        if(!_controller.mutationIdsArray.length) {
          $scope.params.verified = false;
          return;
        }
        let filters = LocationService.filters();

        _controller.mutationService.addSet(_controller.mutationIdsArray).then((set) => {
          filters = deepmerge(filters, {mutation: {id: {is: [`ES:${set.id}`]}}});
          debugger;
          LocationService.filters(filters);
        });
      };

      $scope.$watch('params.uploadedFile', function (newValue) {
      if (!newValue) {return;}
      _controller.fileUpload();
    });
    

  });
})();
