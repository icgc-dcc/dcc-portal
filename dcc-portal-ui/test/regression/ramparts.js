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

var phantom = require('phantom');
var _ = require('underscore');
var assert = require('assert');
var system = require('system');


var usage = "Usage: node ramparts.js <origin_url> <validation_url> <route> [follow_links]\n";


/** 
 * Command line parameters 
 *   originBaseURL - base server
 *   validationBaseURL - server to validate
 *   route - url route
 *   followLinks - 1 to collapse duplicated links, 0 to check everything
 */
var args = process.argv;
var originBaseURL = args[2] || process.stderr.write(usage) && process.exit();
var validationBaseURL = args[3] || process.stderr.write(usage) && process.exit();
var route = args[4] || process.stderr.write(usage) && process.exit();
var followLinks = args[5] || 1;


console.log('\n\nRunning', originBaseURL, validationBaseURL, route, followLinks);



/**
 * Returns basic information about the page
 */
function discovery(selector) {

  console.log('Data discovery ... ');

  // Toggle collapsed elements so they can be scraped
  var hidden = document.querySelectorAll('.icon-caret-left');
  for (var i=0; i < hidden.length; i++) { 
    $(hidden[i]).click();
  }


  // Toggle and expose all facet terms to DOM
  // Open facets, and expand all terms
  var activeTerms = [];
  var inactiveTerms = [];

  var facets = $('ul.t_facets__facet');

  for (var i=0; i < facets.length; i++) {
    var facetName = $(facets[i]).find('.t_facets__facet__title__label')[0].textContent.trim();
    var toggle = $(facets[i]).find('.t_facets__facet__title__label i');

    if (toggle.hasClass('icon-caret-right')) {
      toggle.click();
    }

    var hasMore = $(facets[i]).find('.t_sh__toggle i');
    if (hasMore.hasClass('icon-caret-down')) {
      hasMore.click();
    }

    var actives = $(facets[i]).find('.t_facets__facet__terms__active__term__count').not('.t_facets__facet__term__na');
    var activeLabels = $(facets[i]).find('.t_facets__facet__terms__active__term__label__text');

    var inactives = $(facets[i]).find('.t_facets__facet__terms__inactive__term__count').not('.t_facets__facet__term__na'); 
    var inactiveLabels = $(facets[i]).find('.t_facets__facet__terms__inactive__term__label');

    if (activeLabels.length > 0) {
      for (var i2=0; i2 < activeLabels.length; i2++) {
        var label = activeLabels[i2].textContent.trim();
        var count = '-'; 
        
        if (actives[i2]) {
          count = actives[i2].textContent.trim();
          count = count.replace(/\s+/, '').replace(/,/g, '');
        }


        activeTerms.push({
          facet: facetName,
          term: label,
          count: count
        });
      }
    }
    
    
    if (inactiveLabels.length > 0) {
      for (var i3=0; i3 < inactiveLabels.length; i3++) {
        var label = inactiveLabels[i3].textContent.trim();
        var count = '';
        
        if (inactives[i3]) {
          count = inactives[i3].textContent.trim();
          count = count.replace(/\s+/, '').replace(/,/g, '');
        }

        inactiveTerms.push({
          facet: facetName,
          term: label,
          count: count
        });
      }
    }
  }

  // Gather links
  var links = document.getElementsByTagName('a');
  links = Array.prototype.map.call(links,function(link){
    return {
      href: link.getAttribute('href'),
      value: link.text
    }
  });


  var count = null;
  if (selector) {
    count = document.querySelectorAll(selector)[0].textContent;
    count = count.replace(/\n/, '').replace(/\s+/, '').replace(/,/g, '');
  }

  console.log('Found ', activeTerms.length, ' active terms, ', inactiveTerms.length, ' inactive terms, ', links.length + ' links');

  return {
     activeTerms: activeTerms,
     inactiveTerms: inactiveTerms,
     links: links,
     count: count,
     innerText: document.querySelectorAll('.wrap')[0].innerText
  };
}


/**
 * Check the page content against the validation server's page content
 * This can be flaky, in particluar if there are some randomness involved
 * in the page rendering
 */
function validateInnerText(route, innerText) {
  phantom.create('--ignore-ssl-errors=yes', function(ph) {
    ph.createPage(function(page) {

      function validate(result) {
        var identical = result.innerText === innerText;
        console.log('*** Check page content is identical ... ', identical?'OK':'ERROR');
        ph.exit();
      }

      page.open(validationBaseURL + route, function(status) {
        setTimeout(function() {
          page.evaluate(discovery, validate); 
        }, 5500);
      });
    });
  });
}



/**
 * Given active and inactive facet terms, check that the remote server
 * also has these terms and term counts
 */
