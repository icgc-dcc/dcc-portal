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
translated

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
compile it in a JavaScript file.

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

## Translation

There are different types of services available to translate the strings. Some of them
are listed on [Wikipedia](http://en.wikipedia.org/wiki/Gettext#See_also). The `.pot` file 
generated by Grunt plugin is a standard file format for any translator. This file can be
found in [translations](dcc-portal-ui/translations/) folder. Use any of the listed services or a translator 
to translate the strings. Make sure to save the translated strings in a `.po` file in the same folder.