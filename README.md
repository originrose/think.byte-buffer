# think.byte-buffer

```
think.byte-buffer> (def mgr (create-manager))
#'think.byte-buffer/mgr
think.byte-buffer> (def test-data (._allocate mgr (* 20 Double/BYTES) "doubles" 15))
#'think.byte-buffer/test-data
think.byte-buffer> (def src-ary (double-array (range 20)))
#'think.byte-buffer/src-ary
think.byte-buffer> (.copy mgr src-ary 0 test-data 0 10)
nil
think.byte-buffer> (def dst (double-array (range 10)))
#'think.byte-buffer/dst
think.byte-buffer> (def dst (double-array 10)))
#'think.byte-buffer/dst
RuntimeException Unmatched delimiter: )  clojure.lang.Util.runtimeException (Util.java:221)
think.byte-buffer> (def dst (double-array 10))
#'think.byte-buffer/dst
think.byte-buffer> (.copy mgr src-ary 0 dst 0 10)
IllegalArgumentException No matching method found: copy for class think.byte_buffer.ByteBuffer$BufferManager  clojure.lang.Reflector.invokeMatchingMethod (Reflector.java:80)
think.byte-buffer> (.copy mgr test-data 0 dst 0 10)
nil
think.byte-buffer> dst
#<[D@5ea099a>
think.byte-buffer> (vec dst)
[0.0 1.0 2.0 3.0 4.0 5.0 6.0 7.0 8.0 9.0]
think.byte-buffer> (def dst (short-array 10))
#'think.byte-buffer/dst
think.byte-buffer> (.copy mgr test-data 0 dst 0 10)
nil
think.byte-buffer> (vec dst)
[0 1 2 3 4 5 6 7 8 9]
think.byte-buffer> (def dst (float-array 10))
#'think.byte-buffer/dst
think.byte-buffer> (vec dst)
[0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0]
think.byte-buffer> (.copy mgr test-data 0 dst 0 10)
nil
think.byte-buffer> (vec dst)
[0.0 1.0 2.0 3.0 4.0 5.0 6.0 7.0 8.0 9.0]
think.byte-buffer>
```

## License

Copyright © 2016 FIXME

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
