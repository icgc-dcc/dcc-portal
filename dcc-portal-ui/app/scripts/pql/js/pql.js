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
require('./pql.scss');

(function () {
  'use strict';

  var module = angular.module('icgc.pql', []);

  module.directive('pqlButton', function () {
    return {
      restrict: 'E',
      replace: true,
      transclude: true,
      templateUrl: '/scripts/pql/views/pql.html',
      controller: 'pqlCtrl as pqlCtrl',
      link: function ($scope) {
        $scope.entityType = 'page';
      }
    };
  });

  module.service('PQLApi', function ($state, $location, $log, FilterService, Restangular) {
    /**
     * parsePaginationParams(entityName)
     * Will pull the query fields from the current page URL and parse
     * out the values for 'sort', 'order', 'from', and 'size' currently displayed
     * 
     * param entityName: one of ['donors', 'genes', 'mutations']
     * 
     * return Object, all fields optional: {sort, order, from, size}
     */
    function parsePaginationParams(entityName) {
      const output = {};
      
      const urlQueryParams = $location.search();
      const entityParamString = urlQueryParams[entityName];
      if (entityParamString) {
        try {
          const params = JSON.parse(entityParamString);

          if (params.hasOwnProperty('size')) {
            output.size = params.size;
          }
          if (params.hasOwnProperty('from')) {
            output.from = params.from;
          }
          if (params.hasOwnProperty('order')) {
            output.order = params.order;
          }
          if (params.hasOwnProperty('sort')) {
            output.sort = params.sort;
          }
  
        } catch (e) {
          $log.warn(`Provided URL Query string is invalid JSON: ${queryString}`);
        }
      }
      return output;
    }

    /**
     * PQLService.getPQL()
     * Fetch from server the PQL syntax statement that corresponds with the current page,
     * including all filters, sorting, and pagination parameters
     * 
     * return: Promise with result of Restangular request to get PQL
     */
    this.getPQL = function () {

      const filters = FilterService.filters();
      const params = {
        queryType: 'DONOR_CENTRIC',
        size: 10,
        from: 1,
        order: 'desc',
        filters: filters,
      };

      const searchTab = $state.current.data.tab;
      switch(searchTab) {
        case 'gene':
        params.queryType='GENE_CENTRIC';
        _.merge(params, parsePaginationParams('genes'));
        break;
        case 'mutation':
        params.queryType='MUTATION_CENTRIC';
        _.merge(params, parsePaginationParams('mutations'));
        break;
        
        // donor is intentional fall-through to default
        // Default getPQL to use DONOR_CENTRIC if unspecified
        case 'donor':
        default:
          params.queryType='DONOR_CENTRIC';
          _.merge(params, parsePaginationParams('donors'));
          break;
      }

      return Restangular.all('PQL').get('', params);
    };

  });

  module.controller('pqlCtrl', function ($scope, $element, PQLApi, FilterService) {
    var _ctrl = this;
    this.query = '';
    this.changedUrl = true;
    _ctrl.toggle = function (opt) {
      _ctrl.active = opt;
    };
    this.popoverIsOpen = false;

    this.handleClickShowPQLButton = () => {
      PQLApi.getPQL()
        .then((data) => {
          this.query = data.PQL ? data.PQL : '';
        });
    };
    
    const bodyClickListener = function (e) {
      jQuery($element).each(function () {
        if ((!jQuery(this).is(e.target) && jQuery(this).has(e.target).length === 0 && jQuery('.popover').has(e.target).length === 0)) {
          _ctrl.popoverIsOpen = false;
        }
      });
    };

    jQuery('body').on('click', bodyClickListener);
    $scope.$on('$destroy', () => {
      jQuery('body').off('click', bodyClickListener);
    });
  });

})();