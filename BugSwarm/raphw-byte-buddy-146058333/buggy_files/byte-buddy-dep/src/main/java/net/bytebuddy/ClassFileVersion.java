package net.bytebuddy;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.utility.privilege.ClassLoaderAction;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedAction;

/**
 * A wrapper object for representing a validated class file version in the format that is specified by the
 * <a href="http://docs.oracle.com/javase/specs/jvms/se7/html/jvms-4.html">JVMS</a>.
 */
public class ClassFileVersion implements Comparable<ClassFileVersion> {

    /**
     * Returns the minimal version number that is legal.
     */
    protected static final int BASE_VERSION = 44;

    /**
     * The class file version of Java 1.
     */
    public static final ClassFileVersion JAVA_V1 = new ClassFileVersion(Opcodes.V1_1);

    /**
     * The class file version of Java 2.
     */
    public static final ClassFileVersion JAVA_V2 = new ClassFileVersion(Opcodes.V1_2);

    /**
     * The class file version of Java 3.
     */
    public static final ClassFileVersion JAVA_V3 = new ClassFileVersion(Opcodes.V1_3);

    /**
     * The class file version of Java 4.
     */
    public static final ClassFileVersion JAVA_V4 = new ClassFileVersion(Opcodes.V1_4);

    /**
     * The class file version of Java 5.
     */
    public static final ClassFileVersion JAVA_V5 = new ClassFileVersion(Opcodes.V1_5);

    /**
     * The class file version of Java 6.
     */
    public static final ClassFileVersion JAVA_V6 = new ClassFileVersion(Opcodes.V1_6);

    /**
     * The class file version of Java 7.
     */
    public static final ClassFileVersion JAVA_V7 = new ClassFileVersion(Opcodes.V1_7);

    /**
     * The class file version of Java 8.
     */
    public static final ClassFileVersion JAVA_V8 = new ClassFileVersion(Opcodes.V1_8);

    /**
     * The class file version of Java 9.
     */
    public static final ClassFileVersion JAVA_V9 = new ClassFileVersion(53);

    /**
     * A version locator for the executing JVM.
     */
    private static final VersionLocator VERSION_LOCATOR = findVersionLocator();

    /**
     * Creates a version locator of the executing JVM.
     *
     * @return An appropriate version locator.
     */
    @SuppressFBWarnings(value = "REC_CATCH_EXCEPTION", justification = "Exception not supposed to be rethrown")
    private static VersionLocator findVersionLocator() {
        try {
            Class<?> version = Class.forName("java.lang.Runtime$Version");
            return new VersionLocator.ForJava9CapableVm(version.getDeclaredMethod("current"), version.getDeclaredMethod("major"));
        } catch (Exception ignored) {
            return VersionLocator.ForLegacyVm.INSTANCE;
        }
    }

    /**
     * The version number that is represented by this class file version instance.
     */
    private final int versionNumber;

    /**
     * Creates a wrapper for a given minor-major release of the Java class file file.
     *
     * @param versionNumber The minor-major release number.
     */
    protected ClassFileVersion(int versionNumber) {
        this.versionNumber = versionNumber;
    }

    /**
     * Creates a wrapper for a given minor-major release of the Java class file file.
     *
     * @param versionNumber The minor-major release number.
     * @return A representation of the version number.
     */
    public static ClassFileVersion ofMinorMajor(int versionNumber) {
        ClassFileVersion classFileVersion = new ClassFileVersion(versionNumber);
        if (classFileVersion.getMajorVersion() <= BASE_VERSION) {
            throw new IllegalArgumentException("Class version " + versionNumber + " is not valid");
        }
        return classFileVersion;
    }

    /**
     * Creates a class file version for a given major release of Java. Currently, all versions reaching from
     * Java 1 to Java 8 are supported.
     *
     * @param javaVersion The Java version.
     * @return A wrapper for the given Java class file version.
     */
    public static ClassFileVersion ofJavaVersion(int javaVersion) {
        switch (javaVersion) {
            case 1:
                return JAVA_V1;
            case 2:
                return JAVA_V2;
            case 3:
                return JAVA_V3;
            case 4:
                return JAVA_V4;
            case 5:
                return JAVA_V5;
            case 6:
                return JAVA_V6;
            case 7:
                return JAVA_V7;
            case 8:
                return JAVA_V8;
            case 9:
                return JAVA_V9;
            default:
                throw new IllegalArgumentException("Unknown Java version: " + javaVersion);
        }
    }

    /**
     * Finds the highest class file version that is compatible to the current JVM version by parsing the java.version
     * property which is provided by {@link java.lang.System#getProperty(String)} or by querying available APIs. If
     * neither is possible, an {@link IllegalStateException} is thrown.
     *
     * @return The currently running Java process's class file version.
     */
    public static ClassFileVersion forCurrentJavaVersion() {
        return VERSION_LOCATOR.findCurrentVersion();
    }

