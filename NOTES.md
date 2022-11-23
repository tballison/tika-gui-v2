# Informal Notes

## H2
This is not a secure way of configuring H2, but for testing purposes, and IF you trust your ports, start a server:

```java -cp h2-2.1.214.jar  org.h2.tools.Server -ifNotExists```

Then specify the url with any appended db, e.g. (on linux) ```jdbc:h2:tcp://192.168.200.4:9092/~/tika-test```

The *.mv.db file will be written in your home directory: `~/tika-test.mv.db`