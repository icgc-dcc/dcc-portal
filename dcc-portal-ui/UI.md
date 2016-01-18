#DCC Portal Application UI Documentation#




##Who this Document is for?##
---

* A developer, or team of developers who wish to understand the general frontend
components in order to extend, modify or provide fixes towards the UI.




##Learning Objectives##
---
* Understand the general frameworks used within the frontend.
* Be able to identify the responsibilities of various portions of the UI codebase.
* Learn to customize the DCC Banner, Logo and Footer.
* Theming the DCC Portal frontend.
* In general, gain enough knowledge from prior sections to work on your own feature enhancements in the frontend code.


##The Frontend Stack##
---

###Build###
*Please note:* The below dependendencies are managed using the [NodeJS's](https://nodejs.org) [NPM](https://www.npmjs.com/) tool. Please
refer to the *package.json* file in the current directory to lookup which versions the portal is depending on.


* NodeJS with NPM
* Grunt Tools (NodeJS) for
 * Development
    * Connect (Grunt HTTP Server)
    * Watch (File Watcher) w/LiveReload and SASS Regeneration
 * JS Linting
   * JSHint
 * Frontend Tests
   * Karma w/PhantomJS
 * Concatenation/Minification/Source Map Generation
   * Usemin/CSSmin/HTMLmin/Imagemin
   * Concat
   * Uglify
   * Filerev
   * etc...
   * Please Refer to the Frontend *Gruntfile.js* for More Details)
* SASS Preprocessor (w/Compass) - CSS Development
* Bower (Frontend Library Package Management)
 


###UI###
*Please note:* The below dependendencies are managed using the [Bower](http://bower.io/) package manager. Please
refer to the *bower.json* file in the current directory to lookup which versions of the illustrated libraries the portal is depending on.

* *[jQuery](https://jquery.com/)* - For DOM Manipulation it is used by various 3rd party dependencies and the application itself)
* *[lodash](https://lodash.com/)* - A Common JS utility library.
* *[AngularJS](https://angularjs.org/)* - Version 1.x.y - DCC Portal frontend's primary JS framework.
* *[Angular-ui-router](https://github.com/angular-ui/ui-router)*  - Route Management in AngularJS.
* *[Restangular](https://github.com/mgonto/restangular)* - REST Resource Management in AngularJS.
* *[Angular-bootstrap](https://angular-ui.github.io/bootstrap/)* Bootstrap integration in AngularJS.
* *[D3.js](https://d3js.org/)* - Visualization (Preferred) Library
* *[Highcharts](http://www.highcharts.com/)*  - Visualization Library
* *Other Various Third Party Libs*

##Frontend Breakdown##
---


##Got Questions/Feeback?##
---
 * We are always happy to help - should you have any questions or feedback
please feel free to contact us at [dcc-support@icgc.org](mailto:dcc-support@icgc.org) 