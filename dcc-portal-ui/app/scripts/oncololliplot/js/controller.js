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

/* global $ */
(function() {
  'use strict';

  let module = angular.module('icgc.oncololliplot.controllers', []);

  module.controller('OncoLolliplotController', ($scope, $element, $filter, $timeout, Protein) => {
    const importDependencies = [
      import('react'),
      import('react-dom'),
      import('react-redux'),
      import('./react-components/Lolliplot'),
      import('./redux/store'),
      import('./redux/OncoLolliplot/redux'),
    ];

    Promise.all(importDependencies).then(
      ([
        React,
        { render },
        { Provider },
        Lolliplot,
        configureStore,
        { _defaultState, loadTranscript, resizeWidth },
      ]) => {
        const mutationService = transcriptId => Protein.init(transcriptId).get();

        const intialState = {
          oncoLolliplot: {
            ..._defaultState,
            mutationService,
            transcripts: $scope.transcripts,
            selectedTranscript: $scope.transcripts[0],
            filters: $scope.filters,
            displayWidth: $element.width(),
            highlightedPointId: $scope.highlightedPointId,
          },
        };

        // Create redux store
        const store = configureStore(intialState);

        // Update state on filters changes
        $scope.$watch('filters', () => {
          const {
            oncoLolliplot: { selectedTranscript, mutationService },
          } = store.getState();
          loadTranscript(store.dispatch, {
            selectedTranscript,
            mutationService,
            filters: $scope.filters,
          });
        });

        // Get container elem width
        $scope.getElementDimensions = function() {
          return $element.width();
        };

        // Dispatch resize event is new width has more than 10px difference
        $scope.$watch(
          $scope.getElementDimensions,
          function(newValue, oldValue) {
            if (Math.abs(oldValue - newValue) > 10) {
              store.dispatch(resizeWidth(newValue));
            }
          },
          true
        );

        // Resize displayWidth on window resize
        $(window).on('resize', () => {
          $scope.$apply();
        });

        render(
          <Provider store={store}>
            <Lolliplot d3={d3} />
          </Provider>,
          document.getElementById('onco-lolliplot-container')
        );
      }
    );
  });
})();
