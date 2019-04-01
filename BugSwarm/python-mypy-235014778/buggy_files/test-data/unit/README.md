Tests
=====


Quick Start
-----------

To add a simple unit test for a new feature you developed, open or create a
`test-data/unit/check-*.test` file with a name that roughly relates to the
feature you added.

Add the test in this format anywhere in the file:

    [case testNewSyntaxBasics]
    # flags: --python-version 3.6
    x: int
    x = 5
    y: int = 5

    a: str
    a = 5  # E: Incompatible types in assignment (expression has type "int", variable has type "str")
    b: str = 5  # E: Incompatible types in assignment (expression has type "int", variable has type "str")

    zzz: int
    zzz: str  # E: Name 'zzz' already defined

- no code here is executed, just type checked
- optional `# flags: ` indicates which flags to use for this unit test
- `# E: abc...` indicates that this line should result in type check error
with text "abc..."
- note a space after `E:` and `flags:`
- `# E:12` adds column number to the expected error
- repeating `# E: ` several times in one line indicates multiple expected errors in one line
- `W: ...` and `N: ...` works exactly like `E:`, but report a warning and a note respectively
- lines that don't contain the above should cause no type check errors
- optional `[builtins fixtures/...]` tells the type checker to use
stubs from the indicated file (see Fixtures section below)
- optional `[out]` is an alternative to the "# E:" notation: it indicates that
any text after it contains the expected type checking error messages.
usually, "E: " is preferred because it makes it easier to associate the
errors with the code generating them at a glance, and to change the code of
the test without having to change line numbers in `[out]`
- an empty `[out]` section has no effect


Fixtures
--------

The unit tests use minimal stubs for builtins, so a lot of operations are not
possible. You should generally define any needed classes within the test case
instead of relying on builtins, though clearly this is not always an option
(see below for more about stubs in test cases). This way tests run much
faster and don't break if the stubs change. If your test crashes mysteriously
even though the code works when run manually, you should make sure you have
all the stubs you need for your test case, including built-in classes such as
`list` or `dict`, as these are not included by default.

    - The builtins used by default in unit tests live in
    `test-data/unit/lib-stub`.

    - Individual test cases can override the stubs by using `[builtins
    fixtures/foo.pyi]`; this targets files in `test-data/unit/fixtures`; feel
    free to modify existing files there or create new ones as you deem fit.

    - You are also free to add additional stubs to this directory, but
    generally don't update files in `lib-stub` without first discussing the
    addition with other mypy developers, as additions could slow down the test
    suite.


Running tests and linting
-------------------------

First install any additional dependencies needed for testing:

    $ python3 -m pip install -U -r test-requirements.txt

To run all tests, run the script `runtests.py` in the mypy repository:

    $ ./runtests.py

Note that some tests will be disabled for older python versions.

This will run all tests, including integration and regression tests,
and will type check mypy and verify that all stubs are valid.

You can run a subset of test suites by passing positive or negative
filters:

    $ ./runtests.py lex parse -x lint -x stub

For example, to run unit tests only, which run pretty quickly:

    $ ./runtests.py unit-test pytest

The unit test suites are driven by a mixture of test frameworks: mypy's own
`myunit` framework, and `pytest`, which we're in the process of migrating to.
Test suites for individual components are in the files `mypy/test/test*.py`.
You can run many of these individually by doing `runtests.py testfoobar`. For
finer control over which unit tests are run and how, you can run `py.test` or
`scripts/myunit` directly, or pass inferior arguments via `-a`:

    $ py.test mypy/test/testcheck.py -v -k MethodCall
    $ ./runtests.py -v 'pytest mypy/test/testcheck' -a -v -a -k -a MethodCall

    $ PYTHONPATH=$PWD scripts/myunit -m mypy.test.testlex -v '*backslash*'
    $ ./runtests.py mypy.test.testlex -a -v -a '*backslash*'

You can also run the type checker for manual testing without
installing anything by setting up the Python module search path
suitably (the lib-typing/3.2 path entry is not needed for Python 3.5
or when you have manually installed the `typing` module):

    $ export PYTHONPATH=$PWD:$PWD/lib-typing/3.2
    $ python<version> -m mypy PROGRAM.py

You can add the entry scripts to PATH for a single python3 version:

    $ export PATH=$PWD/scripts
    $ mypy PROGRAM.py

You can check a module or string instead of a file:

    $ mypy PROGRAM.py
    $ mypy -m MODULE
    $ mypy -c 'import MODULE'

To run the linter:

    $ ./runtests.py lint

Many test suites store test case descriptions in text files
(`test-data/unit/*.test`). The module `mypy.test.data` parses these
descriptions. The package `mypy.myunit` contains the test framework used for
the non-checker test cases.

Python evaluation test cases are a little different from unit tests
(`mypy/test/testpythoneval.py`, `test-data/unit/pythoneval.test`). These
type check programs and run them. Unlike the unit tests, these use the
full builtins and library stubs instead of minimal ones. Run them using
`runtests.py testpythoneval`.

`runtests.py` by default runs tests in parallel using as many processes as
there are logical cores the `runtests.py` process is allowed to use (on
some platforms this information isn't available, so 2 processes are used by
default). You can change the number of workers using `-j` option.

All pytest tests run as a single test from the perspective of `runtests.py`,
and so `-j` option has no effect on them. Instead, `pytest` itself determines
the number of processes to use. The default (set in `./pytest.ini`) is the
number of logical cores; this can be overridden using `-n` option.

Note that running more processes than logical cores is likely to
significantly decrease performance; the relevant count is the number of
processes used by `runtests.py` plus those used by `pytest`.


Coverage reports
----------------

There is an experimental feature to generate coverage reports.  To use
this feature, you need to `pip install -U lxml`.  This is an extension
module and requires various library headers to install; on a
Debian-derived system the command
  `apt-get install python3-dev libxml2-dev libxslt1-dev`
may provide the necessary dependencies.

To use the feature, pass e.g. `--txt-report "$(mktemp -d)"`.