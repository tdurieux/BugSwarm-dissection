package net.bytebuddy.implementation;

import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.test.utility.CallTraceable;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Matcher;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collection;

import static net.bytebuddy.matcher.ElementMatchers.isDeclaredBy;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

@RunWith(Parameterized.class)
public class MethodDelegationTest<T extends CallTraceable> extends AbstractImplementationTest {

    private static final String FOO = "foo", BAR = "bar", FIELD_NAME = "qux", PREDEFINED_FIELD_NAME = "baz", PREDEFINED_FIELD_NAME_STATIC = "foobar";

    private static final byte BYTE_MULTIPLICATOR = 3;

    private static final short SHORT_MULTIPLICATOR = 3;

    private static final char CHAR_MULTIPLICATOR = 3;

    private static final int INT_MULTIPLICATOR = 3;

    private static final long LONG_MULTIPLICATOR = 3L;

    private static final float FLOAT_MULTIPLICATOR = 3f;

    private static final double DOUBLE_MULTIPLICATOR = 3d;

    private static final boolean DEFAULT_BOOLEAN = false;

    private static final byte DEFAULT_BYTE = 1;

    private static final short DEFAULT_SHORT = 1;

    private static final char DEFAULT_CHAR = 1;

    private static final int DEFAULT_INT = 1;

    private static final long DEFAULT_LONG = 1L;

    private static final float DEFAULT_FLOAT = 1f;

    private static final double DEFAULT_DOUBLE = 1d;

    private final Class<T> sourceType;

    private final Class<?> targetType;

    private final Class<?>[] parameterTypes;

    private final Object[] arguments;

    private final Matcher<?> matcher;