    /**
     * Finds the highest class file version that is compatible to the current JVM version by parsing the java.version
     * property which is provided by {@link java.lang.System#getProperty(String)} or by querying available APIs. If this
     * is not possible, the {@code fallback} version is returned.
     *
     * @param fallback The version to fallback to if locating a class file version is not possible.
     * @return The currently running Java process's class file version or the fallback if locating this version is impossible.
     */
    @SuppressFBWarnings(value = "REC_CATCH_EXCEPTION", justification = "Exception not supposed to be rethrown")
    public static ClassFileVersion forCurrentJavaVersion(ClassFileVersion fallback) {
        try {
            return forCurrentJavaVersion();
        } catch (Exception ignored) {
            return fallback;
        }
    }

    /**
     * Extracts a class' class version. The class' byte code is located by querying the {@link ClassLoader} of the class.
     *
     * @param type The type for which to locate a class file version.
     * @return The type's class file version.
     * @throws IOException If an error occurs while reading the class file.
     */
    public static ClassFileVersion of(Class<?> type) throws IOException {
        return of(type, AccessController.getContext());
    }

    /**
     * Extracts a class' class version. The class' byte code is located by querying the {@link ClassLoader} of the class.
     *
     * @param type                 The type for which to locate a class file version.
     * @param accessControlContext The access control context to use.
     * @return The type's class file version.
     * @throws IOException If an error occurs while reading the class file.
     */
    public static ClassFileVersion of(Class<?> type, AccessControlContext accessControlContext) throws IOException {
        return of(type, ClassFileLocator.ForClassLoader.of(ClassLoaderAction.apply(type, accessControlContext), accessControlContext));
    }

    /**
     * Extracts a class' class version.
     *
     * @param type             The type for which to locate a class file version.
     * @param classFileLocator The class file locator to query for a class file.
     * @return The type's class file version.
     * @throws IOException If an error occurs while reading the class file.
     */
    public static ClassFileVersion of(Class<?> type, ClassFileLocator classFileLocator) throws IOException {
        return of(new TypeDescription.ForLoadedType(type), classFileLocator);
    }

    /**
     * Extracts a class' class version.
     *
     * @param typeDescription  The type for which to locate a class file version.
     * @param classFileLocator The class file locator to query for a class file.
     * @return The type's class file version.
     * @throws IOException If an error occurs while reading the class file.
     */
    public static ClassFileVersion of(TypeDescription typeDescription, ClassFileLocator classFileLocator) throws IOException {
        ClassReader classReader = new ClassReader(classFileLocator.locate(typeDescription.getName()).resolve());
        VersionExtractor versionExtractor = new VersionExtractor();
        classReader.accept(versionExtractor, ClassReader.SKIP_CODE);
        return ClassFileVersion.ofMinorMajor(versionExtractor.getClassFileVersionNumber());
    }

    /**
     * Returns the minor-major release number of this class file version.
     *
     * @return The minor-major release number of this class file version.
     */
    public int getMinorMajorVersion() {
        return versionNumber;
    }

    /**
     * Returns the major version this instance represents.
     *
     * @return The major version this instance represents.
     */
    public int getMajorVersion() {
        return versionNumber & 0xFF;
    }

    /**
     * Returns the minor version this instance represents.
     *
     * @return The minor version this instance represents.
     */
    public int getMinorVersion() {
        return versionNumber >> 16;
    }

    /**
     * Returns the Java runtime version number of this class file version.
     *
     * @return The Java runtime version.
     */
    public int getJavaVersion() {
        return getMajorVersion() - BASE_VERSION;
    }

    /**
     * Checks if this class file version is at least of the provided version.
     *
     * @param classFileVersion The version to check against.
     * @return {@code true} if this version is at least of the given version.
     */
    public boolean isAtLeast(ClassFileVersion classFileVersion) {
        return compareTo(classFileVersion) > -1;
    }

    /**
     * Checks if this class file version is less than the provided version.
     *
     * @param classFileVersion The version to check against.
     * @return {@code true} if this version is less than the given version.
     */
    public boolean isLessThan(ClassFileVersion classFileVersion) {
        return compareTo(classFileVersion) < 0;
    }

    @Override
    public int compareTo(ClassFileVersion other) {
        return Integer.signum(getMajorVersion() == other.getMajorVersion()
                ? getMinorVersion() - other.getMinorVersion()
                : getMajorVersion() - other.getMajorVersion());
    }

