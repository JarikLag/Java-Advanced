SET data=src\info\kgeorgiy\java\advanced\implementor\
SET link=https://docs.oracle.com/en/java/javase/11/docs/api
SET package=ru.ifmo.rain.abubakirov.implementor

javadoc -d javadoc -link %link% -cp src\;%test%; -private -author -version %package% %data%Impler.java %data%JarImpler.java %data%ImplerException.java
pause