    public MethodDelegationTest(Class<T> sourceType,
                                Class<?> targetType,
                                Class<?>[] parameterTypes,
                                Object[] arguments,
                                Matcher<?> matcher) {
        this.sourceType = sourceType;
        this.targetType = targetType;
        this.parameterTypes = parameterTypes;
        this.arguments = arguments;
        this.matcher = matcher;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {BooleanSource.class, BooleanTarget.class, new Class<?>[]{boolean.class}, new Object[]{DEFAULT_BOOLEAN}, is(!DEFAULT_BOOLEAN)},
                {ByteSource.class, ByteTarget.class, new Class<?>[]{byte.class}, new Object[]{DEFAULT_BYTE}, is((byte) (DEFAULT_BYTE * BYTE_MULTIPLICATOR))},
                {ShortSource.class, ShortTarget.class, new Class<?>[]{short.class}, new Object[]{DEFAULT_SHORT}, is((short) (DEFAULT_SHORT * SHORT_MULTIPLICATOR))},
                {CharSource.class, CharTarget.class, new Class<?>[]{char.class}, new Object[]{DEFAULT_CHAR}, is((char) (DEFAULT_CHAR * CHAR_MULTIPLICATOR))},
                {IntSource.class, IntTarget.class, new Class<?>[]{int.class}, new Object[]{DEFAULT_INT}, is(DEFAULT_INT * INT_MULTIPLICATOR)},
                {LongSource.class, LongTarget.class, new Class<?>[]{long.class}, new Object[]{DEFAULT_LONG}, is(DEFAULT_LONG * LONG_MULTIPLICATOR)},
                {FloatSource.class, FloatTarget.class, new Class<?>[]{float.class}, new Object[]{DEFAULT_FLOAT}, is(DEFAULT_FLOAT * FLOAT_MULTIPLICATOR)},
                {DoubleSource.class, DoubleTarget.class, new Class<?>[]{double.class}, new Object[]{DEFAULT_DOUBLE}, is(DEFAULT_DOUBLE * DOUBLE_MULTIPLICATOR)},
                {VoidSource.class, VoidTarget.class, new Class<?>[0], new Object[0], nullValue()},
                {StringSource.class, StringTarget.class, new Class<?>[]{String.class}, new Object[]{FOO}, is(FOO + BAR)},
        });
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testStaticMethodBinding() throws Exception {
        DynamicType.Loaded<T> loaded = implement(sourceType, MethodDelegation.to(targetType));
        assertThat(loaded.getLoadedAuxiliaryTypes().size(), is(0));
        assertThat(loaded.getLoaded().getDeclaredMethods().length, is(1));
        assertThat(loaded.getLoaded().getDeclaredFields().length, is(0));
        T instance = loaded.getLoaded().getDeclaredConstructor().newInstance();
        assertThat(instance.getClass(), not(CoreMatchers.<Class<?>>is(sourceType)));
        assertThat(instance, instanceOf(sourceType));
        assertThat(loaded.getLoaded().getDeclaredMethod(FOO, parameterTypes).invoke(instance, arguments), (Matcher) matcher);
        instance.assertZeroCalls();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testStaticFieldBinding() throws Exception {
        DynamicType.Loaded<T> loaded = implement(sourceType, MethodDelegation.to(targetType.getDeclaredConstructor().newInstance()).filter(isDeclaredBy(targetType)));
        assertThat(loaded.getLoadedAuxiliaryTypes().size(), is(0));
        assertThat(loaded.getLoaded().getDeclaredMethods().length, is(1));
        assertThat(loaded.getLoaded().getDeclaredFields().length, is(1));
        T instance = loaded.getLoaded().getDeclaredConstructor().newInstance();
        assertThat(instance.getClass(), not(CoreMatchers.<Class<?>>is(sourceType)));
        assertThat(instance, instanceOf(sourceType));
        assertThat(loaded.getLoaded().getDeclaredMethod(FOO, parameterTypes).invoke(instance, arguments), (Matcher) matcher);
        instance.assertZeroCalls();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testFieldInstanceBindingAndDefined() throws Exception {
        DynamicType.Loaded<T> loaded = implement(sourceType, MethodDelegation.toField(FIELD_NAME).defineAs(targetType, Visibility.PUBLIC).filter(isDeclaredBy(targetType)));
        assertThat(loaded.getLoadedAuxiliaryTypes().size(), is(0));
        assertThat(loaded.getLoaded().getDeclaredMethods().length, is(1));
        assertThat(loaded.getLoaded().getDeclaredFields().length, is(1));
        T instance = loaded.getLoaded().getDeclaredConstructor().newInstance();
        Field field = loaded.getLoaded().getDeclaredField(FIELD_NAME);
        assertThat(field.getModifiers(), is(Modifier.PUBLIC));
        assertThat(field.getType(), CoreMatchers.<Class<?>>is(targetType));
        field.setAccessible(true);
        field.set(instance, targetType.getDeclaredConstructor().newInstance());
        assertThat(instance.getClass(), not(CoreMatchers.<Class<?>>is(sourceType)));
        assertThat(instance, instanceOf(sourceType));
        assertThat(loaded.getLoaded().getDeclaredMethod(FOO, parameterTypes).invoke(instance, arguments), (Matcher) matcher);
        instance.assertZeroCalls();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testFieldStaticBinding() throws Exception {
        DynamicType.Loaded<T> loaded = implement(sourceType, MethodDelegation.toField(PREDEFINED_FIELD_NAME_STATIC).filter(isDeclaredBy(targetType)));
        assertThat(loaded.getLoadedAuxiliaryTypes().size(), is(0));
        assertThat(loaded.getLoaded().getDeclaredMethods().length, is(1));
        assertThat(loaded.getLoaded().getDeclaredFields().length, is(0));
        T instance = loaded.getLoaded().getDeclaredConstructor().newInstance();
        Field field = loaded.getLoaded().getField(PREDEFINED_FIELD_NAME_STATIC);
        field.set(null, targetType.getDeclaredConstructor().newInstance());
        assertThat(instance.getClass(), not(CoreMatchers.<Class<?>>is(sourceType)));
        assertThat(instance, instanceOf(sourceType));
        assertThat(loaded.getLoaded().getDeclaredMethod(FOO, parameterTypes).invoke(instance, arguments), (Matcher) matcher);
        instance.assertZeroCalls();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testFieldInstanceBinding() throws Exception {
        DynamicType.Loaded<T> loaded = implement(sourceType, MethodDelegation.toField(PREDEFINED_FIELD_NAME).filter(isDeclaredBy(targetType)));
        assertThat(loaded.getLoadedAuxiliaryTypes().size(), is(0));
        assertThat(loaded.getLoaded().getDeclaredMethods().length, is(1));
        assertThat(loaded.getLoaded().getDeclaredFields().length, is(0));
        T instance = loaded.getLoaded().getDeclaredConstructor().newInstance();
        Field field = loaded.getLoaded().getField(PREDEFINED_FIELD_NAME);
        field.set(instance, targetType.getDeclaredConstructor().newInstance());
        assertThat(instance.getClass(), not(CoreMatchers.<Class<?>>is(sourceType)));
        assertThat(instance, instanceOf(sourceType));
        assertThat(loaded.getLoaded().getDeclaredMethod(FOO, parameterTypes).invoke(instance, arguments), (Matcher) matcher);
        instance.assertZeroCalls();
    }

    public static class BooleanSource extends CallTraceable {

        public BooleanTarget baz;

        public static BooleanTarget foobar;

        public boolean foo(boolean b) {
            register(FOO);
            return b;
        }
    }

    public static class BooleanTarget {

        public static boolean bar(boolean b) {
            return !b;
        }

        public boolean qux(boolean b) {
            return bar(b);
        }
    }

    public static class ByteSource extends CallTraceable {

        public ByteTarget baz;

        public static ByteTarget foobar;

        public byte foo(byte b) {
            register(FOO);
            return b;
        }
    }

    public static class ByteTarget {

        public static byte bar(byte b) {
            return (byte) (b * BYTE_MULTIPLICATOR);
        }

        public byte qux(byte b) {
            return bar(b);
        }
    }

    public static class ShortSource extends CallTraceable {

        public ShortTarget baz;

        public static ShortTarget foobar;

        public short foo(short s) {
            register(FOO);
            return s;
        }
    }

    public static class ShortTarget {

        public static short bar(short s) {
            return (short) (s * SHORT_MULTIPLICATOR);
        }

        public short qux(short s) {
            return bar(s);
        }
    }

    public static class CharSource extends CallTraceable {

        public CharTarget baz;

        public static CharTarget foobar;

        public char foo(char s) {
            register(FOO);
            return s;
        }
    }

    public static class CharTarget {

        public static char bar(char c) {
            return (char) (c * CHAR_MULTIPLICATOR);
        }

        public char qux(char c) {
            return bar(c);
        }
    }

    public static class IntSource extends CallTraceable {

        public IntTarget baz;

        public static IntTarget foobar;

        public int foo(int i) {
            register(FOO);
            return i;
        }
    }

    public static class IntTarget {

        public static int bar(int i) {
            return i * INT_MULTIPLICATOR;
        }

        public int qux(int i) {
            return bar(i);
        }
    }

    public static class LongSource extends CallTraceable {

        public LongTarget baz;

        public static LongTarget foobar;

        public long foo(long l) {
            register(FOO);
            return l;
        }
    }

    public static class LongTarget {

        public static long bar(long l) {
            return l * LONG_MULTIPLICATOR;
        }

        public long qux(long l) {
            return bar(l);
        }
    }

    public static class FloatSource extends CallTraceable {

        public FloatTarget baz;

        public static FloatTarget foobar;

        public float foo(float f) {
            register(FOO);
            return f;
        }
    }

    public static class FloatTarget {

        public static float bar(float f) {
            return f * FLOAT_MULTIPLICATOR;
        }

        public float qux(float f) {
            return bar(f);
        }
    }

    public static class DoubleSource extends CallTraceable {

        public DoubleTarget baz;

        public static DoubleTarget foobar;

        public double foo(double d) {
            register(FOO);
            return d;
        }
    }

    public static class DoubleTarget {

        public static double bar(double d) {
            return d * DOUBLE_MULTIPLICATOR;
        }

        public double qux(double d) {
            return bar(d);
        }
    }

    public static class VoidSource extends CallTraceable {

        public VoidTarget baz;

        public static VoidTarget foobar;

        public void foo() {
            register(FOO);
        }
    }

    public static class VoidTarget {

        public static void bar() {
            /* empty */
        }

        public void qux() {
            bar();
        }
    }

    public static class StringSource extends CallTraceable {

        public StringTarget baz;

        public static StringTarget foobar;

        public String foo(String s) {
            register(FOO);
            return s;
        }
    }

    public static class StringTarget {

        public static String bar(String s) {
            return s + BAR;
        }

        public String qux(String s) {
            return bar(s);
        }
    }
}
