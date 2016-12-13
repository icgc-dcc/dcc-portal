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

(function() {
    'use strict';

    function PortalFeatureConstructor(features, $state, LocationService, Settings) {


      function _enable(feature) {
        if (features.hasOwnProperty(feature) === false) { return }
        features[feature] = true;
        if ($state.current.name) {
          $state.go($state.current.name, {}, {reload: true});
        }
      }

      function _disable(feature) {
        if (features.hasOwnProperty(feature) === false) { return }
        features[feature] = false;
        if ($state.current.name) {
          $state.go($state.current.name, {}, {reload: true});
        }
      }


      function init(settingsJson) {
        for (var featureName in settingsJson.featureFlags) {
          if (settingsJson.featureFlags[featureName] === true) {
            _enable(featureName);
          } else if (settingsJson.featureFlags[featureName] === false) {
            _disable(featureName);
          }
        }

        // Allow features to be turned on via query param on application load
        var enable = LocationService.getParam('enable');
        if (typeof enable !== 'undefined') {
          enable.split(',').forEach(function(feature) {
            _enable(feature.trim());
          });
        }

      }

      Settings.get().then(init);

      this.get = function(s) {
        if (features.hasOwnProperty(s) === false) { return false }
        return features[s];
      };

      this.enable = function(s) {
        _enable(s);
      };

      this.disable = function(s) {
        _disable(s);
      };

      this.list = function() {
        return features;
      };
  }

  /**
   * This serves as a debugging service to toggle features that are merged
   * but disabled.
   *
   * Note: This works automatically for views that are tied to a state, otherwise
   * it will be up to the callers to check for state change via watch/observe or other means.
   */
  angular.module('icgc.portalfeature', [])
    .provider('PortalFeature', function() {

       var _enabledFeatures = {
          AUTH_TOKEN: true,
          ICGC_GET: true,
       };

      this.hasFeature = function(featureID) {
        return _.get(_enabledFeatures, featureID, false);
      };

      this.$get = ['$state', 'LocationService', 'Settings', function($state, LocationService, Settings) {
          return new PortalFeatureConstructor(_enabledFeatures, $state, LocationService, Settings);
      }];
  });



})();
