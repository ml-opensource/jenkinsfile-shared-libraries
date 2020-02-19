# jenkinsfile-shared-libraries
Primary script library for Fuzz's Jenkins Pipeline (Public variant).

Every Pipeline build executed on our infrastructure has access to the scripts located in the `vars` directory.

Note that this repository exists to facilitate easy hand off of projects to and from the Fuzz engineering
team; to that end, all Fuzz-specific references are wrapped in try-catch blocks so if they are not present
your scripts won't fail.

Please consider forking this repository for specific project needs. In unusual scenarios some of these functions
are security risks and thus should be stripped out where possible.


# License

```
Copyright 2019 Fuzz Productions

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
```
