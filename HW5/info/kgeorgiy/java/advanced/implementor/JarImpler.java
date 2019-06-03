package info.kgeorgiy.java.advanced.implementor;

import java.nio.file.Path;

/**
 * @author Georgiy Korneev (kgeorgiy@kgeorgiy.info)
 */
public interface JarImpler extends Impler {
    /**
     * Produces .jar file implementing class or interface specified by provided token.
     * <p>
     * Generated class classes name should be same as classes name of the type token with Impl suffix
     * added.
     *
     * @param token type token to create implementation for.
     * @param jarFile target .jar file.
     * @throws ImplerException when implementation cannot be generated.
     */
    void implementJar(Class<?> token, Path jarFile) throws ImplerException;
}
