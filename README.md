similarImage
============
[![Build Status](https://travis-ci.org/dozedoff/similarImage.png?branch=master)](https://travis-ci.org/dozedoff/similarImage) [![Coverage Status](https://coveralls.io/repos/dozedoff/similarImage/badge.png?branch=master)](https://coveralls.io/r/dozedoff/similarImage?branch=master)

A similar image finder using DCT for image comparison.
Based on the [Looks Like It](http://www.hackerfactor.com/blog/?/archives/432-Looks-Like-It.html) blog post.

- Supports distributed mode, the software can be run on multiple machines to speed up processing.
- Dockerfile to create container images.

### Note
------
- The hash generated in 0.0.2 and 0.1.0 are incompatible, due to a faulty implementation.
- If you have a pre 0.1.2 database, you will need to run a flyway baseline for 0.1.2, then migrate to 0.2.1
- If you have a 0.2.0 database, you will need to run a flyway baseline
