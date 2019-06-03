SET test_package=info/kgeorgiy/java/advanced/implementor/
SET source_package=ru/ifmo/rain/abubakirov/implementor/

jar cfm Implementor.jar manifest.txt %source_package%*.class %test_package%*.class
pause