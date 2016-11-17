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

angular.module('app.ui.tpls', [
  'template/tsize.html',
  'template/sortable.html',
  'template/pagination.html',
  'template/dialog/message.html',
  'template/tooltip.html',
  'template/lists'
]);



angular.module('template/lists', []).run(function ($templateCache, gettextCatalog) {

  $templateCache.put('template/mutationTranscriptList.html',
    '<a target="_blank" ' +
    'data-ng-href="http://feb2014.archive.ensembl.org/Homo_sapiens/Transcript/Summary?db=core;t={{item.id}}">' +
    '<i class="icon-external-link"></i><span data-ng-bind-html="item.name | highlight: highlightFilter"></span></a>');


  $templateCache.put('template/compoundList.html', '<span data-ng-bind-html="item.name | highlight: highlightFilter"></span>');
  // Display a list of gene set annotations
  $templateCache.put('template/geneGeneSetList.html',
    '<ul data-ng-if="item.qualifiers">' +
    '<li data-ng-repeat="qualifier in item.qualifiers">' +
    '<span>{{qualifier | trans }} <a href="genesets/{{item.id}}"><span>{{item.name}}</span></a></span>' +
    '</li>' +
    '</ul>' +
    '<span data-ng-if="!item.qualifiers">' +
    '<a href="genesets/{{item.id}}"><span>{{item.name}}</span></a>' +
    '</span>');

  $templateCache.put ( 'template/reactomePathway.html',
    '<span><a href="http://www.reactome.org/PathwayBrowser/#DIAGRAM={{item.diagramId}}&ID={{item.geneSetId}}"' +
    ' target="_blank">' +
    '<i class="icon-external-link"></i> ' + gettextCatalog.getString('View in Reactome Pathway Browser') + '</a>' +
    '</span><pathway-tree tree="[item.root]"></pathway-tree><br>'
  );
});


angular.module('template/tooltip.html', []).run(function ($templateCache) {
  $templateCache.put('template/tooltip.html',
    '<div class="tooltip {{placement}} in fade" style="top:-999px; left:-999px; display:block">\n' +
    '  <div class="tooltip-arrow"></div>\n' +
    '  <div class="tooltip-inner" data-ng-bind-html="html"></div>\n' +
    '</div>\n' +
    '');
});


angular.module('template/tsize.html', []).run(function ($templateCache) {
  $templateCache.put('template/tsize.html',
    '<select style="width:100px; margin-bottom:5px" ng-options="size +\' rows\' for size in sizes"' +
    ' ng-model="selectedSize" ng-change="update()"></select>'
  );
});

angular.module('template/sortable.html', []).run(function ($templateCache) {
  $templateCache.put('template/sortable.html',
    '<span ng-click="onClick()" style="cursor: pointer"><span ng-transclude></span> ' +
    '<i ng-if="!active" style="color:hsl(0,0%,80%)" class="icon-sort"></i>' +
    '<i ng-if="active" ng-class="{\'icon-sort-down\': reversed, \'icon-sort-up\':!reversed}"></i></span>');
});

angular.module('template/pagination.html', []).run(function ($templateCache) {
  $templateCache.put('template/pagination.html',
    '<div style="margin-top: 1rem"><div ng-if="data.hits.length"><span data-table-size data-type="{{ type }}" ' +
    'data-current-size="{{data.pagination.size}}"></span>' +
    '<span ng-if="data.pagination.pages > 1" class="pull-right">' +
    '<div><ul class="pagination">' +
    '<li ng-repeat="page in pages" ng-class="{active: page.active, disabled: page.disabled}">' +
    '<a ng-click="selectPage(page.number)" tooltip="{{:: page.tooltip }}">{{page.text}}</a></li>' +
    '</ul></div>' +
    '</span></div></div>'
  );
});

angular.module('template/dialog/message.html', []).run(['$templateCache', function ($templateCache) {
  $templateCache.put('template/dialog/message.html',
    '<div class=\"modal-dialog\">' +
    '<div class=\"modal-content\">' +
    '<div class=\"modal-header\">' +
    '	<h1>{{ title }}</h1>' +
    '</div>' +
    '<div class=\"modal-body\">' +
    '	<p>{{ message }}</p>' +
    '</div>' +
    '<div class=\"modal-footer\">' +
    '	<button ng-repeat=\"btn in buttons\" ng-click=\"close(btn.result)\"' +
    'class=btn ng-class=\"btn.cssClass\">{{ btn.label }}</button>' +
    '</div></div></div>' +
    '');
}]);
