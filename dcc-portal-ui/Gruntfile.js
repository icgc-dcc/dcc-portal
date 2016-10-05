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

// # Globbing
// for performance reasons we're only matching one level down:
// 'test/spec/{,*/}*.js'
// use this if you want to recursively match all subfolders:
// 'test/spec/**/*.js'

module.exports = function (grunt) {
  // load all grunt tasks
  require('matchdep').filterDev('grunt-*').forEach(grunt.loadNpmTasks);
  require('time-grunt')(grunt);
  var configProvider = require('./grunt/grunt-config-provider')(grunt);

  // configurable paths
  var yeomanConfig = {
    app: 'app',
    dist: 'target/classes/app',
    developIndexFile: 'develop/html/index.develop.html'
  };


  try {
    yeomanConfig.app = require('./bower.json').appPath || yeomanConfig.app;
  }
  catch (e) {
  }

  grunt.initConfig({
    'bower-install-simple': configProvider.setConfigForTask('bower-install-simple', function() {

        /**
        * Bower configuration
        * See: https://www.npmjs.com/package/grunt-bower-install-simple
        */

        var config =  {options: { color: false } };

        if (configProvider.isProductionBuild()) {
          config.prod = { options: { production: true, interactive: false, forceLatest: false } };
        }
        else {
          config.dev = { options: { production: false,  interactive: true, forceLatest: false } };
        }

        return config;
    })
    // Gets the default dev config object in this context because
    // we have yet to set a default
    .getConfigForTask('bower-install-simple'),
    peg: {
      pql: {
        src: './app/scripts/common/js/pql/conf/pql.pegjs',
        dest: './app/scripts/common/js/pql/pqlparser.js',
        options: {
          exportVar: 'PqlPegParser'
        }
      }
    },
    yeoman: yeomanConfig,
    jsdoc2md: {
      oneOutputFile: {
        src: '<%= yeoman.dist %>/scripts/scripts.js',
        dest: '<%= yeoman.dist %>/docs/api/README.md'
      }
    },
    nggettext_extract: {
      pot: {
        files: {
          'translations/strings.pot' : ['<%= yeoman.app %>/scripts/**/*.html','<%= yeoman.app %>/scripts/**/*.js'],
        }
      },
    },
    nggettext_compile: {
      all: {
        files: {
          '<%= yeoman.app %>/scripts/translations/js/translations.js': ['translations/*.po']
        }
      },
    }
  });

  grunt.registerTask('extractText', ['nggettext_extract',]);
  grunt.registerTask('compileText', ['nggettext_compile',]);
  
};