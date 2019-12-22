# commons-io

The ``commons-io`` contains advanced IO methods.

## content

### ``LinesReader``

A class for reading compound lines from a source. This is especially useful for reading log files that might contain a stack trace that spreads oer several lines.  
You will be able to provde a ``Predicate`` that decides whether a line will be appended to the previous one or not.

The ``LinesReader``is optimized for performance. As long as the ``Predicate``performs really fast the LinesReader will be only slightly slower than ``BufferedReader.lines()``.