    @Override
    public boolean equals(Object other) {
        return this == other || !(other == null || getClass() != other.getClass())
                && versionNumber == ((ClassFileVersion) other).versionNumber;
    }

    @Override
    public int hashCode() {
        return versionNumber;
    }

    @Override
    public String toString() {
        return "ClassFileVersion{versionNumber=" + versionNumber + '}';
    }

    /**
     * A locator for the executing VM's Java version.
     */
    protected interface VersionLocator {

        /**
         * Locates the current VM's major version number.
         *
         * @return The current VM's major version number.
         */
        ClassFileVersion findCurrentVersion();

        /**
         * A version locator for a JVM of at least version 9.
         */
        class ForJava9CapableVm implements VersionLocator {

            /**
             * Indicates that a reflective method call invokes a static method.
             */
            private static final Object STATIC_METHOD = null;

            /**
             * The {@code java java.lang.Runtime.Version#current()} method.
             */
            private final Method current;

            /**
             * The {@code java.lang.Runtime.Version#major()} method.
             */
            private final Method major;

            /**
             * Creates a new version locator for a Java 9 capable VM.
             *
             * @param current The {@code java.lang.Runtime.Version#current()} method.
             * @param major   The {@code java.lang.Runtime.Version#major()} method.
             */
            protected ForJava9CapableVm(Method current, Method major) {
                this.current = current;
                this.major = major;
            }

            @Override
            public ClassFileVersion findCurrentVersion() {
                try {
                    return ClassFileVersion.ofJavaVersion((Integer) major.invoke(current.invoke(STATIC_METHOD)));
                } catch (InvocationTargetException exception) {
                    throw new IllegalStateException("Could not look up VM version", exception.getCause());
                } catch (IllegalAccessException exception) {
                    throw new IllegalStateException("Could not access VM version lookup", exception);
                }
            }

            @Override
            public boolean equals(Object object) {
                if (this == object) return true;
                if (object == null || getClass() != object.getClass()) return false;
                ForJava9CapableVm that = (ForJava9CapableVm) object;
                return current.equals(that.current) && major.equals(that.major);
            }

            @Override
            public int hashCode() {
                int result = current.hashCode();
                result = 31 * result + major.hashCode();
                return result;
            }

            @Override
            public String toString() {
                return "ClassFileVersion.VersionLocator.ForJava9CapableVm{" +
                        "current=" + current +
                        ", major=" + major +
                        '}';
            }
        }

        /**
         * A version locator for a JVM that does not provide the {@code jdk.Version} class.
         */
        enum ForLegacyVm implements VersionLocator, PrivilegedAction<String> {

            /**
             * The singleton instance.
             */
            INSTANCE;

            /**
             * The system property for this JVM's Java version.
             */
            private static final String JAVA_VERSION_PROPERTY = "java.version";

            @Override
            public ClassFileVersion findCurrentVersion() {
                String versionString = AccessController.doPrivileged(this);
                // To be removed once the implementation of Java 9 is finalized.
                if (versionString.startsWith("9")) {
                    return ClassFileVersion.JAVA_V9;
                }
                int[] versionIndex = {-1, 0, 0};
                for (int i = 1; i < 3; i++) {
                    versionIndex[i] = versionString.indexOf('.', versionIndex[i - 1] + 1);
                    if (versionIndex[i] == -1) {
                        throw new IllegalStateException("This JVM's version string does not seem to be valid: " + versionString);
                    }
                }
                return ClassFileVersion.ofJavaVersion(Integer.parseInt(versionString.substring(versionIndex[1] + 1, versionIndex[2])));
            }

            @Override
            public String run() {
                return System.getProperty(JAVA_VERSION_PROPERTY);
            }

            @Override
            public String toString() {
                return "ClassFileVersion.VersionLocator.ForLegacyVm." + name();
            }
        }
    }

    /**
     * A simple visitor that extracts the class file version of a class file.
     */
    protected static class VersionExtractor extends ClassVisitor {

        /**
         * The class file version extracted from a class.
         */
        private int classFileVersionNumber;

        /**
         * Creates a new extractor.
         */
        protected VersionExtractor() {
            super(Opcodes.ASM5);
        }

        @Override
        public void visit(int classFileVersionNumber, int modifier, String internalName, String signature, String superTypeName, String[] interfaceName) {
            this.classFileVersionNumber = classFileVersionNumber;
        }

        /**
         * Returns the class file version number found in a class file.
         *
         * @return The class file version number found in a class file.
         */
        protected int getClassFileVersionNumber() {
            return classFileVersionNumber;
        }

        @Override
        public String toString() {
            return "ClassFileVersion.VersionExtractor{" +
                    "classFileVersionNumber=" + classFileVersionNumber +
                    '}';
        }
    }
}
