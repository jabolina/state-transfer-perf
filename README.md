

`mvn clean install`

`./bin/run --help` for all options.

Basic scenario with two nodes:

```bash
$ ./bin/run -cc configuration/local/control.xml -cs 2 --num-keys 1000000 --first
$ ./bin/run -cc configuration/local/control.xml -cs 2 --num-keys 1000000
```

You can integrate with async-profiler:

```bash
./bin/run -cc configuration/local/control.xml -cs 2 --first --profiling /home/jab/async-profiler/lib/libasyncProfiler.so 
```

This will capture CPU events and write down the JFR in the target folder.
You can use jfr-converter to transform the JFR into the flamegraph.
