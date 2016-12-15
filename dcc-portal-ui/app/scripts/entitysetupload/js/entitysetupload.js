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

  angular.module ('icgc.entitySetUpload', [
    'icgc.entitySetUpload.controllers',
    'icgc.entitySetUpload.services'
  ]);
})();

(function() {
  'use strict';
  
  angular.module ('icgc.entitySetUpload.services', [])
  .service ('EntitySetUploadVerificationService', function (Restangular, SetService) {
    const _service = this;

    _service.readFileContent = (filepath) => {
      let data = new FormData();
      data.append('filepath', filepath);

      return Restangular.one('ui').customPOST(data, 'search/file', {}, {'Content-Type': undefined});
    }

    _service.addSet = (entityIds, entityType) => {
      let params = {};

      params.filters = {};
      params.filters[entityType] = {id : {is : entityIds }};
      params.isTransient = true;
      params.type = entityType;
      params.size = entityIds.length;
      params.name = `Uploaded ${entityType} set`;
      return SetService.addSet(entityType, params, entityType === 'file');
    }
  });

})();

(function () {
  'use strict';

  const mutationIdRegEx = new RegExp(/\bMU([1-9])\d*\b/g);
  const fileIdRegEx = new RegExp(/\bFI([1-9])\d*\b/g);

  angular.module ('icgc.entitySetUpload.controllers', [])
    .controller ('EntitySetUploadController', function ($scope, $modalInstance, EntitySetUploadVerificationService,
      LocationService, entityType) {

      const _controller = this;

      _controller.entityUploadService = EntitySetUploadVerificationService;

      $scope.params = {};
      $scope.params.entityType = entityType;

      $scope.cancel = function () {
        $modalInstance.dismiss('cancel');
      };

      _controller.fileUpload = () => {
        if($scope.params.uploadedFile){
          $scope.params.fileName = $scope.params.uploadedFile.name;
          _controller.entityUploadService.readFileContent($scope.params.uploadedFile).then((fileData) => {
            $scope.params.entityIds = fileData.data;
            $scope.verifyInput();
          });
        }
      };

      $scope.verifyInput = () => {
        // Total number of user added entity IDs
        $scope.params.entityIdsTotal = $scope.params.entityIds.split(/\W+/g).length;

        /**
         *  We should be verfying the entity IDs against an API endpoint
         *  currently the endpoint doesnt exist so checking against a RegEx
         * */ 
        $scope.params.entityIdsArray = _.words($scope.params.entityIds, $scope.params.entityType === 'file' ? fileIdRegEx : mutationIdRegEx);

        if(!$scope.params.entityIdsArray.length) {
          $scope.params.verified = false;
        } else {
          $scope.params.verified = true;
        }
      }

      $scope.submit = () => {
        if(!$scope.params.entityIds || !$scope.params.verified) {return;}

        let filters = LocationService.filters();

        _controller.entityUploadService.addSet($scope.params.entityIdsArray, $scope.params.entityType).then((set) => {
          filters = _.set (filters, [$scope.params.entityType, 'id', 'is'], [`ES:${set.id}`]);
          LocationService.filters(filters);
        });
      };

      $scope.$watch('params.uploadedFile', function (newValue) {
        if (!newValue) {return;}
        _controller.fileUpload();
      });
  });
})();
