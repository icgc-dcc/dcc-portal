/**
 * Created by mmoncada on 12/16/15.
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