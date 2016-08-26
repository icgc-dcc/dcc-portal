# ICGC DCC - Portal UI Internationalization

## Overview

For the Internationalization of ICGC DCC Portal we are using [angular-gettext](https://github.com/rubenv/angular-gettext). This tool extracts the translatable strings from the application
and adds them into a standard `.pot` template file. [Development](#development) section explains more detail into how to setup the tool into work enviornment. once the `.pot` has been
generated, the translator can use it as a resource and translate the strings. Please look at the [Translation](#translation) section for more details. The translation should crate a new
`.po` file which would then be used to compile strings and translations into a JavaScript file. 

## Development

These are the instruction on how to setup angular-gettext, integrate into the application. For the English version Portal is using already generated `strings.en_CA.po` 
and compiled translation `app/scripts/translations/js/translations.js` file. Any string changes should be done on `.po` file. Please look at [Integration](#integration) section for more
details.

### Setup

- Initial setup for angular-gettext has been done for Portal. You can just run `bower install` command from the root directory of **dcc-portal-ui** and it will install needed module to start developing. Below information is just for knowledge. 

- Install the angular-gettext plugin

	```
	bower install angular-gettext
	```

- Include the source files into the app

	```
	<script src="bower_components/angular-gettext/dist/angular-gettext.min.js"></script>
	```

- Add a dependency to angular-gettext in the Angular app:
	```
	angular.module('myApp', ['gettext']);
	```

### Annotations

The strings in ICGC DCC POrtal have been annoted three ways:

- Static strings in html are wrapped around using 'translate' directive. The 'translate'
directive itself might have 'translate-comment' attribute which gives more information
about the string to a translator. There are some cases where we are marking the directive
with an 'has-markup' attribute which tells the developer that there is HTML code inside the directive.

	```
	<translate>Hello!</translate>
	<translate translate-comment="name expands to the user name">Hello {{name}}!</translate>
	<translate has-markup>
	  <a href="/team">Team</a>
	</translate>
	```

- Strings in HTML attributes such as tooltips and labels are annoted using 'translate' filter

	```
	<input type="text" placeholder="{{'Username'|translate}}" />
	```

- Javascripts string can be translated using gettextCatalog.getString() function. You can inject
gettextCatalog as a dependency.

	```
	angular.module("myApp").controller("helloController", function (gettextCatalog) {
      var translated = gettextCatalog.getString("Hello");
    });
	```

### Extracting Strings

Strings can be extracted using different build tools like Grunt, Gulp and webpack. However, Grunt
is the only one officially supported by the angular-gettext community. The build tool will extract
the strings in a `.pot` file: a standard gettext template that lists all the strings that should be
translated. Grunt plugin is already configured for the portal. You will only need to run extract task `grunt extractText` and compile task `grunt compileText` once you have the desired translation. Below information is just for your knowledge. 

- Install the Grunt plugin [grunt-angular-gettext](https://www.npmjs.com/package/grunt-angular-gettext)

	```
	npm install grunt-angular-gettext
	```

- Once installed, load the module in the Grunt file

	```
	grunt.loadNpmTasks('grunt-angular-gettext');
	```

- The plugin provides two task types: extracting and compiling strings. To extract
strings use `nggettext_extract` task in Gruntfile, add configure it

	```
	nggettext_extract: {
	  pot: {
	    files: {
		  'translations/strings.pot' : ['/scripts/**/*.html','/scripts/**/*.js'],
		}
	  },
	}
	```

### Translating Strings

Please look at the [Translation](#translation) section for more details.

### Compiling translations

Once the strings are converted and save in a `.po` file and use the same Grunt plugin to 
compile it in a JavaScript file. You will only need to run compile task as the configuration for it has already been done in Portal. Run the task from command line `grunt compileText`

- Create a new `nggettext_compile` task in the Gruntfile. Point to the location where 
the compiled JavaScript should be saved.

	```
	nggettext_compile: {
	  all: {
	    files: {
		  '/scripts/translations/js/translations.js' : ['translations/*.po'],
		}
	  },
	}
	```

- Once the strings are compiled in a JavaScript file. Make sure to add it to the `index.html
file of the project

	```
	<script src="scripts/translations/js/translations.js"></script>
	```

### Applying the translation

Please set the language you would like to use in the `app.js`. Makesure to use proper 
language code. This code can be found in the `.po` file header.

	
	app.run(function(gettextCatalog) {
	  gettextCatalog.setCurrentLanguage('fr_CA');
	}
	
### Integration

- Any text changes should be done on templates or .po files based on type of the change.

- Addition of strings or text would need to be done on templates side. These strings can then be extracted using mentioned Grunt task. This task would update the `.pot` file. You can
then use one of the services to update the `.po` file. For the english version `.po` file can be updated manually as it doesn't need any translated strings.

- Any changes to strings or text can be done on `.po` file. Find the string/text you would like to change and update the `msgstr ""` with desired value. A spelling change can be done
as shown below. `msgid` in this case would not change as it is the key for the tool to identiy strings that needs to be replaced.

	Before:
		msgid "Analyses"
		msgstr ""
	After:
		msgid "Analyses"
		msgstr "Analysis"

- Deletion of strings or text would need to be done of templates side. Once the strings/texts are removed run the extract Grunt task. This task would update the `.pot` file. You can
then use one of the services to update the `.po` file. Which would remove the strings/texts from `.po` file as well. For the english version `.po` file can be updated manually by deleting the entries.

### Work arounds

- The tool fails certain times when it encounters '&' in the strings. The work around for that is below

	```
	<span translate>Login &amp; Play</span> <!-- translates correctly -->
	<span translate>Login & Play</span> <!-- fail -->
	<span>{{ 'Login & Play' | translate }}</span> <!-- translates correctly -->
	<span>{{ 'Login &amp; Play' | translate }}</span> <!-- fail -->
	```

- The other work around we have is with `&nbsp;` on `repository.external.html` page. The headings on pie chart breaks into a new line if the string has ' ' in it. So `Experimental Strategy` would break into a new line. However, `Experimental&nbsp;Strategy` would be ok. Passing the string with `&nbsp;` to the translate filter would not work. To overcome this issue a new filter was introduced. The string to the Pie chart heading was passed something like this.

	```
	{{'Experimental Strategy' | translate | addEntity}}
	```
First we are annoting the string using translate filter. Since the string doesn't have `&nbsp;`, the transalte filter would work as expected. The second addEntity filter then adds the non-breaking space entity into the string and therefore not breaking the Pie chart heading.

	```
	module.filter('addEntity', function(){
	  return function(string){
       return string.split(' ').join('\u00A0');
     };
  	});
	```
- Angular constants would not be able to use angular-gettext plugin to extract the string. Therefore a function was created which would help the Grunt plugin extracting texts from Angular constants.

	```
	function gettext(string){
     return string;
   }
	```
Function returns the string itself. So it shouldn't cause any issue with development. Once you annote the strings in constants with this function, the extract Grunt task would pickit up and append it to the `.pot` file. You could then use angular-gettext translate filter to obtain actuall translation of the string.

	```
	{{item.name | translate}}
	```
- Some times we are binding angular scope data inside HTML attribures. Tooltip is good example of it. it's impossible to translate the following without translating strings in JS, we have implemented a work around for it:

	```
	<i class="icon-download-cloud" data-tooltip="View in {{dataRepoTitle}}' />
	```
The work around for this is to wrap the scope variable with different delimiters  and pass a filter that in turns replaces them with the default delimiters.
	
	```
	<i class="icon-download-cloud" 
	  data-tooltip="{{ 'View in [[dataRepoTitle]]' | translate | subDelimiters:{dataRepoTitle: dataRepoTitle} }}' />
	```
The `subDelimiters` filter replaces `[[` with default delimiters `{{` and returns interpolated string. 
	
	```
	module.filter('subDelimiters', function($interpolate){
     return function(string, context){
       string = string.replace(/\[\[/g, '{{').replace(/\]\]/g, '}}');
       var interpolateFn = $interpolate(string);
       return interpolateFn(context);
     };
  });
  ```

## Translation

There are different types of services available to translate the strings. Some of them
are listed on [Wikipedia](http://en.wikipedia.org/wiki/Gettext#See_also). The `.pot` file 
generated by Grunt plugin is a standard file format for any translator. This file can be
found in [translations](dcc-portal-ui/translations/) folder. Use any of the listed services or a translator 
to translate the strings. Make sure to save the translated strings in a `.po` file in the same folder.