package net.bytebuddy.implementation;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Test;

import java.util.List;

import static net.bytebuddy.matcher.ElementMatchers.isToString;
import static org.mockito.Mockito.when;

public class MethodDelegationOtherTest {

    private static final String FOO = "foo", BAR = "bar";

    @Test(expected = IllegalStateException.class)
    public void testDelegationToInvisibleInstanceThrowsException() throws Exception {
        new ByteBuddy()
                .subclass(Object.class)
                .method(isToString())
                .intercept(MethodDelegation.to(new Qux()))
                .make();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDelegationWithIllegalType() throws Exception {
        MethodDelegation.to(new Object(), String.class);
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(MethodDelegation.class).refine(new ObjectPropertyAssertion.Refinement<List<?>>() {
            @Override
            public void apply(List<?> mock) {
                when(mock.size()).thenReturn(1);
            }
        }).apply();
        ObjectPropertyAssertion.of(MethodDelegation.FieldDefinable.class).apply();
        ObjectPropertyAssertion.of(MethodDelegation.Appender.class).apply();
        ObjectPropertyAssertion.of(MethodDelegation.ImplementationDelegate.ForConstruction.class).apply();
        ObjectPropertyAssertion.of(MethodDelegation.ImplementationDelegate.ForStaticMethod.class).apply();
        ObjectPropertyAssertion.of(MethodDelegation.ImplementationDelegate.ForField.class).apply();
    }

    public static class Foo {

        public static void foo() {
            /* empty */
        }

        @Override
        public boolean equals(Object other) {
            return other != null && other.getClass() == getClass();
        }

        @Override
        public int hashCode() {
            return 31;
        }
    }

    public static class Bar {

        public static void bar() {
            /* empty */
        }

        @Override
        public boolean equals(Object other) {
            return other != null && other.getClass() == getClass();
        }

        @Override
        public int hashCode() {
            return 27;
        }
    }

    static class Qux {
        /* empty */
    }
}
