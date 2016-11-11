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

'use strict';


/**
 * Renders gene-ontology inferred tree digram and reactome pathway hierarchies.
 *
 * Note:
 * Cannot use a recursive template, partially because angularJS caps the number of digest cycles
 * to 10. While this can be modified (albeit globally), it may bring other problems...
 *
 */
angular.module('icgc.ui.trees', []).directive('pathwayTree', function($compile) {
  return {
    restrict: 'E',
    replace: true,
    scope: {
      tree: '='
    },
    template: '<div class="tree"></div>',
    link: function($scope, $element) {
      // D3 would've been easier...
      function addNesting(e, current) {
        var ul, li, span, anchor;
        ul = angular.element('<ul>');
        li = angular.element('<li>');
        span = angular.element('<span>');

        if (current.children) {
          anchor = angular.element('<a>').attr('data-ng-href', '/genesets/' + current.id).text(current.name);
          anchor.appendTo(span);
        } else {
          angular.element('<strong>').text(current.name).appendTo(span);
        }


        span.appendTo(li);
        li.appendTo(ul);
        ul.appendTo(e);

        if (current.children && current.children.length > 0) {
          current.children.forEach(function(child) {
            addNesting(li, child);
          });
        }
      }

      $scope.tree.forEach(function(child) {
        addNesting($element[0], child);
      });


      // Dynamically generated contents need compilation
      $compile($element.contents())($scope);

    }
  };
}).directive('inferredTree', function($compile, gettextCatalog) {
  return {
    restrict: 'E',
    replace: true,
    scope: {
      tree: '='
    },
    template: '<div><span data-ng-click="toggle()">' +
      '<i data-ng-class="{\'icon-check-empty\':!showID, \'icon-check\':showID}"></i> ' + 
      gettextCatalog.getString('Show GO IDs') + '</span>' +
      '<div class="tree"></div></div>',
    link: function($scope, $element) {

      $scope.toggle = function() {
        $scope.showID = !$scope.showID;
      };

      // If grand child exist, this is inferred.
      function getRelation(relation, hasGrandChild) {
        var element = angular.element('<abbr>').attr('data-tooltip-placement', 'left');

        if (relation === 'is_a') {
          return element.text('I ')
            .attr('data-tooltip', 
              hasGrandChild? gettextCatalog.getString('Inferred is a') : gettextCatalog.getString('Is a'))
            .attr('class', 'goterm_is_a');
        } else if (relation === 'part_of') {
          return element.text('P')
            .attr('data-tooltip', 
              hasGrandChild? gettextCatalog.getString('Inferred part of') : gettextCatalog.getString('Part of'))
            .attr('class', 'goterm_part_of');
        } else if (relation === 'regulates') {
          return element.text('R')
            .attr('data-tooltip', 
              hasGrandChild? gettextCatalog.getString('Inferred regulates') : gettextCatalog.getString('Regulates'))
            .attr('class', 'goterm_regulates');
        } else if (relation === 'positively_regulates') {
          return element.text('R')
            .attr('data-tooltip', 
              hasGrandChild? gettextCatalog.getString('Inferred positively regulates') : gettextCatalog.getString('Positively regulates'))
            .attr('class', 'goterm_positively_regulates');
        } else if (relation === 'negatively_regulates') {
          return element.text('R')
            .attr('data-tooltip', 
              hasGrandChild? gettextCatalog.getString('Inferred negatively regulates') : gettextCatalog.getString('Negatively regulates'))
            .attr('class', 'goterm_negatively_regulates');
        } else if (relation === 'self') {
          return element.text('');
        } else {
          return element.text('U')
            .attr('data-tooltip', gettextCatalog.getString('Unknown - Not possible to infer relation'))
            .attr('class', 'goterm_unknown');
        }
      }

      function addNesting(e, current) {
        var ul, li, span, anchor, relation, label, hasGrandChild = false;

        ul = angular.element('<ul>');
        li = angular.element('<li>');
        span = angular.element('<span>');

        if (current.child && current.child.child) {
          hasGrandChild = true;
        }

        current.goTerms.forEach(function(goTerm) {
          label = ' {{showID? "' + goTerm.id + ' ' + goTerm.name + '":"' + goTerm.name + '"}}';
          anchor = angular.element('<a>').attr('data-ng-href', '/genesets/' + goTerm.id).text(label);
          relation = getRelation(goTerm.relation, hasGrandChild);

          if (relation) {
            relation.appendTo(span);
          }

          if (goTerm.relation === 'self') {
            angular.element('<strong>').text(label).appendTo(span);
          } else {
            anchor.appendTo(span);
          }

          angular.element('<br>').appendTo(span);
        });

        span.appendTo(li);
        li.appendTo(ul);
        ul.appendTo(e);

        if (current.child) {
          addNesting(li, current.child);
        }
      }

      addNesting($element.find('div')[0], $scope.tree);

      // Dynamically generated contents need compilation
      $compile($element.find('div').contents())($scope);

    }
  };
});

