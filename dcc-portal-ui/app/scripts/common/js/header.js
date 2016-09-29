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

  var moduleNamespace = 'app.common.header';
  var controllersNamespace = moduleNamespace + '.controllers';
  var controllerName = 'MainHeaderController';

  angular.module (moduleNamespace, [controllersNamespace]);
  angular.module (controllersNamespace, [])
    .controller (controllerName, ['RouteInfoService', 'Settings', function (RouteInfoService, Settings) {

    var _ctrl = this;

    _ctrl.mirror = {};

    function styleClass (name) {
      return 't_nav__items__item__' + name;
    }

    var items = _.map (['home', 'projects', 'advancedSearch', 'dataAnalysis', 'dataReleases', 'dataRepositories'],
      RouteInfoService.get);
    var itemAttributes = [{
        icon: 'icon-home',
        styleClass: styleClass ('home')
      }, {
        icon: 'icon-list',
        styleClass: styleClass ('projects')
      }, {
        icon: 'icon-search',
        styleClass: styleClass ('advanced')
      }, {
        icon: 'icon-beaker',
        styleClass: styleClass ('analysis')
      }, {
        icon: 'icon-database',
        styleClass: styleClass ('download')
      }, {
        icon: 'icon-download-cloud',
        styleClass: styleClass ('data_repositories')
      }];
    /*
     * Since _.zipWith was introduced in lodash 3.8.0 and we're still using 3.7.x, this is
     * my poor man's implementation. Once we upgrade lodash, all this can be reduced to one line:
     * var menuItems = _.zipWith (items, itemAttributes, _.assign);
     */
    var menuItems = _.map (_.zip (items, itemAttributes), function (itemPair) {
      return _.reduce (itemPair, function (result, item) {
        return _.assign (result, item);
      }, {});
    });

    Settings.get().then(function(setting){
      _ctrl.mirror = setting.mirror;
    });

    _ctrl.menuItems = menuItems;
  }]);

})();