function validateFacets(route, activeTerms, inactiveTerms) {
  phantom.create('--ignore-ssl-errors=yes', function(ph) {
    ph.createPage(function(page) {

      function validate(result) {

        // Active facets should be consistent across servers
        activeTerms.forEach(function(term) {
          var match = _.find(result.activeTerms, function(t) { return t.facet === term.facet && t.term === term.term; });
          console.log('Facet:', term.facet, term.term, term.count, match.count, '\t\t', match.count === term.count? 'OK' : 'ERROR');
        });

        // Inactive facets should be consistent across servers
        inactiveTerms.forEach(function(term) {
          var match = _.find(result.inactiveTerms, function(t) { return t.facet === term.facet && t.term === term.term; });
          console.log('Facet:', term.facet, term.term, term.count, match.count, '\t\t', match.count === term.count? 'OK' : 'ERROR');
        });

        // Check active facets
        // Not all facets are created equal, there are 2 edge cases
        //  - ID facets, these haave no counts per se. 
        //  - One-to-many counts, these counts are typically higher than the page results
        // whiteList are the facets that have one-to-one relations
        var whiteList = ['Type', 'Curated Gene Set', 'Primary Site', 'Project', 'Gender', 'Tumour Stage', 'Vital Status', 'Disease Status', 'Relapse Type', 'Age at Diagnosis'];

        if (result.count) {
          var activeGroups = _.groupBy(activeTerms, function(d) { return d.facet; });

          Object.keys(activeGroups).forEach(function(key) {
            var facet = activeGroups[key];
            var facetCount  = 0;
            facet.forEach(function(t) {
              if (t.count === '-') {
                facetCount += 1;
              } else {
                facetCount += +t.count;
              }
            });

            if (whiteList.indexOf(key) >= 0) {
              console.log('Active facet vs page result:', key, facetCount, result.count, facetCount === +result.count? 'OK' : 'ERROR');
            } else {
              console.log('Active *facet* vs page result:', key, facetCount, result.count, facetCount >= +result.count? 'OK' : 'ERROR');
            }
          });

        }
        ph.exit();
      }

      page.open(validationBaseURL + route, function(status) {
        setTimeout(function() {
          if (route.indexOf('/projects') >= 0 || route.indexOf('/search') >= 0) {
            page.evaluate(discovery, validate, _getSelector(route)); 
          } else {
            page.evaluate(discovery, validate); 
          }
        }, 3000);
      });
    })
  });
}


function _getSelector(href) {
  var selector = '';
  if (href.indexOf('/search') >= 0) {
    selector = '#adv-donor-count';
    if (href.indexOf('/search/m') >= 0)  {
      selector = '#adv-mutation-count';
    } else if (href.indexOf('/search/g') >= 0) {
      selector = '#adv-gene-count';
    }
  } else if (href.indexOf('/projects/details') >= 0) {
    selector = '#projects-project-count';
  } else if (href.indexOf('/projects/history') >= 0) {
    selector = '#projects-project-count';
  } else if (href.indexOf('/projects/summary') >= 0) {
    selector = '#projects-project-count';
  } else if (href.indexOf('/projects') >= 0) {
    selector = '#projects-project-count';
  }
  return selector;
}


function validateLink(href, value) {

  // Normalize
  value = value.replace(/,/g, '');
  value = parseInt(value, 10);
  if (href.charAt(0) !== '/') href = '/' + href;
  if (isNaN(value)) return;

  phantom.create('--ignore-ssl-errors=yes', function(ph) {

    function evaluate(selector) {
      var res = null;
      res = document.querySelectorAll(selector)[0].textContent;
      res = res.replace(/\n/, '').replace(/\s+/, '').replace(/,/g, '');
      res = parseInt(res, 10);
      return res;
    }
  
    function validate(result) {
      console.log(href, value, result, '\t\t', value===result? 'OK':'ERROR');
      ph.exit();
    }

    var selector = _getSelector(href);

    // Reroute so we can get the total results
    if (href.indexOf('/projects') >= 0) {
      var t = href.split('?');
      href = '/projects/details' + (t[1]? ('?'+t[1]) : '');
    }

    ph.createPage(function(page) {
      page.open(validationBaseURL + href, function(status) {
        setTimeout(function() {
          page.evaluate(evaluate, validate, selector);
        }, 3000);
      });
    })

  });
}



phantom.create('--ignore-ssl-errors=yes', function(ph) {

  function validate(result) {
    var links = _.filter(result.links, function(d) {
      var value = d.value;
      value = value.replace(/,/g, '');
      value = parseInt(value, 10);

      return d.href !== '' &&            // not buttons
        isNaN(value) === false &&        // is numerical
        d.href.indexOf('http') === -1 &&   // is internal
        d.href.indexOf('https') === -1;    // is internal
    });

    // Remove duplicates
    links = _.unique(links, function(l) {
      return l.href + '::' + l.value;
    });

    console.log('Running cross validation:', validationBaseURL + route);

    // Dispatch innerText validation
    validateInnerText(route, result.innerText);
    
    // Dispatch facet validation
    validateFacets(route, result.activeTerms, result.inactiveTerms);

    // Dispatch link validation
    var batchCounter = 0;
    var batchSize = 3;

    while(followLinks > 0) {
      (function(batchCounter) {
        setTimeout(function() {
          for (var ii = (batchCounter * batchSize); ii < ((batchCounter+1) * batchSize); ii++) {
            if (ii < links.length) {
              validateLink(links[ii].href, links[ii].value);
            }
          }
        }, batchCounter * 7000);
      })(batchCounter);

      if (batchCounter * batchSize >= links.length) {
        break;
      }
      batchCounter ++;
    }

    ph.exit();
  }



  return ph.createPage(function(page) {
    page.set('onConsoleMessage', function(msg) {
      // console.log('-- ' + msg);
    });

    page.set('onError', function(msg, trace) {
      console.log('page error', msg);
    });

    _page = page;

    return page.open(originBaseURL + route, function(status) {
      console.log('waiting to process', originBaseURL + route);
      setTimeout(function() {
        console.log('evaluating...');
        page.evaluate(discovery, validate); 

     }, 4000);

    });
  });
});
