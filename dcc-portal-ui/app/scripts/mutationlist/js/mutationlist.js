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

  // Angular wiring
  angular.module ('icgc.mutationlist.services', [])
  .service ('MutationSetVerificationService', function () {
    
  });

})();

(function () {
  'use strict';

  angular.module ('icgc.mutationlist.controllers', [])
    .controller ('MutationListController', function ($scope, $modalInstance,
      SetService, LocationService) {

      let filters = LocationService.filters();

      $scope.mutationSets = _.cloneDeep(SetService.getAllMutationSets());
      $scope.isSelect = false;

      // to check if a set was previously selected and if its still in effect
      const checkSetInFilter = () => {
        if(filters.mutation && filters.mutation.id){
          _.each(filters.mutation.id.is, (id) => {
            if(_.includes(id,'ES')){
              const set = _.find($scope.mutationSets, function(set){
                return `ES:${set.id}` === id;
              });
              if(set){
                set.selected = true;
              }
            }
          })
        }
      };

       $scope.cancel = function () {
        $modalInstance.dismiss('cancel');
      };

      checkSetInFilter();
  });
})();
