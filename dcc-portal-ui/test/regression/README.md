ramparts
===
Cross-server regression tester with phantomJS. Given a URL, ramparts executes against an origin server and scrapes the page content for API invocations.
It then executes the API against another server and validates the results against what is been shown on the orign server.

*Note 1*: facet testing support is largely experimental, as it is largely reliant on class-based selectors. It does not actually simulate facet term clicking, but
rather that resultant page has the same facet terms and facet term counts.

*Note 2*: ramparts.js will test against donor, gene, mutation advanced search tabs automatically
so it is sufficient to pass in /search?filters={...} for the ```<page>``` argument.

*Note 3*: ramparts.js creates additional processes to sandbox actions. You will need a decent machine !!

*Disclaimer*: This test tool is for the most part stable but could use some TLC to smooth out some of its rough spots.

Installation and setup
---

- Install node modules
  ```
  npm install
  ```

- Run
  ```
  # Set up path to phantomJS binary
  # Point to the phantomJS binary, note the npm version of phantomJS may have issues on MaxOSX
  export PATH=$PATH:node_modules/phantomjs/bin

  # Execute test
  node ramparts.js <origin_base_url> <validation_base_url> <page>
  ```

- Examples
  ```
  # Check genes, check localhost:8888 against localhost:9999
  node ramparts.js http://localhost:8888 http://localhost:9999 /search/g

  # Check donor with filter, consistency on localhost:9000
  node ramparts.js http://localhost:9000 http://localhost:9000 /search\?filters=%7B%22donor%22:%7B%22id%22:%7B%22is%22:%5B%22DO1%22%5D%7D%7D%7D
  ```



MacOS X issues
---
At this moment (August 2015), the phantomJS binaries (1.9x and 2.0x) from npm have issues running on Yosemite. You can download the custom-built binary from the following link and set the PATH to point to it.

https://github.com/eugene1g/phantomjs/releases


Sample output
---
Checking home page

```
$ node ./ramparts.js https://dcc.icgc.org https://dcc.icgc.org /

Running https://dcc.icgc.org https://dcc.icgc.org / 1
waiting to process https://dcc.icgc.org/
evaluating...
Running cross validation: https://dcc.icgc.org/
/search/m 16459160 16459160 		 OK
/search 12979 12979 		 OK
/projects/details 55 55 		 OK
*** Check page content is identical ...  OK
/search/g?filters={"donor":{"availableDataTypes":{"is":["ssm"]}}} 57543 57543 		 OK
```


Caveats
---
ramparts uses fire-and-forget mechanism to do validation checks. This means there may be false positive errors if the servers can not respond in a timely manner. Page content check will typically fail for cross-server checks because each build is different.


ramparts ???
---
ramparts is an instrumental piece from the album *To Record Only Water For 10 Days*.
