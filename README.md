similarImage
============
[![Build Status](https://travis-ci.org/seeker/similarImage.png?branch=develop)](https://travis-ci.org/seeker/similarImage)
[![Coverage Status](https://coveralls.io/repos/seeker/similarImage/badge.png?branch=develop)](https://coveralls.io/r/seeker/similarImage?branch=develop)
[![codecov](https://codecov.io/gh/seeker/similarImage/branch/develop/graph/badge.svg)](https://codecov.io/gh/seeker/similarImage)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/9443ff2c55da47329ed354e7a3d15f29)](https://www.codacy.com/app/seeker/similarImage?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=seeker/similarImage&amp;utm_campaign=Badge_Grade)

A similar image finder using DCT for image comparison.
Based on the [Looks Like It](http://www.hackerfactor.com/blog/?/archives/432-Looks-Like-It.html) blog post.

- Supports distributed mode, the software can be run on multiple machines to speed up processing.
- Dockerfile to create container images.

### Note
------
- The hash generated in 0.0.2 and 0.1.0 are incompatible, due to a faulty implementation.
- If you have a pre 0.1.2 database, you will need to run a flyway baseline for 0.1.2, then migrate to 0.2.1
- If you have a 0.2.0 database, you will need to run a flyway baseline
