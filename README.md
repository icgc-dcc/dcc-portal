ICGC DCC Data Submission Application
===

Instructions
---

Clone the repository

	git clone https://github.com/icgc-dcc/data-submission.git

Start the server

	cd data-submission/server
	mvn exec:java

Start brunch (in another console)

	cd data-submission/client
	brunch w -s

Start the proxy (in yet another console)

	cd data-submission/client
	cake proxy

Point your browser to [http://localhost:3001/](http://localhost:3001/)
