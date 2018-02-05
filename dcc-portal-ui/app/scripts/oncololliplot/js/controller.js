/*
 * Copyright 2018(c) The Ontario Institute for Cancer Research. All rights reserved.
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

(function() {
  'use strict';

  let module = angular.module('icgc.oncololliplot.controllers', []);

  module.controller('OncoLolliplotController', ($scope, $element, Protein) => {
    const transcript = $scope.transcript;
    const importDependencies = [
      import('react'),
      import('react-dom'),
      import('./Lolliplot'),
      Protein.init(transcript.id).get(),
    ];

    Promise.all(importDependencies).then(([React, ReactDOM, Lolliplot, mutations]) => {
      const renderLolliplot = (_transcript, _filters, _mutations) =>
        ReactDOM.render(
          <Lolliplot
            d3={d3}
            transcript={_transcript}
            filters={_filters} // linked
            mutations={_mutations} // will be linked
            displayWidth={900} // will be linked
            scope={$scope}
          />,
          document.getElementById('onco-lolliplot-container')
        );

      // Initial Render
      renderLolliplot(transcript, $scope.filters, mutations);

      // Re-render on facet change
      $scope.$watch('filters', () => {
        renderLolliplot(transcript, $scope.filters, mutations);
      });
    });
  });
})();
