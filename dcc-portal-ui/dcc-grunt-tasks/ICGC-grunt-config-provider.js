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

// The purpose of the provider is to ensure that the appropriate configs
// (for those registered via setConfigForTask() public method)
// are made available for the given environment (i.e. production vs development)
module.exports = function (grunt) {

  function ICGCGruntConfigProvider(grunt) {


    var _CONFIG_CONSTANTS = {
        BUILD_ENV: {DEV: 'development', PRODUCTION: 'production'}
      },
      _currentConfigBuildEnvironment = _CONFIG_CONSTANTS.BUILD_ENV.DEV,
      _configFunctionMap = {},
      _self = this;


    function _initTasks() {

      grunt.registerTask('ICGC-setBuildEnv', 'Sets the target build environment (default: ' +
                                             _CONFIG_CONSTANTS.BUILD_ENV.DEV + ')', function (env) {
        var message = 'Setting GRUNT build environment to ';

        switch (env.toLowerCase()) {
          case _CONFIG_CONSTANTS.BUILD_ENV.PRODUCTION:
            _currentConfigBuildEnvironment = env;
            break;
          default:
            _currentConfigBuildEnvironment = _CONFIG_CONSTANTS.BUILD_ENV.DEV;
            break;
        }

        grunt.log.oklns(message + _currentConfigBuildEnvironment);

        _updateAllTaskConfigs();

      });
    }

    function _updateAllTaskConfigs() {

      // Loop through the registered tasks and ensure we have the appropriate configs for them
      // (given the current set environment _currentConfigBuildEnvironment )
      for (var taskName in _configFunctionMap) {

        if (_updateGruntConfigForTaskAndCurrentBuildEnv(taskName)) {
          grunt.log.oklns('Assigning config for task \'' + taskName +
                          '\' (Build Env: ' + _currentConfigBuildEnvironment + ')');
        }

      }
    }

    function _updateGruntConfigForTaskAndCurrentBuildEnv(taskName) {

      var currentGruntConfigForTask = grunt.config(taskName);

      if (typeof currentGruntConfigForTask === 'undefined') {
        return false;
      }

      var configuration = _self.getConfigForTask(taskName);

      if (typeof configuration !== 'undefined' && configuration !== null) {
        grunt.config(taskName, configuration);
      }

      return true;
    }

    function _init() {
      _initTasks();
    }

    _init();


    // Public APIs
    this.getConfigForTask = function (taskName) {
      var config = {},
        task = typeof taskName === 'string' ? taskName : null;

      if (typeof _configFunctionMap[task] === 'undefined' || task === null) {
        return null;
      }

      config = _configFunctionMap[task].call(_self);

      return config;
    };

    this.setConfigForTask = function (taskName, config) {

      if (typeof taskName !== 'string' || config === null) {
        return null;
      }

      var task = taskName;

      // Handle the different "config" parameter types...
      switch (typeof config) {
        case 'function':
          _configFunctionMap[task] = config;
          break;
        case 'object':
          _configFunctionMap[task] = function () {
            return config;
          };
          break;
        default:
          break;
      }

      return this;
    };

    this.isProductionBuild = function () {
      return _currentConfigBuildEnvironment === _CONFIG_CONSTANTS.BUILD_ENV.PRODUCTION;
    };


    return this;
  }

  return new ICGCGruntConfigProvider(grunt);

};