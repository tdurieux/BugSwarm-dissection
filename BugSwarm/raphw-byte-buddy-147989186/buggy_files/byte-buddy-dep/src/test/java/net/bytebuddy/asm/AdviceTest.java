package net.bytebuddy.asm;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.ParameterDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.MethodVisitor;

import java.io.IOException;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

import static junit.framework.TestCase.fail;
import static net.bytebuddy.matcher.ElementMatchers.*;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AdviceTest {

    private static final String FOO = "foo", BAR = "bar", QUX = "qux", BAZ = "baz";

    private static final String ENTER = "enter", EXIT = "exit", INSIDE = "inside", THROWABLE = "throwable";

    private static final int VALUE = 42;

    @Test
    public void testEmptyAdviceEntryAndExit() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(EmptyMethod.class)
                .visit(Advice.to(EmptyAdvice.class).on(named(FOO)).readerFlags(ClassReader.SKIP_DEBUG))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO).invoke(type.getConstructor().newInstance()), nullValue(Object.class));
    }

    @Test
    public void testEmptyAdviceEntryAndExitWithEntrySuppression() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(EmptyMethod.class)
                .visit(Advice.to(EmptyAdviceWithEntrySuppression.class).on(named(FOO)).readerFlags(ClassReader.SKIP_DEBUG))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO).invoke(type.getConstructor().newInstance()), nullValue(Object.class));
    }

    @Test
    public void testEmptyAdviceEntryAndExitWithExitSuppression() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(EmptyMethod.class)
                .visit(Advice.to(EmptyAdviceWithEntrySuppression.class).on(named(FOO)).readerFlags(ClassReader.SKIP_DEBUG))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO).invoke(type.getConstructor().newInstance()), nullValue(Object.class));
    }

    @Test
    public void testEmptyAdviceEntryAndExitWithSuppression() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(EmptyMethod.class)
                .visit(Advice.to(EmptyAdviceWithSuppression.class).on(named(FOO)).readerFlags(ClassReader.SKIP_DEBUG))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO).invoke(type.getConstructor().newInstance()), nullValue(Object.class));
    }

    @Test
    public void testEmptyAdviceEntryAndExitWithExceptionHandling() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(EmptyMethod.class)
                .visit(Advice.to(EmptyAdviceWithExceptionHandling.class).on(named(FOO)).readerFlags(ClassReader.SKIP_DEBUG))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO).invoke(type.getConstructor().newInstance()), nullValue(Object.class));
    }

    @Test
    public void testEmptyAdviceEntryAndExitWithExceptionHandlingAndEntrySuppression() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(EmptyMethod.class)
                .visit(Advice.to(EmptyAdviceWithExceptionHandlingAndEntrySuppression.class).on(named(FOO)).readerFlags(ClassReader.SKIP_DEBUG))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO).invoke(type.getConstructor().newInstance()), nullValue(Object.class));
    }

    @Test
    public void testEmptyAdviceEntryAndExitWithExceptionHandlingAndExitSuppression() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(EmptyMethod.class)
                .visit(Advice.to(EmptyAdviceWithExceptionHandlingAndExitSuppression.class).on(named(FOO)).readerFlags(ClassReader.SKIP_DEBUG))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO).invoke(type.getConstructor().newInstance()), nullValue(Object.class));
    }

    @Test
    public void testEmptyAdviceEntryAndExitWithExceptionHandlingAndSuppression() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(EmptyMethod.class)
                .visit(Advice.to(EmptyAdviceWithExceptionHandlingAndSuppression.class).on(named(FOO)).readerFlags(ClassReader.SKIP_DEBUG))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO).invoke(type.getConstructor().newInstance()), nullValue(Object.class));
    }

    @Test
    public void testEmptyAdviceEntry() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(EmptyMethod.class)
                .visit(Advice.to(EmptyAdviceEntry.class).on(named(FOO)).readerFlags(ClassReader.SKIP_DEBUG))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO).invoke(type.getConstructor().newInstance()), nullValue(Object.class));
    }

    @Test
    public void testEmptyAdviceEntryWithSuppression() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(EmptyMethod.class)
                .visit(Advice.to(EmptyAdviceEntryWithSuppression.class).on(named(FOO)).readerFlags(ClassReader.SKIP_DEBUG))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO).invoke(type.getConstructor().newInstance()), nullValue(Object.class));
    }

    @Test
    public void testEmptyAdviceExit() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(EmptyMethod.class)
                .visit(Advice.to(EmptyAdviceExit.class).on(named(FOO)).readerFlags(ClassReader.SKIP_DEBUG))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO).invoke(type.getConstructor().newInstance()), nullValue(Object.class));
    }

    @Test
    public void testEmptyAdviceExitAndSuppression() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(EmptyMethod.class)
                .visit(Advice.to(EmptyAdviceExitAndSuppression.class).on(named(FOO)).readerFlags(ClassReader.SKIP_DEBUG))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO).invoke(type.getConstructor().newInstance()), nullValue(Object.class));
    }

    @Test
    public void testEmptyAdviceExitWithExceptionHandling() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(EmptyMethod.class)
                .visit(Advice.to(EmptyAdviceExitWithExceptionHandling.class).on(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO).invoke(type.getConstructor().newInstance()), nullValue(Object.class));
    }

    @Test
    public void testEmptyAdviceExitWithExceptionHandlingAndSuppression() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(EmptyMethod.class)
                .visit(Advice.to(EmptyAdviceExitWithExceptionHandlingAndSuppression.class).on(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO).invoke(type.getConstructor().newInstance()), nullValue(Object.class));
    }

    @Test
    public void testTrivialDelegation() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(EmptyDelegationAdvice.class)
                .visit(Advice.to(EmptyDelegationAdvice.class).on(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO).invoke(type.getConstructor().newInstance()), nullValue(Object.class));
    }

    @Test
    public void testTrivialAdvice() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.to(TrivialAdvice.class).on(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO).invoke(type.getConstructor().newInstance()), is((Object) FOO));
        assertThat(type.getDeclaredField(ENTER).get(null), is((Object) 1));
        assertThat(type.getDeclaredField(EXIT).get(null), is((Object) 1));
    }

    @Test
    public void testTrivialAdviceWithDelegation() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(TrivialAdviceDelegation.class)
                .visit(Advice.to(TrivialAdviceDelegation.class).on(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO).invoke(type.getConstructor().newInstance()), is((Object) FOO));
        assertThat(type.getDeclaredField(ENTER).get(null), is((Object) 1));
        assertThat(type.getDeclaredField(EXIT).get(null), is((Object) 1));
    }

    @Test
    public void testTrivialAdviceWithSuppression() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.to(TrivialAdviceWithSuppression.class).on(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO).invoke(type.getConstructor().newInstance()), is((Object) FOO));
        assertThat(type.getDeclaredField(ENTER).get(null), is((Object) 1));
        assertThat(type.getDeclaredField(EXIT).get(null), is((Object) 1));
    }

    @Test
    public void testTrivialAdviceDistributedEnterOnly() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.to(TrivialAdvice.class, EmptyAdvice.class).on(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO).invoke(type.getConstructor().newInstance()), is((Object) FOO));
        assertThat(type.getDeclaredField(ENTER).get(null), is((Object) 1));
        assertThat(type.getDeclaredField(EXIT).get(null), is((Object) 0));
    }

    @Test
    public void testTrivialAdviceDistributedExitOnly() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.to(EmptyAdvice.class, TrivialAdvice.class).on(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO).invoke(type.getConstructor().newInstance()), is((Object) FOO));
        assertThat(type.getDeclaredField(ENTER).get(null), is((Object) 0));
        assertThat(type.getDeclaredField(EXIT).get(null), is((Object) 1));
    }

    @Test
    public void testTrivialAdviceWithDelegationEnterOnly() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(TrivialAdviceDelegation.class)
                .visit(Advice.to(TrivialAdviceDelegation.class, EmptyAdvice.class).on(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO).invoke(type.getConstructor().newInstance()), is((Object) FOO));
        assertThat(type.getDeclaredField(ENTER).get(null), is((Object) 1));
        assertThat(type.getDeclaredField(EXIT).get(null), is((Object) 0));
    }

    @Test
    public void testTrivialAdviceWithDelegationExitOnly() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(TrivialAdviceDelegation.class)
                .visit(Advice.to(EmptyAdvice.class, TrivialAdviceDelegation.class).on(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO).invoke(type.getConstructor().newInstance()), is((Object) FOO));
        assertThat(type.getDeclaredField(ENTER).get(null), is((Object) 0));
        assertThat(type.getDeclaredField(EXIT).get(null), is((Object) 1));
    }

    @Test
    public void testTrivialAdviceMultipleMethods() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.to(TrivialAdvice.class).on(isMethod()))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO).invoke(type.getConstructor().newInstance()), is((Object) FOO));
        assertThat(type.getDeclaredField(ENTER).get(null), is((Object) 1));
        assertThat(type.getDeclaredField(EXIT).get(null), is((Object) 1));
        assertThat(type.getDeclaredMethod(BAZ).invoke(null), is((Object) FOO));
        assertThat(type.getDeclaredField(ENTER).get(null), is((Object) 2));
        assertThat(type.getDeclaredField(EXIT).get(null), is((Object) 2));
    }

    @Test
    public void testTrivialAdviceMultipleMethodsWithSuppression() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.to(TrivialAdviceWithSuppression.class).on(isMethod()))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO).invoke(type.getConstructor().newInstance()), is((Object) FOO));
        assertThat(type.getDeclaredField(ENTER).get(null), is((Object) 1));
        assertThat(type.getDeclaredField(EXIT).get(null), is((Object) 1));
        assertThat(type.getDeclaredMethod(BAZ).invoke(null), is((Object) FOO));
        assertThat(type.getDeclaredField(ENTER).get(null), is((Object) 2));
        assertThat(type.getDeclaredField(EXIT).get(null), is((Object) 2));
    }

    @Test
    public void testTrivialAdviceNested() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.to(TrivialAdvice.class).on(named(FOO)))
                .visit(Advice.to(TrivialAdvice.class).on(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO).invoke(type.getConstructor().newInstance()), is((Object) FOO));
        assertThat(type.getDeclaredField(ENTER).get(null), is((Object) 2));
        assertThat(type.getDeclaredField(EXIT).get(null), is((Object) 2));
    }

    @Test
    public void testTrivialAdviceNestedWithSuppression() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.to(TrivialAdviceWithSuppression.class).on(named(FOO)))
                .visit(Advice.to(TrivialAdviceWithSuppression.class).on(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO).invoke(type.getConstructor().newInstance()), is((Object) FOO));
        assertThat(type.getDeclaredField(ENTER).get(null), is((Object) 2));
        assertThat(type.getDeclaredField(EXIT).get(null), is((Object) 2));
    }

    @Test
    public void testTrivialAdviceWithHandler() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.to(TrivialAdvice.class).on(named(FOO + BAZ)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO + BAZ).invoke(type.getConstructor().newInstance()), is((Object) FOO));
        assertThat(type.getDeclaredField(ENTER).get(null), is((Object) 1));
        assertThat(type.getDeclaredField(EXIT).get(null), is((Object) 1));
    }

    @Test
    public void testTrivialAdviceWithHandlerAndSuppression() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.to(TrivialAdviceWithSuppression.class).on(named(FOO + BAZ)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO + BAZ).invoke(type.getConstructor().newInstance()), is((Object) FOO));
        assertThat(type.getDeclaredField(ENTER).get(null), is((Object) 1));
        assertThat(type.getDeclaredField(EXIT).get(null), is((Object) 1));
    }

    @Test
    public void testAdviceOnConstructor() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.to(TrivialAdviceSkipException.class).on(isConstructor()))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getConstructor().newInstance(), notNullValue(Object.class));
        assertThat(type.getDeclaredField(ENTER).get(null), is((Object) 1));
        assertThat(type.getDeclaredField(EXIT).get(null), is((Object) 1));
    }

    @Test
    public void testAdviceOnConstructorExitAdviceWithSuppression() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.to(TrivialAdviceSkipExceptionWithSuppression.class).on(isConstructor()))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getConstructor().newInstance(), notNullValue(Object.class));
        assertThat(type.getDeclaredField(ENTER).get(null), is((Object) 1));
        assertThat(type.getDeclaredField(EXIT).get(null), is((Object) 1));
    }

    @Test
    public void testFrameAdviceSimpleShift() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.to(FrameShiftAdvice.class).on(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO).invoke(type.getConstructor().newInstance()), is((Object) FOO));
    }

    @Test
    public void testFrameAdviceSimpleShiftExpanded() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.to(FrameShiftAdvice.class).on(named(FOO)).readerFlags(ClassReader.EXPAND_FRAMES))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO).invoke(type.getConstructor().newInstance()), is((Object) FOO));
    }

    @Test(expected = IllegalStateException.class)
    public void testAdviceOnConstructorWithSuppressionNotLegal() throws Exception {
        new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.to(TrivialAdvice.class).on(isConstructor()))
                .make();
    }

    @Test
    public void testAdviceWithImplicitArgument() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.to(ArgumentAdvice.class).on(named(BAR)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(BAR, String.class).invoke(type.getConstructor().newInstance(), BAR), is((Object) BAR));
        assertThat(type.getDeclaredField(ENTER).get(null), is((Object) 1));
        assertThat(type.getDeclaredField(EXIT).get(null), is((Object) 1));
    }

    @Test
    public void testAdviceWithImplicitArgumentDelegation() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(ArgumentAdviceDelegationImplicit.class)
                .visit(Advice.to(ArgumentAdviceDelegationImplicit.class).on(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO, String.class).invoke(type.getConstructor().newInstance(), BAR), is((Object) BAR));
    }

    @Test
    public void testAdviceWithExplicitArgument() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.to(ArgumentAdviceExplicit.class).on(named(QUX)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(QUX, String.class, String.class).invoke(type.getConstructor().newInstance(), FOO, BAR), is((Object) (FOO + BAR)));
        assertThat(type.getDeclaredField(ENTER).get(null), is((Object) 1));
        assertThat(type.getDeclaredField(EXIT).get(null), is((Object) 1));
    }

    @Test
    public void testAdviceWithIncrement() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(IncrementSample.class)
                .visit(Advice.to(IncrementAdvice.class).on(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO, int.class).invoke(type.getConstructor().newInstance(), 0), is((Object) 2));
    }

    @Test
    public void testAdviceWithThisReference() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.to(ThisReferenceAdvice.class).on(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO).invoke(type.getConstructor().newInstance()), is((Object) FOO));
        assertThat(type.getDeclaredField(ENTER).get(null), is((Object) 1));
        assertThat(type.getDeclaredField(EXIT).get(null), is((Object) 1));
    }

    @Test
    public void testAdviceWithOptionalThisReferenceNonOptional() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.to(OptionalThisReferenceAdvice.class).on(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO).invoke(type.getConstructor().newInstance()), is((Object) FOO));
        assertThat(type.getDeclaredField(ENTER).get(null), is((Object) 1));
        assertThat(type.getDeclaredField(EXIT).get(null), is((Object) 1));
    }

    @Test
    public void testAdviceWithOptionalThisReferenceOptional() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.to(OptionalThisReferenceAdvice.class).on(named(BAZ)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(BAZ).invoke(null), is((Object) FOO));
        assertThat(type.getDeclaredField(ENTER).get(null), is((Object) 1));
        assertThat(type.getDeclaredField(EXIT).get(null), is((Object) 1));
    }

    @Test
    public void testAdviceWithEntranceValue() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.to(EntranceValueAdvice.class).on(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO).invoke(type.getConstructor().newInstance()), is((Object) FOO));
        assertThat(type.getDeclaredField(ENTER).get(null), is((Object) 1));
        assertThat(type.getDeclaredField(EXIT).get(null), is((Object) 1));
    }

    @Test
    public void testAdviceWithReturnValue() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.to(ReturnValueAdvice.class).on(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO).invoke(type.getConstructor().newInstance()), is((Object) FOO));
        assertThat(type.getDeclaredField(ENTER).get(null), is((Object) 0));
        assertThat(type.getDeclaredField(EXIT).get(null), is((Object) 1));
    }

    @Test
    public void testAdviceWithExceptionHandler() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.to(ExceptionHandlerAdvice.class).on(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO).invoke(type.getConstructor().newInstance()), is((Object) FOO));
        assertThat(type.getDeclaredField(ENTER).get(null), is((Object) 1));
        assertThat(type.getDeclaredField(EXIT).get(null), is((Object) 1));
    }

    @Test
    public void testAdviceWithExceptionHandlerNested() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.to(ExceptionHandlerAdvice.class).on(named(FOO)))
                .visit(Advice.to(ExceptionHandlerAdvice.class).on(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO).invoke(type.getConstructor().newInstance()), is((Object) FOO));
        assertThat(type.getDeclaredField(ENTER).get(null), is((Object) 2));
        assertThat(type.getDeclaredField(EXIT).get(null), is((Object) 2));
    }

    @Test
    public void testAdviceNotSkipExceptionImplicit() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.to(TrivialAdvice.class).on(named(FOO + BAR)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        try {
            type.getDeclaredMethod(FOO + BAR).invoke(type.getConstructor().newInstance());
            fail();
        } catch (InvocationTargetException exception) {
            assertThat(exception.getCause(), instanceOf(RuntimeException.class));
        }
        assertThat(type.getDeclaredField(ENTER).get(null), is((Object) 1));
        assertThat(type.getDeclaredField(EXIT).get(null), is((Object) 1));
    }

    @Test
    public void testAdviceNotSkipExceptionImplicitNested() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.to(TrivialAdvice.class).on(named(FOO + BAR)))
                .visit(Advice.to(TrivialAdvice.class).on(named(FOO + BAR)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        try {
            type.getDeclaredMethod(FOO + BAR).invoke(type.getConstructor().newInstance());
            fail();
        } catch (InvocationTargetException exception) {
            assertThat(exception.getCause(), instanceOf(RuntimeException.class));
        }
        assertThat(type.getDeclaredField(ENTER).get(null), is((Object) 2));
        assertThat(type.getDeclaredField(EXIT).get(null), is((Object) 2));
    }

    @Test
    public void testAdviceSkipExceptionImplicit() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.to(TrivialAdviceSkipException.class).on(named(FOO + BAR)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        try {
            type.getDeclaredMethod(FOO + BAR).invoke(type.getConstructor().newInstance());
            fail();
        } catch (InvocationTargetException exception) {
            assertThat(exception.getCause(), instanceOf(RuntimeException.class));
        }
        assertThat(type.getDeclaredField(ENTER).get(null), is((Object) 1));
        assertThat(type.getDeclaredField(EXIT).get(null), is((Object) 0));
    }

    @Test
    public void testAdviceNotSkipExceptionExplicit() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.to(TrivialAdvice.class).on(named(BAR + BAZ)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        try {
            type.getDeclaredMethod(BAR + BAZ).invoke(type.getConstructor().newInstance());
            fail();
        } catch (InvocationTargetException exception) {
            assertThat(exception.getCause(), instanceOf(NullPointerException.class));
        }
        assertThat(type.getDeclaredField(ENTER).get(null), is((Object) 1));
        assertThat(type.getDeclaredField(EXIT).get(null), is((Object) 1));
    }

    @Test
    public void testAdviceNotSkipExceptionExplicitNested() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.to(TrivialAdvice.class).on(named(BAR + BAZ)))
                .visit(Advice.to(TrivialAdvice.class).on(named(BAR + BAZ)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        try {
            type.getDeclaredMethod(BAR + BAZ).invoke(type.getConstructor().newInstance());
            fail();
        } catch (InvocationTargetException exception) {
            assertThat(exception.getCause(), instanceOf(NullPointerException.class));
        }
        assertThat(type.getDeclaredField(ENTER).get(null), is((Object) 2));
        assertThat(type.getDeclaredField(EXIT).get(null), is((Object) 2));
    }

    @Test
    public void testAdviceSkipExceptionExplicit() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.to(TrivialAdviceSkipException.class).on(named(BAR + BAZ)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        try {
            type.getDeclaredMethod(BAR + BAZ).invoke(type.getConstructor().newInstance());
            fail();
        } catch (InvocationTargetException exception) {
            assertThat(exception.getCause(), instanceOf(NullPointerException.class));
        }
        assertThat(type.getDeclaredField(ENTER).get(null), is((Object) 1));
        assertThat(type.getDeclaredField(EXIT).get(null), is((Object) 0));
    }

    @Test
    public void testAdviceSkipExceptionDoesNotSkipNonException() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.to(TrivialAdviceSkipException.class).on(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO).invoke(type.getConstructor().newInstance()), is((Object) FOO));
        assertThat(type.getDeclaredField(ENTER).get(null), is((Object) 1));
        assertThat(type.getDeclaredField(EXIT).get(null), is((Object) 1));
    }

    @Test
    public void testObsoleteReturnValue() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.to(ObsoleteReturnValueAdvice.class).on(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO).invoke(type.getConstructor().newInstance()), is((Object) FOO));
        assertThat(type.getDeclaredField(ENTER).get(null), is((Object) 1));
        assertThat(type.getDeclaredField(EXIT).get(null), is((Object) 0));
    }

    @Test
    public void testUnusedReturnValue() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.to(UnusedReturnValueAdvice.class).on(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO).invoke(type.getConstructor().newInstance()), is((Object) FOO));
        assertThat(type.getDeclaredField(ENTER).get(null), is((Object) 1));
        assertThat(type.getDeclaredField(EXIT).get(null), is((Object) 1));
    }

    @Test
    public void testVariableMappingAdviceLarger() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.to(AdviceWithVariableValues.class).on(named(BAR)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(BAR, String.class).invoke(type.getConstructor().newInstance(), FOO + BAR + QUX + BAZ), is((Object) (FOO + BAR + QUX + BAZ)));
        assertThat(type.getDeclaredField(ENTER).get(null), is((Object) 1));
        assertThat(type.getDeclaredField(EXIT).get(null), is((Object) 1));
    }

    @Test
    public void testVariableMappingInstrumentedLarger() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.to(AdviceWithVariableValues.class).on(named(QUX + BAZ)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(QUX + BAZ).invoke(type.getConstructor().newInstance()), is((Object) (FOO + BAR + QUX + BAZ)));
        assertThat(type.getDeclaredField(ENTER).get(null), is((Object) 1));
        assertThat(type.getDeclaredField(EXIT).get(null), is((Object) 1));
    }

    @Test
    public void testExceptionWhenNotThrown() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.to(ThrowableAdvice.class).on(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO).invoke(type.getConstructor().newInstance()), is((Object) (FOO)));
        assertThat(type.getDeclaredField(THROWABLE).get(null), nullValue(Object.class));
    }

    @Test
    public void testExceptionWhenThrown() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.to(ThrowableAdvice.class).on(named(FOO + BAR)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        try {
            type.getDeclaredMethod(FOO + BAR).invoke(type.getConstructor().newInstance());
            fail();
        } catch (InvocationTargetException exception) {
            assertThat(exception.getCause(), instanceOf(RuntimeException.class));
        }
        assertThat(type.getDeclaredField(THROWABLE).get(null), instanceOf(RuntimeException.class));
    }

    @Test
    public void testAdviceThrowOnEnter() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(TracableSample.class)
                .visit(Advice.to(ThrowOnEnter.class).on(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        try {
            type.getDeclaredMethod(FOO).invoke(type.getConstructor().newInstance());
            fail();
        } catch (InvocationTargetException exception) {
            assertThat(exception.getCause(), instanceOf(RuntimeException.class));
        }
        assertThat(type.getDeclaredField(ENTER).get(null), is((Object) 1));
        assertThat(type.getDeclaredField(INSIDE).get(null), is((Object) 0));
        assertThat(type.getDeclaredField(EXIT).get(null), is((Object) 0));
    }

    @Test
    public void testAdviceThrowOnExit() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(TracableSample.class)
                .visit(Advice.to(ThrowOnExit.class).on(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        try {
            type.getDeclaredMethod(FOO).invoke(type.getConstructor().newInstance());
            fail();
        } catch (InvocationTargetException exception) {
            assertThat(exception.getCause(), instanceOf(RuntimeException.class));
        }
        assertThat(type.getDeclaredField(ENTER).get(null), is((Object) 1));
        assertThat(type.getDeclaredField(INSIDE).get(null), is((Object) 1));
        assertThat(type.getDeclaredField(EXIT).get(null), is((Object) 1));
    }

    @Test
    public void testAdviceThrowSuppressed() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(TracableSample.class)
                .visit(Advice.to(ThrowSuppressed.class).on(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        type.getDeclaredMethod(FOO).invoke(type.getConstructor().newInstance());
        assertThat(type.getDeclaredField(ENTER).get(null), is((Object) 1));
        assertThat(type.getDeclaredField(INSIDE).get(null), is((Object) 1));
        assertThat(type.getDeclaredField(EXIT).get(null), is((Object) 1));
    }

    @Test
    public void testAdviceThrowNotSuppressedOnEnter() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(TracableSample.class)
                .visit(Advice.to(ThrowNotSuppressedOnEnter.class).on(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        try {
            type.getDeclaredMethod(FOO).invoke(type.getConstructor().newInstance());
            fail();
        } catch (InvocationTargetException exception) {
            assertThat(exception.getCause(), instanceOf(Exception.class));
        }
        assertThat(type.getDeclaredField(ENTER).get(null), is((Object) 1));
        assertThat(type.getDeclaredField(INSIDE).get(null), is((Object) 0));
        assertThat(type.getDeclaredField(EXIT).get(null), is((Object) 0));
    }

    @Test
    public void testAdviceThrowNotSuppressedOnExit() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(TracableSample.class)
                .visit(Advice.to(ThrowNotSuppressedOnExit.class).on(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        try {
            type.getDeclaredMethod(FOO).invoke(type.getConstructor().newInstance());
            fail();
        } catch (InvocationTargetException exception) {
            assertThat(exception.getCause(), instanceOf(Exception.class));
        }
        assertThat(type.getDeclaredField(ENTER).get(null), is((Object) 1));
        assertThat(type.getDeclaredField(INSIDE).get(null), is((Object) 1));
        assertThat(type.getDeclaredField(EXIT).get(null), is((Object) 1));
    }

    @Test
    public void testThisValueSubstitution() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Box.class)
                .visit(Advice.to(ThisSubstitutionAdvice.class).on(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO).invoke(type.getDeclaredConstructor(String.class).newInstance(FOO)), is((Object) BAR));
    }

    @Test
    public void testThisValueSubstitutionOptional() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Box.class)
                .visit(Advice.to(ThisOptionalSubstitutionAdvice.class).on(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO).invoke(type.getDeclaredConstructor(String.class).newInstance(FOO)), is((Object) BAR));
    }

    @Test(expected = IllegalStateException.class)
    public void testIllegalThisValueSubstitution() throws Exception {
        new ByteBuddy()
                .redefine(Box.class)
                .visit(Advice.to(IllegalThisSubstitutionAdvice.class).on(named(FOO)))
                .make();
    }

    @Test
    public void testParameterValueSubstitution() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Box.class)
                .visit(Advice.to(ParameterSubstitutionAdvice.class).on(named(BAR)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(BAR, String.class).invoke(null, FOO), is((Object) BAR));
    }

    @Test(expected = IllegalStateException.class)
    public void testIllegalParameterValueSubstitution() throws Exception {
        new ByteBuddy()
                .redefine(Box.class)
                .visit(Advice.to(IllegalParameterSubstitutionAdvice.class).on(named(BAR)))
                .make();
    }

    @Test
    public void testReturnValueSubstitution() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.to(ReturnSubstitutionAdvice.class).on(named(BAR)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(BAR, String.class).invoke(type.getConstructor().newInstance(), FOO), is((Object) BAR));
    }

    @Test(expected = IllegalStateException.class)
    public void testIllegalReturnValueSubstitution() throws Exception {
        new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.to(IllegalReturnSubstitutionAdvice.class).on(named(FOO)))
                .make();
    }

    @Test
    public void testEnterValueSubstitution() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.to(EnterSubstitutionAdvice.class).on(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO).invoke(type.getConstructor().newInstance()), is((Object) FOO));
    }

    @Test(expected = IllegalStateException.class)
    public void testIllegalEnterValueSubstitution() throws Exception {
        new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.to(IllegalEnterSubstitutionAdvice.class).on(named(BAR)))
                .make();
    }

    @Test
    public void testFieldAdviceImplicit() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(FieldSample.class)
                .visit(Advice.to(FieldAdviceImplicit.class).on(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO).invoke(type.getConstructor().newInstance()), is((Object) FOO));
        assertThat(type.getDeclaredField(ENTER).get(null), is((Object) 1));
        assertThat(type.getDeclaredField(EXIT).get(null), is((Object) 1));
    }

    @Test
    public void testFieldAdviceExplicit() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(FieldSample.class)
                .visit(Advice.to(FieldAdviceExplicit.class).on(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO).invoke(type.getConstructor().newInstance()), is((Object) FOO));
        assertThat(type.getDeclaredField(ENTER).get(null), is((Object) 1));
        assertThat(type.getDeclaredField(EXIT).get(null), is((Object) 1));
    }

    @Test
    public void testOriginAdvice() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.to(OriginAdvice.class).on(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO).invoke(type.getConstructor().newInstance()), is((Object) FOO));
        assertThat(type.getDeclaredField(ENTER).get(null), is((Object) 1));
        assertThat(type.getDeclaredField(EXIT).get(null), is((Object) 1));
    }

    @Test
    public void testOriginCustomAdvice() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.to(OriginCustomAdvice.class).on(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO).invoke(type.getConstructor().newInstance()), is((Object) FOO));
        assertThat(type.getDeclaredField(ENTER).get(null), is((Object) 1));
        assertThat(type.getDeclaredField(EXIT).get(null), is((Object) 1));
    }

    @Test
    public void testExceptionSuppressionAdvice() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.to(ExceptionSuppressionAdvice.class).on(named(FOO + BAR)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO + BAR).invoke(type.getConstructor().newInstance()), nullValue(Object.class));
    }

    @Test
    public void testExceptionTypeAdvice() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(ExceptionTypeAdvice.class)
                .visit(Advice.to(ExceptionTypeAdvice.class).on(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        try {
            type.getDeclaredMethod(FOO).invoke(type.getConstructor().newInstance());
            fail();
        } catch (InvocationTargetException exception) {
            assertThat(exception.getCause(), instanceOf(IllegalStateException.class));
        }
        assertThat(type.getDeclaredField(ENTER).get(null), is((Object) 1));
        assertThat(type.getDeclaredField(EXIT).get(null), is((Object) 1));
    }

    @Test
    public void testExceptionNotCatchedAdvice() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(ExceptionNotCatchedAdvice.class)
                .visit(Advice.to(ExceptionNotCatchedAdvice.class).on(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        try {
            type.getDeclaredMethod(FOO).invoke(type.getConstructor().newInstance());
            fail();
        } catch (InvocationTargetException exception) {
            assertThat(exception.getCause(), instanceOf(Exception.class));
        }
        assertThat(type.getDeclaredField(ENTER).get(null), is((Object) 1));
        assertThat(type.getDeclaredField(EXIT).get(null), is((Object) 0));
    }

    @Test
    public void testExceptionCatchedAdvice() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(ExceptionCatchedAdvice.class)
                .visit(Advice.to(ExceptionCatchedAdvice.class).on(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        try {
            type.getDeclaredMethod(FOO).invoke(type.getConstructor().newInstance());
            fail();
        } catch (InvocationTargetException exception) {
            assertThat(exception.getCause(), instanceOf(RuntimeException.class));
        }
        assertThat(type.getDeclaredField(ENTER).get(null), is((Object) 1));
        assertThat(type.getDeclaredField(EXIT).get(null), is((Object) 1));
    }

    @Test
    public void testExceptionCatchedWithExchangeAdvice() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(ExceptionCatchedWithExchangeAdvice.class)
                .visit(Advice.to(ExceptionCatchedWithExchangeAdvice.class).on(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        try {
            type.getDeclaredMethod(FOO).invoke(type.getConstructor().newInstance());
            fail();
        } catch (InvocationTargetException exception) {
            assertThat(exception.getCause(), instanceOf(IOException.class));
        }
        assertThat(type.getDeclaredField(ENTER).get(null), is((Object) 1));
        assertThat(type.getDeclaredField(EXIT).get(null), is((Object) 1));
    }

    @Test
    public void testNonAssignableCasting() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(NonAssignableReturnTypeAdvice.class)
                .visit(Advice.to(NonAssignableReturnTypeAdvice.class).on(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        try {
            type.getDeclaredMethod(FOO).invoke(type.getConstructor().newInstance());
            fail();
        } catch (InvocationTargetException exception) {
            assertThat(exception.getCause(), instanceOf(ClassCastException.class));
        }
    }

    @Test
    public void testTrivialAssignableCasting() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(TrivialReturnTypeAdvice.class)
                .visit(Advice.to(TrivialReturnTypeAdvice.class).on(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO).invoke(type.getConstructor().newInstance()), is((Object) BAR));
    }

    @Test
    public void testPrimitiveNonAssignableCasting() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(NonAssignablePrimitiveReturnTypeAdvice.class)
                .visit(Advice.to(NonAssignablePrimitiveReturnTypeAdvice.class).on(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        try {
            type.getDeclaredMethod(FOO).invoke(type.getConstructor().newInstance());
            fail();
        } catch (InvocationTargetException exception) {
            assertThat(exception.getCause(), instanceOf(ClassCastException.class));
        }
    }

    @Test
    public void testUserTypeValue() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.withCustomMapping().bind(Custom.class, new Advice.DynamicValue<Custom>() {
                    @Override
                    public Object resolve(MethodDescription.InDefinedShape instrumentedMethod,
                                          ParameterDescription.InDefinedShape target,
                                          AnnotationDescription.Loadable<Custom> annotation,
                                          boolean initialized) {
                        return Object.class;
                    }
                }).to(CustomAdvice.class).on(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO).invoke(type.getConstructor().newInstance()), is((Object) FOO));
    }

    @Test
    public void testUserUnloadedTypeValue() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.withCustomMapping().bind(Custom.class, new Advice.DynamicValue<Custom>() {
                    @Override
                    public Object resolve(MethodDescription.InDefinedShape instrumentedMethod,
                                          ParameterDescription.InDefinedShape target,
                                          AnnotationDescription.Loadable<Custom> annotation,
                                          boolean initialized) {
                        return TypeDescription.OBJECT;
                    }
                }).to(CustomAdvice.class).on(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO).invoke(type.getConstructor().newInstance()), is((Object) FOO));
    }

    @Test
    public void testUserSerializableTypeValue() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.withCustomMapping().bind(Custom.class, new Advice.DynamicValue<Custom>() {
                    @Override
                    public Object resolve(MethodDescription.InDefinedShape instrumentedMethod,
                                          ParameterDescription.InDefinedShape target,
                                          AnnotationDescription.Loadable<Custom> annotation,
                                          boolean initialized) {
                        return Collections.singletonMap(FOO, BAR);
                    }
                }).to(CustomSerializableAdvice.class).on(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO).invoke(type.getConstructor().newInstance()), is((Object) FOO));
    }

    @Test
    public void testSkipInstrumentedMethod() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(SkipIfTrueAdvice.class)
                .visit(Advice.to(SkipIfTrueAdvice.class).on(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO).invoke(type.getConstructor().newInstance()), nullValue(Object.class));
        assertThat(type.getDeclaredField(ENTER).get(null), is((Object) 1));
        assertThat(type.getDeclaredField(EXIT).get(null), is((Object) 1));
        assertThat(type.getDeclaredField(FOO).get(null), is((Object) 0));
    }

    @Test
    public void testSkipInstrumentedMethodNonInlined() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(SkipIfTrueNonInlinedAdvice.class)
                .visit(Advice.to(SkipIfTrueNonInlinedAdvice.class).on(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO).invoke(type.getConstructor().newInstance()), nullValue(Object.class));
        assertThat(type.getDeclaredField(ENTER).get(null), is((Object) 1));
        assertThat(type.getDeclaredField(EXIT).get(null), is((Object) 1));
        assertThat(type.getDeclaredField(FOO).get(null), is((Object) 0));
    }

    @Test
    public void testSkipInstrumentedMethodWithSuppression() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(SkipIfTrueWithSuppressionAdvice.class)
                .visit(Advice.to(SkipIfTrueWithSuppressionAdvice.class).on(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO).invoke(type.getConstructor().newInstance()), is((Object) FOO));
        assertThat(type.getDeclaredField(ENTER).get(null), is((Object) 1));
        assertThat(type.getDeclaredField(EXIT).get(null), is((Object) 1));
        assertThat(type.getDeclaredField(FOO).get(null), is((Object) 1));
    }

    @Test
    public void testSkipInstrumentedMethodNonInlinedWithSuppression() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(SkipIfTrueNonInlinedWithSuppressionAdvice.class)
                .visit(Advice.to(SkipIfTrueNonInlinedWithSuppressionAdvice.class).on(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO).invoke(type.getConstructor().newInstance()), is((Object) FOO));
        assertThat(type.getDeclaredField(ENTER).get(null), is((Object) 1));
        assertThat(type.getDeclaredField(EXIT).get(null), is((Object) 1));
        assertThat(type.getDeclaredField(FOO).get(null), is((Object) 1));
    }

    @Test(expected = IllegalStateException.class)
    public void testUserSerializableTypeValueNonAssignable() throws Exception {
        new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.withCustomMapping().bind(Custom.class, new Advice.DynamicValue<Custom>() {
                    @Override
                    public Object resolve(MethodDescription.InDefinedShape instrumentedMethod,
                                          ParameterDescription.InDefinedShape target,
                                          AnnotationDescription.Loadable<Custom> annotation,
                                          boolean initialized) {
                        return Collections.singletonList(FOO);
                    }
                }).to(CustomSerializableAdvice.class).on(named(FOO)))
                .make();
    }

    @Test(expected = IllegalStateException.class)
    public void testIllegalUserValue() throws Exception {
        new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.withCustomMapping().bind(Custom.class, new Advice.DynamicValue<Custom>() {
                    @Override
                    public Object resolve(MethodDescription.InDefinedShape instrumentedMethod,
                                          ParameterDescription.InDefinedShape target,
                                          AnnotationDescription.Loadable<Custom> annotation,
                                          boolean initialized) {
                        return new Object();
                    }
                }).to(CustomAdvice.class).on(named(FOO)))
                .make();
    }

    @Test(expected = IllegalStateException.class)
    public void testIllegalPrimitiveNullUserValue() throws Exception {
        new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.withCustomMapping().bind(Custom.class, new Advice.DynamicValue<Custom>() {
                    @Override
                    public Object resolve(MethodDescription.InDefinedShape instrumentedMethod,
                                          ParameterDescription.InDefinedShape target,
                                          AnnotationDescription.Loadable<Custom> annotation,
                                          boolean initialized) {
                        return null;
                    }
                }).to(CustomPrimitiveAdvice.class).on(named(FOO)))
                .make();
    }

    @Test(expected = IllegalStateException.class)
    public void testAdviceWithThisReferenceOnConstructor() throws Exception {
        new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.to(ThisReferenceAdvice.class).on(isConstructor()))
                .make();
    }

    @Test(expected = IllegalStateException.class)
    public void testAdviceWithFieldOnConstructor() throws Exception {
        new ByteBuddy()
                .redefine(FieldSample.class)
                .visit(Advice.to(FieldAdviceExplicit.class).on(isConstructor()))
                .make();
    }

    @Test(expected = IllegalStateException.class)
    public void testAdviceWithExceptionCatchOnConstructor() throws Exception {
        new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.to(EmptyAdviceExitWithExceptionHandling.class).on(isConstructor()))
                .make();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAdviceWithoutAnnotations() throws Exception {
        Advice.to(Object.class);
    }

    @Test(expected = IllegalStateException.class)
    public void testDuplicateAdvice() throws Exception {
        Advice.to(DuplicateAdvice.class);
    }

    @Test(expected = IllegalStateException.class)
    public void testIOExceptionOnRead() throws Exception {
        ClassFileLocator classFileLocator = mock(ClassFileLocator.class);
        when(classFileLocator.locate(TrivialAdvice.class.getName())).thenThrow(new IOException());
        Advice.to(TrivialAdvice.class, classFileLocator);
    }

    @Test(expected = IllegalStateException.class)
    public void testNonStaticAdvice() throws Exception {
        Advice.to(NonStaticAdvice.class);
    }

    @Test(expected = IllegalStateException.class)
    public void testAmbiguousAdvice() throws Exception {
        Advice.to(AmbiguousAdvice.class);
    }

    @Test(expected = IllegalStateException.class)
    public void testAmbiguousAdviceDelegation() throws Exception {
        Advice.to(AmbiguousAdviceDelegation.class);
    }

    @Test(expected = IllegalStateException.class)
    public void testCannotBindEnterToEnter() throws Exception {
        Advice.to(EnterToEnterAdvice.class);
    }

    @Test(expected = IllegalStateException.class)
    public void testCannotBindEnterToReturn() throws Exception {
        Advice.to(EnterToReturnAdvice.class);
    }

    @Test(expected = IllegalStateException.class)
    public void testAdviceWithNonExistentArgument() throws Exception {
        new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.to(IllegalArgumentAdvice.class).on(named(FOO)))
                .make();
    }

    @Test(expected = IllegalStateException.class)
    public void testAdviceWithNonAssignableParameterImplicit() throws Exception {
        new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.to(IllegalArgumentAdvice.class).on(named(BAR)))
                .make();
    }

    @Test(expected = IllegalStateException.class)
    public void testAdviceWithNonAssignableParameter() throws Exception {
        new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.to(IllegalArgumentWritableAdvice.class).on(named(BAR)))
                .make();
    }

    @Test(expected = IllegalStateException.class)
    public void testAdviceWithNonEqualParameter() throws Exception {
        new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.to(IllegalArgumentReadOnlyAdvice.class).on(named(BAR)))
                .make();
    }

    @Test(expected = IllegalStateException.class)
    public void testAdviceThisReferenceNonExistent() throws Exception {
        new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.to(ThisReferenceAdvice.class).on(named(BAZ)))
                .make();
    }

    @Test(expected = IllegalStateException.class)
    public void testAdviceWithNonAssignableThisReference() throws Exception {
        new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.to(IllegalThisReferenceAdvice.class).on(named(FOO)))
                .make();
    }

    @Test(expected = IllegalStateException.class)
    public void testAdviceWithNonEqualThisReference() throws Exception {
        new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.to(IllegalThisReferenceWritableAdvice.class).on(named(FOO)))
                .make();
    }

    @Test(expected = IllegalStateException.class)
    public void testAdviceWithNonAssignableReturnValue() throws Exception {
        new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.to(NonAssignableReturnAdvice.class).on(named(FOO + QUX)))
                .make();
    }

    @Test(expected = IllegalStateException.class)
    public void testAdviceWithNonAssignableReturnValueWritable() throws Exception {
        new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.to(NonEqualReturnWritableAdvice.class).on(named(FOO + QUX)))
                .make();
    }

    @Test
    public void testAdviceAbstractMethodIsSkipped() throws Exception {
        Advice.Dispatcher.Resolved.ForMethodEnter methodEnter = mock(Advice.Dispatcher.Resolved.ForMethodEnter.class);
        Advice.Dispatcher.Resolved.ForMethodExit methodExit = mock(Advice.Dispatcher.Resolved.ForMethodExit.class);
        MethodDescription.InDefinedShape methodDescription = mock(MethodDescription.InDefinedShape.class);
        when(methodDescription.isAbstract()).thenReturn(true);
        MethodVisitor methodVisitor = mock(MethodVisitor.class);
        assertThat(new Advice(methodEnter, methodExit).wrap(mock(TypeDescription.class),
                methodDescription,
                methodVisitor,
                mock(ClassFileVersion.class),
                0,
                0), sameInstance(methodVisitor));
    }

    @Test
    public void testAdviceNativeMethodIsSkipped() throws Exception {
        Advice.Dispatcher.Resolved.ForMethodEnter methodEnter = mock(Advice.Dispatcher.Resolved.ForMethodEnter.class);
        Advice.Dispatcher.Resolved.ForMethodExit methodExit = mock(Advice.Dispatcher.Resolved.ForMethodExit.class);
        MethodDescription.InDefinedShape methodDescription = mock(MethodDescription.InDefinedShape.class);
        when(methodDescription.isNative()).thenReturn(true);
        MethodVisitor methodVisitor = mock(MethodVisitor.class);
        assertThat(new Advice(methodEnter, methodExit).wrap(mock(TypeDescription.class),
                methodDescription,
                methodVisitor,
                mock(ClassFileVersion.class),
                0,
                0), sameInstance(methodVisitor));
    }

    @Test(expected = IllegalStateException.class)
    public void testAdviceWithNonAssignableEnterValue() throws Exception {
        Advice.to(NonAssignableEnterAdvice.class);
    }

    @Test(expected = IllegalStateException.class)
    public void testIllegalThrowableRequest() throws Exception {
        Advice.to(IllegalThrowableRequestAdvice.class);
    }

    @Test(expected = IllegalStateException.class)
    public void testIllegalThrowableType() throws Exception {
        Advice.to(IllegalThrowableTypeAdvice.class);
    }

    @Test(expected = IllegalStateException.class)
    public void testFieldIllegalExplicit() throws Exception {
        new ByteBuddy()
                .redefine(FieldSample.class)
                .visit(Advice.to(FieldAdviceIllegalExplicit.class).on(named(FOO)))
                .make();
    }

    @Test(expected = IllegalStateException.class)
    public void testFieldNonExistent() throws Exception {
        new ByteBuddy()
                .redefine(FieldSample.class)
                .visit(Advice.to(FieldAdviceNonExistent.class).on(named(FOO)))
                .make();
    }

    @Test(expected = IllegalStateException.class)
    public void testFieldNonAssignable() throws Exception {
        new ByteBuddy()
                .redefine(FieldSample.class)
                .visit(Advice.to(FieldAdviceNonAssignable.class).on(named(FOO)))
                .make();
    }

    @Test(expected = IllegalStateException.class)
    public void testFieldWrite() throws Exception {
        new ByteBuddy()
                .redefine(FieldSample.class)
                .visit(Advice.to(FieldAdviceWrite.class).on(named(FOO)))
                .make();
    }

    @Test(expected = IllegalStateException.class)
    public void testIllegalOriginType() throws Exception {
        Advice.to(IllegalOriginType.class);
    }

    @Test(expected = IllegalStateException.class)
    public void testIllegalOriginPattern() throws Exception {
        Advice.to(IllegalOriginPattern.class);
    }

    @Test(expected = IllegalStateException.class)
    public void testIllegalOriginPatternEnd() throws Exception {
        Advice.to(IllegalOriginPatternEnd.class);
    }

    @Test(expected = IllegalStateException.class)
    public void testCannotWriteOrigin() throws Exception {
        new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.to(IllegalOriginWriteAdvice.class).on(named(FOO)))
                .make();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDuplicateRegistration() throws Exception {
        Advice.withCustomMapping().bind(Custom.class, new Advice.DynamicValue<Annotation>() {
            @Override
            public Object resolve(MethodDescription.InDefinedShape instrumentedMethod,
                                  ParameterDescription.InDefinedShape target,
                                  AnnotationDescription.Loadable<Annotation> annotation,
                                  boolean initialized) {
                return null;
            }
        }).bind(Custom.class, (Serializable) null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNotAnnotationType() throws Exception {
        Advice.withCustomMapping().bind(Annotation.class, (Serializable) null);
    }

    @Test(expected = IllegalStateException.class)
    public void testInlineAdviceCannotWriteParameter() throws Exception {
        Advice.to(IllegalArgumentWritableInlineAdvice.class);
    }

    @Test(expected = IllegalStateException.class)
    public void testInlineAdviceCannotWriteThis() throws Exception {
        Advice.to(IllegalThisWritableInlineAdvice.class);
    }

    @Test(expected = IllegalStateException.class)
    public void testInlineAdviceCannotWriteField() throws Exception {
        Advice.to(IllegalFieldWritableInlineAdvice.class);
    }

    @Test(expected = IllegalStateException.class)
    public void testInlineAdviceCannotWriteThrow() throws Exception {
        Advice.to(IllegalThrowWritableInlineAdvice.class);
    }

    @Test(expected = IllegalStateException.class)
    public void testInlineAdviceCannotWriteReturn() throws Exception {
        Advice.to(IllegalThrowWritableInlineAdvice.class);
    }

    @Test(expected = IllegalStateException.class)
    public void testInvisibleDelegationAdvice() throws Exception {
        new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.to(NonVisibleAdvice.class).on(named(FOO)))
                .make();
    }

    @Test(expected = IllegalStateException.class)
    public void testNonResolvedAdvice() throws Exception {
        Advice.to(new TypeDescription.ForLoadedType(TrivialAdvice.class));
    }

    @Test(expected = IllegalStateException.class)
    public void testSkipIfTrueNonCompatibleReturnType() throws Exception {
        Advice.to(SkipIfTrueNonCompatibleReturnType.class);
    }

    @Test
    public void testCannotInstantiateSuppressionMarker() throws Exception {
        Class<?> type = Class.forName(Advice.class.getName() + "$NoExceptionHandler");
        assertThat(Modifier.isPrivate(type.getModifiers()), is(true));
        try {
            Constructor<?> constructor = type.getDeclaredConstructor();
            assertThat(Modifier.isPrivate(constructor.getModifiers()), is(true));
            constructor.setAccessible(true);
            constructor.newInstance();
            fail();
        } catch (InvocationTargetException exception) {
            assertThat(exception.getCause(), instanceOf(UnsupportedOperationException.class));
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDistributedAdviceNoEnterAdvice() throws Exception {
        Advice.to(Object.class, EmptyAdvice.class);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDistributedAdviceNoExitAdvice() throws Exception {
        Advice.to(EmptyAdvice.class, Object.class);
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(Advice.class).apply();
        ObjectPropertyAssertion.of(Advice.WithCustomMapping.class).apply();
        ObjectPropertyAssertion.of(Advice.MethodSizeHandler.NoOp.class).apply();
        ObjectPropertyAssertion.of(Advice.MethodSizeHandler.Default.class).applyBasic();
        ObjectPropertyAssertion.of(Advice.MethodSizeHandler.Default.ForAdvice.class).refine(new ObjectPropertyAssertion.Refinement<MethodDescription>() {
            @Override
            public void apply(MethodDescription mock) {
                when(mock.getReturnType()).thenReturn(TypeDescription.Generic.VOID);
            }
        }).applyBasic();
        ObjectPropertyAssertion.of(Advice.StackMapFrameHandler.NoOp.class).apply();
        ObjectPropertyAssertion.of(Advice.StackMapFrameHandler.Default.class).applyBasic();
        ObjectPropertyAssertion.of(Advice.StackMapFrameHandler.Default.ForAdvice.class).applyBasic();
        ObjectPropertyAssertion.of(Advice.StackMapFrameHandler.Default.TranslationMode.class).apply();
        ObjectPropertyAssertion.of(Advice.Dispatcher.SuppressionHandler.NoOp.class).apply();
        ObjectPropertyAssertion.of(Advice.Dispatcher.SuppressionHandler.Suppressing.class).apply();
        ObjectPropertyAssertion.of(Advice.Dispatcher.SuppressionHandler.Suppressing.Bound.class).applyBasic();
        ObjectPropertyAssertion.of(Advice.Dispatcher.Inactive.class).apply();
        ObjectPropertyAssertion.of(Advice.Dispatcher.OffsetMapping.Context.ForMethodEntry.class).apply();
        ObjectPropertyAssertion.of(Advice.Dispatcher.OffsetMapping.Context.ForMethodExit.class).apply();
        ObjectPropertyAssertion.of(Advice.Dispatcher.OffsetMapping.Target.ForParameter.ReadOnly.class).apply();
        ObjectPropertyAssertion.of(Advice.Dispatcher.OffsetMapping.Target.ForParameter.ReadWrite.class).apply();
        ObjectPropertyAssertion.of(Advice.Dispatcher.OffsetMapping.Target.ForParameter.ReadWrite.WithCasting.class).apply();
        ObjectPropertyAssertion.of(Advice.Dispatcher.OffsetMapping.Target.ForField.ReadOnly.class).apply();
        ObjectPropertyAssertion.of(Advice.Dispatcher.OffsetMapping.Target.ForField.ReadWrite.class).apply();
        ObjectPropertyAssertion.of(Advice.Dispatcher.OffsetMapping.Target.ForConstantPoolValue.class).apply();
        ObjectPropertyAssertion.of(Advice.Dispatcher.OffsetMapping.Target.ForDefaultValue.class).apply();
        ObjectPropertyAssertion.of(Advice.Dispatcher.OffsetMapping.Target.ForBoxedArguments.class).apply();
        ObjectPropertyAssertion.of(Advice.Dispatcher.OffsetMapping.Target.ForBoxedParameter.ReadOnly.class).apply();
        ObjectPropertyAssertion.of(Advice.Dispatcher.OffsetMapping.Target.ForBoxedParameter.ReadWrite.class).apply();
        ObjectPropertyAssertion.of(Advice.Dispatcher.OffsetMapping.Target.ForBoxedParameter.BoxingDispatcher.class).apply();
        ObjectPropertyAssertion.of(Advice.Dispatcher.OffsetMapping.Target.ForSerializedObject.class).apply();
        ObjectPropertyAssertion.of(Advice.Dispatcher.OffsetMapping.Target.ForNullConstant.class).apply();
        final int[] value = new int[1];
        ObjectPropertyAssertion.of(Advice.Dispatcher.OffsetMapping.ForParameter.class).refine(new ObjectPropertyAssertion.Refinement<Advice.Argument>() {
            @Override
            public void apply(Advice.Argument mock) {
                when(mock.value()).thenReturn(value[0]++);
            }
        }).apply();
        ObjectPropertyAssertion.of(Advice.Dispatcher.OffsetMapping.ForParameter.Factory.class).apply();
        ObjectPropertyAssertion.of(Advice.Dispatcher.OffsetMapping.ForThisReference.class).apply();
        ObjectPropertyAssertion.of(Advice.Dispatcher.OffsetMapping.ForThisReference.Factory.class).apply();
        ObjectPropertyAssertion.of(Advice.Dispatcher.OffsetMapping.ForField.WithImplicitType.class).apply();
        ObjectPropertyAssertion.of(Advice.Dispatcher.OffsetMapping.ForField.WithExplicitType.class).apply();
        ObjectPropertyAssertion.of(Advice.Dispatcher.OffsetMapping.ForField.Factory.class).apply();
        ObjectPropertyAssertion.of(Advice.Dispatcher.OffsetMapping.ForBoxedReturnValue.class).apply();
        ObjectPropertyAssertion.of(Advice.Dispatcher.OffsetMapping.ForBoxedReturnValue.Factory.class).apply();
        ObjectPropertyAssertion.of(Advice.Dispatcher.OffsetMapping.ForBoxedArguments.class).apply();
        ObjectPropertyAssertion.of(Advice.Dispatcher.OffsetMapping.ForOrigin.class).apply();
        ObjectPropertyAssertion.of(Advice.Dispatcher.OffsetMapping.ForOrigin.Factory.class).apply();
        ObjectPropertyAssertion.of(Advice.Dispatcher.OffsetMapping.ForOrigin.Renderer.ForConstantValue.class).apply();
        ObjectPropertyAssertion.of(Advice.Dispatcher.OffsetMapping.ForOrigin.Renderer.ForDescriptor.class).apply();
        ObjectPropertyAssertion.of(Advice.Dispatcher.OffsetMapping.ForOrigin.Renderer.ForMethodName.class).apply();
        ObjectPropertyAssertion.of(Advice.Dispatcher.OffsetMapping.ForOrigin.Renderer.ForStringRepresentation.class).apply();
        ObjectPropertyAssertion.of(Advice.Dispatcher.OffsetMapping.ForOrigin.Renderer.ForTypeName.class).apply();
        ObjectPropertyAssertion.of(Advice.Dispatcher.OffsetMapping.ForOrigin.Renderer.ForReturnTypeName.class).apply();
        ObjectPropertyAssertion.of(Advice.Dispatcher.OffsetMapping.ForOrigin.Renderer.ForJavaSignature.class).apply();
        ObjectPropertyAssertion.of(Advice.Dispatcher.OffsetMapping.ForIgnored.class).apply();
        ObjectPropertyAssertion.of(Advice.Dispatcher.OffsetMapping.ForUserValue.class).apply();
        final Iterator<Class<?>> annotationTypes = Arrays.<Class<?>>asList(Object.class, String.class, int.class, float.class).iterator();
        ObjectPropertyAssertion.of(Advice.Dispatcher.OffsetMapping.ForUserValue.Factory.class).create(new ObjectPropertyAssertion.Creator<Class<?>>() {
            @Override
            public Class<?> create() {
                return annotationTypes.next();
            }
        }).apply();
        ObjectPropertyAssertion.of(Advice.Dispatcher.OffsetMapping.ForInstrumentedType.class).apply();
        ObjectPropertyAssertion.of(Advice.Dispatcher.OffsetMapping.ForReturnValue.class).apply();
        ObjectPropertyAssertion.of(Advice.Dispatcher.OffsetMapping.ForReturnValue.Factory.class).apply();
        ObjectPropertyAssertion.of(Advice.Dispatcher.OffsetMapping.ForThrowable.class).apply();
        ObjectPropertyAssertion.of(Advice.Dispatcher.OffsetMapping.ForEnterValue.class).apply();
        ObjectPropertyAssertion.of(Advice.Dispatcher.OffsetMapping.ForEnterValue.Factory.class).apply();
        final Iterator<Class<?>> types = Arrays.<Class<?>>asList(Object.class, String.class).iterator();
        ObjectPropertyAssertion.of(Advice.Dispatcher.OffsetMapping.Illegal.class).create(new ObjectPropertyAssertion.Creator<Class<?>>() {
            @Override
            public Class<?> create() {
                return types.next();
            }
        }).apply();
        ObjectPropertyAssertion.of(Advice.Dispatcher.Inlining.class).apply();
        //ObjectPropertyAssertion.of(Advice.Dispatcher.Inlining.Resolved.ForMethodEnter.class).applyBasic();
        ObjectPropertyAssertion.of(Advice.Dispatcher.Inlining.Resolved.ForMethodEnter.AdviceMethodInliner.class).applyBasic();
        //ObjectPropertyAssertion.of(Advice.Dispatcher.Inlining.Resolved.ForMethodExit.WithExceptionHandler.class).applyBasic();
        //ObjectPropertyAssertion.of(Advice.Dispatcher.Inlining.Resolved.ForMethodExit.WithoutExceptionHandler.class).applyBasic();
        ObjectPropertyAssertion.of(Advice.Dispatcher.Inlining.Resolved.ForMethodExit.AdviceMethodInliner.class).applyBasic();
        ObjectPropertyAssertion.of(Advice.Dispatcher.Inlining.Resolved.AdviceMethodInliner.ExceptionTableSubstitutor.class).applyBasic();
        ObjectPropertyAssertion.of(Advice.Dispatcher.Inlining.Resolved.AdviceMethodInliner.ExceptionTableCollector.class).applyBasic();
        ObjectPropertyAssertion.of(Advice.Dispatcher.Inlining.Resolved.AdviceMethodInliner.ExceptionTableExtractor.class).applyBasic();
        ObjectPropertyAssertion.of(Advice.Dispatcher.Inlining.CodeTranslationVisitor.ForMethodEnter.class).applyBasic();
        ObjectPropertyAssertion.of(Advice.Dispatcher.Inlining.CodeTranslationVisitor.ForMethodExit.class).refine(new ObjectPropertyAssertion.Refinement<MethodDescription.InDefinedShape>() {
            @Override
            public void apply(MethodDescription.InDefinedShape mock) {
                when(mock.getReturnType()).thenReturn(TypeDescription.Generic.VOID);
            }
        }).applyBasic();
        ObjectPropertyAssertion.of(Advice.DynamicValue.ForFixedValue.class).apply();
        ObjectPropertyAssertion.of(Advice.DynamicValue.ForAnnotationProperty.class).apply();
        ObjectPropertyAssertion.of(Advice.Dispatcher.Resolved.ForMethodEnter.SkipDispatcher.class).apply();
    }

    @SuppressWarnings("unused")
    public static class Sample {

        private Object object;

        public static int enter, exit;

        public static Throwable throwable;

        public String foo() {
            return FOO;
        }

        public String foobar() {
            throw new RuntimeException();
        }

        public String bar(String argument) {
            return argument;
        }

        public String qux(String first, String second) {
            return first + second;
        }

        public static String baz() {
            return FOO;
        }

        public String quxbaz() {
            String foo = FOO, bar = BAR, qux = QUX, baz = BAZ;
            return foo + bar + qux + baz;
        }

        public void fooqux() {
            /* do nothing */
        }

        public void barbaz() {
            object.getClass(); // implicit null pointer
        }

        public String foobaz() {
            try {
                throw new Exception();
            } catch (Exception ignored) {
                return FOO;
            }
        }
    }

    public static class TracableSample {

        public static int enter, exit, inside;

        public void foo() {
            inside++;
        }
    }

    @SuppressWarnings("unused")
    public static class EmptyDelegationAdvice {

        public void foo() {
            /* empty */
        }

        @Advice.OnMethodEnter
        private static void enter() {
            /* empty */
        }

        @Advice.OnMethodExit
        private static void exit() {
            /* empty */
        }
    }

    @SuppressWarnings("unused")
    public static class TrivialAdvice {

        @Advice.OnMethodEnter
        private static void enter() {
            Sample.enter++;
        }

        @Advice.OnMethodExit(onThrowable = Exception.class)
        private static void exit() {
            Sample.exit++;
        }
    }

    @SuppressWarnings("unused")
    public static class TrivialAdviceWithSuppression {

        @Advice.OnMethodEnter(suppress = Exception.class)
        private static void enter() {
            Sample.enter++;
        }

        @Advice.OnMethodExit(suppress = Exception.class)
        private static void exit() {
            Sample.exit++;
        }
    }

    @SuppressWarnings("unused")
    public static class TrivialAdviceDelegation {

        public static int enter, exit;

        public String foo() {
            return FOO;
        }

        @Advice.OnMethodEnter(inline = false)
        private static void enter() {
            enter++;
        }

        @Advice.OnMethodExit(inline = false, onThrowable = Exception.class)
        private static void exit() {
            exit++;
        }
    }

    @SuppressWarnings("unused")
    public static class EmptyMethod {

        public void foo() {
            /* empty */
        }
    }

    @SuppressWarnings("unused")
    public static class EmptyAdvice {

        @Advice.OnMethodEnter
        @Advice.OnMethodExit
        private static void advice() {
            /* empty */
        }
    }

    @SuppressWarnings("unused")
    public static class EmptyAdviceDelegation {

        public void foo() {
            /* do nothing */
        }

        @Advice.OnMethodEnter(inline = false)
        @Advice.OnMethodExit(inline = false)
        private static void advice() {
            /* empty */
        }
    }

    @SuppressWarnings("unused")
    public static class EmptyAdviceWithEntrySuppression {

        @Advice.OnMethodEnter(suppress = Throwable.class)
        @Advice.OnMethodExit
        private static void advice() {
            /* empty */
        }
    }

    @SuppressWarnings("unused")
    public static class EmptyAdviceWithExitSuppression {

        @Advice.OnMethodEnter
        @Advice.OnMethodExit(suppress = Throwable.class)
        private static void advice() {
            /* empty */
        }
    }

    @SuppressWarnings("unused")
    public static class EmptyAdviceWithSuppression {

        @Advice.OnMethodEnter
        @Advice.OnMethodExit(suppress = Throwable.class)
        private static void advice() {
            /* empty */
        }
    }

    @SuppressWarnings("unused")
    public static class EmptyAdviceWithExceptionHandling {

        @Advice.OnMethodEnter
        @Advice.OnMethodExit(onThrowable = Exception.class)
        private static void advice() {
            /* empty */
        }
    }

    @SuppressWarnings("unused")
    public static class EmptyAdviceWithExceptionHandlingAndEntrySuppression {

        @Advice.OnMethodEnter(suppress = Throwable.class)
        @Advice.OnMethodExit(onThrowable = Exception.class)
        private static void advice() {
            /* empty */
        }
    }

    @SuppressWarnings("unused")
    public static class EmptyAdviceWithExceptionHandlingAndExitSuppression {

        @Advice.OnMethodEnter
        @Advice.OnMethodExit(suppress = Throwable.class, onThrowable = Exception.class)
        private static void advice() {
            /* empty */
        }
    }

    @SuppressWarnings("unused")
    public static class EmptyAdviceWithExceptionHandlingAndSuppression {

        @Advice.OnMethodEnter(suppress = Throwable.class)
        @Advice.OnMethodExit(suppress = Throwable.class, onThrowable = Exception.class)
        private static void advice() {
            /* empty */
        }
    }

    @SuppressWarnings("unused")
    public static class EmptyAdviceEntry {

        @Advice.OnMethodEnter
        private static void advice() {
            /* empty */
        }
    }

    @SuppressWarnings("unused")
    public static class EmptyAdviceEntryWithSuppression {

        @Advice.OnMethodEnter(suppress = Throwable.class)
        private static void advice() {
            /* empty */
        }
    }

    @SuppressWarnings("unused")
    public static class EmptyAdviceExit {

        @Advice.OnMethodExit
        private static void advice() {
            /* empty */
        }
    }

    @SuppressWarnings("unused")
    public static class EmptyAdviceExitAndSuppression {

        @Advice.OnMethodExit(suppress = Throwable.class)
        private static void advice() {
            /* empty */
        }
    }

    @SuppressWarnings("unused")
    public static class EmptyAdviceExitWithExceptionHandling {

        @Advice.OnMethodExit(onThrowable = Exception.class)
        private static void advice() {
            /* empty */
        }
    }

    @SuppressWarnings("unused")
    public static class EmptyAdviceExitWithExceptionHandlingAndSuppression {

        @Advice.OnMethodExit(suppress = Throwable.class, onThrowable = Exception.class)
        private static void advice() {
            /* empty */
        }
    }

    @SuppressWarnings("unused")
    public static class AdviceWithVariableValues {

        @Advice.OnMethodEnter
        private static int enter() {
            int foo = VALUE, bar = VALUE * 2;
            Sample.enter++;
            return foo + bar;
        }

        @Advice.OnMethodExit
        private static void exit(@Advice.Enter int enter, @Advice.Return String value) {
            int foo = VALUE, bar = VALUE * 2;
            if (foo + bar != enter || !value.equals(FOO + BAR + QUX + BAZ)) {
                throw new AssertionError();
            }
            Sample.exit++;
        }
    }

    @SuppressWarnings("unused")
    public static class TrivialAdviceSkipException {

        @Advice.OnMethodEnter
        private static void enter() {
            Sample.enter++;
        }

        @Advice.OnMethodExit
        private static void exit() {
            Sample.exit++;
        }
    }

    @SuppressWarnings("unused")
    public static class TrivialAdviceSkipExceptionWithSuppression {

        @Advice.OnMethodEnter(suppress = Exception.class)
        private static void enter() {
            Sample.enter++;
        }

        @Advice.OnMethodExit(suppress = Exception.class)
        private static void exit() {
            Sample.exit++;
        }
    }

    @SuppressWarnings("unused")
    public static class ArgumentAdvice {

        public static int enter, exit;

        @Advice.OnMethodEnter
        private static void enter(String argument) {
            if (!argument.equals(BAR)) {
                throw new AssertionError();
            }
            Sample.enter++;
        }

        @Advice.OnMethodExit
        private static void exit(String argument) {
            if (!argument.equals(BAR)) {
                throw new AssertionError();
            }
            Sample.exit++;
        }
    }

    @SuppressWarnings("unused")
    public static class ArgumentAdviceExplicit {

        @Advice.OnMethodEnter
        private static void enter(@Advice.Argument(1) String argument) {
            if (!argument.equals(BAR)) {
                throw new AssertionError();
            }
            Sample.enter++;
        }

        @Advice.OnMethodExit
        private static void exit(@Advice.Argument(1) String argument) {
            if (!argument.equals(BAR)) {
                throw new AssertionError();
            }
            Sample.exit++;
        }
    }

    @SuppressWarnings("unused")
    public static class ArgumentAdviceDelegationImplicit {

        public String foo(String value) {
            return value;
        }

        @Advice.OnMethodEnter(inline = false)
        private static void enter(String argument) {
            if (!argument.equals(BAR)) {
                throw new AssertionError();
            }
        }

        @Advice.OnMethodExit(inline = false)
        private static void exit(String argument) {
            if (!argument.equals(BAR)) {
                throw new AssertionError();
            }
        }
    }

    @SuppressWarnings("unused")
    public static class IncrementSample {

        public int foo(int value) {
            return ++value;
        }
    }

    @SuppressWarnings("unused")
    public static class IncrementAdvice {

        @Advice.OnMethodEnter
        private static int enter(@Advice.Argument(value = 0, readOnly = false) int argument, @Advice.Ignored int ignored) {
            if (++argument != 1) {
                throw new AssertionError();
            }
            if (++ignored != 0) {
                throw new AssertionError();
            }
            int value = 0;
            if (++value != 1) {
                throw new AssertionError();
            }
            return value;
        }

        @Advice.OnMethodExit
        private static int exit(@Advice.Argument(value = 0, readOnly = false) int argument, @Advice.Ignored int ignored) {
            if (++argument != 3) {
                throw new AssertionError();
            }
            if (++ignored != 0) {
                throw new AssertionError();
            }
            int value = 0;
            if (++value != 1) {
                throw new AssertionError();
            }
            return value;
        }
    }

    @SuppressWarnings("unused")
    public static class ThisReferenceAdvice {

        @Advice.OnMethodEnter
        private static void enter(@Advice.This Sample thiz) {
            if (thiz == null) {
                throw new AssertionError();
            }
            Sample.enter++;
        }

        @Advice.OnMethodExit
        private static void exit(@Advice.This Sample thiz) {
            if (thiz == null) {
                throw new AssertionError();
            }
            Sample.exit++;
        }
    }

    @SuppressWarnings("unused")
    public static class OptionalThisReferenceAdvice {

        @Advice.OnMethodEnter
        private static void enter(@Advice.This(optional = true) Sample thiz) {
            Sample.enter++;
        }

        @Advice.OnMethodExit
        private static void exit(@Advice.This(optional = true) Sample thiz) {
            Sample.exit++;
        }
    }

    @SuppressWarnings("unused")
    public static class EntranceValueAdvice {

        @Advice.OnMethodEnter
        private static int enter() {
            Sample.enter++;
            return VALUE;
        }

        @Advice.OnMethodExit
        private static void exit(@Advice.Enter int value) {
            if (value != VALUE) {
                throw new AssertionError();
            }
            Sample.exit++;
        }
    }

    @SuppressWarnings("unused")
    public static class ReturnValueAdvice {

        @Advice.OnMethodExit
        private static void exit(@Advice.Return String value) {
            if (!value.equals(FOO)) {
                throw new AssertionError();
            }
            Sample.exit++;
        }
    }

    @SuppressWarnings("unused")
    public static class ExceptionHandlerAdvice {

        @Advice.OnMethodEnter(suppress = Throwable.class)
        private static void enter() {
            try {
                throw new Exception();
            } catch (Exception ignored) {
                Sample.enter++;
            }
        }

        @Advice.OnMethodExit(suppress = Throwable.class)
        private static void exit() {
            try {
                throw new Exception();
            } catch (Exception ignored) {
                Sample.exit++;
            }
        }
    }

    @SuppressWarnings("unused")
    public static class ObsoleteReturnValueAdvice {

        @Advice.OnMethodEnter
        private static int enter() {
            Sample.enter++;
            return VALUE;
        }
    }

    @SuppressWarnings("unused")
    public static class UnusedReturnValueAdvice {

        @Advice.OnMethodEnter
        private static int enter() {
            Sample.enter++;
            return VALUE;
        }

        @Advice.OnMethodExit
        private static void exit() {
            Sample.exit++;
        }
    }

    @SuppressWarnings("unused")
    public static class ThrowableAdvice {

        @Advice.OnMethodExit(onThrowable = Exception.class)
        private static void exit(@Advice.Thrown Throwable throwable) {
            Sample.throwable = throwable;
        }
    }

    @SuppressWarnings("unused")
    public static class ThrowOnEnter {

        @Advice.OnMethodEnter
        private static void enter() {
            TracableSample.enter++;
            throw new RuntimeException();
        }

        @Advice.OnMethodExit
        private static void exit() {
            TracableSample.exit++;
        }
    }

    @SuppressWarnings("unused")
    public static class ThrowOnExit {

        @Advice.OnMethodEnter
        private static void enter() {
            TracableSample.enter++;
        }

        @Advice.OnMethodExit
        private static void exit() {
            TracableSample.exit++;
            throw new RuntimeException();
        }
    }

    @SuppressWarnings("unused")
    public static class ThrowSuppressed {

        @Advice.OnMethodEnter(suppress = RuntimeException.class)
        private static void enter() {
            TracableSample.enter++;
            throw new RuntimeException();
        }

        @Advice.OnMethodExit(suppress = RuntimeException.class)
        private static void exit() {
            TracableSample.exit++;
            throw new RuntimeException();
        }
    }

    @SuppressWarnings("unused")
    public static class ThrowNotSuppressedOnEnter {

        @Advice.OnMethodEnter(suppress = RuntimeException.class)
        private static void enter() throws Exception {
            TracableSample.enter++;
            throw new Exception();
        }
    }

    @SuppressWarnings("unused")
    public static class ThrowNotSuppressedOnExit {

        @Advice.OnMethodEnter(suppress = RuntimeException.class)
        private static void enter() {
            TracableSample.enter++;
            throw new RuntimeException();
        }

        @Advice.OnMethodExit(suppress = RuntimeException.class)
        private static void exit() throws Exception {
            TracableSample.exit++;
            throw new Exception();
        }
    }

    @SuppressWarnings("unused")
    public static class ThisSubstitutionAdvice {

        @Advice.OnMethodEnter
        @SuppressWarnings("all")
        private static void enter(@Advice.This(readOnly = false) Box box) {
            box = new Box(BAR);
        }
    }

    @SuppressWarnings("unused")
    public static class ThisOptionalSubstitutionAdvice {

        @Advice.OnMethodEnter
        @SuppressWarnings("all")
        private static void enter(@Advice.This(readOnly = false, optional = true) Box box) {
            box = new Box(BAR);
        }
    }

    @SuppressWarnings("unused")
    public static class IllegalThisSubstitutionAdvice {

        @Advice.OnMethodEnter
        @SuppressWarnings("all")
        private static void enter(@Advice.This Box box) {
            box = new Box(BAR);
        }
    }

    @SuppressWarnings("unused")
    public static class ParameterSubstitutionAdvice {

        @Advice.OnMethodEnter
        @SuppressWarnings("all")
        private static void enter(@Advice.Argument(value = 0, readOnly = false) String value) {
            value = BAR;
        }
    }

    @SuppressWarnings("unused")
    public static class IllegalParameterSubstitutionAdvice {

        @Advice.OnMethodEnter
        @SuppressWarnings("all")
        private static void enter(@Advice.Argument(0) String value) {
            value = BAR;
        }
    }

    @SuppressWarnings("unused")
    public static class EnterSubstitutionAdvice {

        @Advice.OnMethodEnter
        private static String enter() {
            return FOO;
        }

        @Advice.OnMethodExit
        @SuppressWarnings("all")
        private static void exit(@Advice.Enter(readOnly = false) String value) {
            value = BAR;
            if (!value.equals(BAR)) {
                throw new AssertionError();
            }
        }
    }

    @SuppressWarnings("unused")
    public static class IllegalEnterSubstitutionAdvice {

        @Advice.OnMethodEnter
        private static String enter() {
            return FOO;
        }

        @Advice.OnMethodExit
        @SuppressWarnings("all")
        private static void exit(@Advice.Enter String value) {
            value = BAR;
            throw new AssertionError();
        }
    }

    @SuppressWarnings("unused")
    public static class ReturnSubstitutionAdvice {

        @Advice.OnMethodExit
        @SuppressWarnings("all")
        private static void exit(@Advice.Return(readOnly = false) String value) {
            value = BAR;
            if (!value.equals(BAR)) {
                throw new AssertionError();
            }
        }
    }

    @SuppressWarnings("unused")
    public static class IllegalReturnSubstitutionAdvice {

        @Advice.OnMethodExit
        @SuppressWarnings("all")
        private static void exit(@Advice.Return String value) {
            value = BAR;
            throw new AssertionError();
        }
    }

    @SuppressWarnings("unused")
    public static class Box {

        public final String value;

        public Box(String value) {
            this.value = value;
        }

        public String foo() {
            return value;
        }

        public static String bar(String value) {
            return value;
        }
    }

    @SuppressWarnings("unused")
    public static class FieldSample {

        public static int enter, exit;

        private String foo = FOO;

        public String foo() {
            return foo;
        }

        public static String bar() {
            return BAR;
        }
    }

    @SuppressWarnings("unused")
    public static class FieldAdviceImplicit {

        @Advice.OnMethodEnter
        private static void enter(@Advice.FieldValue("foo") String foo) {
            FieldSample.enter++;
            if (!foo.equals(FOO)) {
                throw new AssertionError();
            }
        }

        @Advice.OnMethodExit
        private static void exit(@Advice.FieldValue("foo") String foo) {
            FieldSample.exit++;
            if (!foo.equals(FOO)) {
                throw new AssertionError();
            }
        }
    }

    @SuppressWarnings("unused")
    public static class FieldAdviceExplicit {

        @Advice.OnMethodEnter
        private static void enter(@Advice.FieldValue(value = "foo", declaringType = FieldSample.class) String foo) {
            FieldSample.enter++;
            if (!foo.equals(FOO)) {
                throw new AssertionError();
            }
        }

        @Advice.OnMethodExit
        private static void exit(@Advice.FieldValue(value = "foo", declaringType = FieldSample.class) String foo) {
            FieldSample.exit++;
            if (!foo.equals(FOO)) {
                throw new AssertionError();
            }
        }
    }

    @SuppressWarnings("unused")
    public static class OriginAdvice {

        @Advice.OnMethodEnter
        private static void enter(@Advice.Origin String origin) throws Exception {
            if (!origin.equals(Sample.class.getDeclaredMethod(FOO).toString())) {
                throw new AssertionError();
            }
            Sample.enter++;
        }

        @Advice.OnMethodExit
        private static void exit(@Advice.Origin String origin) throws Exception {
            if (!origin.equals(Sample.class.getDeclaredMethod(FOO).toString())) {
                throw new AssertionError();
            }
            Sample.exit++;
        }
    }

    @SuppressWarnings("unused")
    public static class OriginCustomAdvice {

        @Advice.OnMethodEnter
        private static void enter(@Advice.Origin("#t #m #d #r #s") String origin, @Advice.Origin Class<?> type) throws Exception {
            if (!origin.equals(Sample.class.getName() + " "
                    + FOO
                    + " ()L" + String.class.getName().replace('.', '/') + "; "
                    + String.class.getName()
                    + " ()")) {
                throw new AssertionError();
            }
            if (type != Sample.class) {
                throw new AssertionError();
            }
            Sample.enter++;
        }

        @Advice.OnMethodExit
        private static void exit(@Advice.Origin("\\#\\#\\\\#m") String origin, @Advice.Origin Class<?> type) throws Exception {
            if (!origin.equals("##\\" + FOO)) {
                throw new AssertionError();
            }
            if (type != Sample.class) {
                throw new AssertionError();
            }
            Sample.exit++;
        }
    }

    @SuppressWarnings("all")
    public static class FrameShiftAdvice {

        @Advice.OnMethodEnter
        private static String enter() {
            int v0 = 0;
            if (v0 != 0) {
                // do nothing
            }
            return BAR;
        }

        @Advice.OnMethodExit
        private static void exit(@Advice.Enter String value) {
            if (!value.equals(BAR)) {
                throw new AssertionError();
            }
        }
    }

    @SuppressWarnings("unused")
    public static class ExceptionTypeAdvice {

        public static int enter, exit;

        public void foo() {
            throw new IllegalStateException();
        }

        @Advice.OnMethodEnter(suppress = UnsupportedOperationException.class)
        private static void enter() {
            enter++;
            throw new UnsupportedOperationException();
        }

        @Advice.OnMethodExit(suppress = ArrayIndexOutOfBoundsException.class, onThrowable = IllegalStateException.class)
        private static void exit() {
            exit++;
            throw new ArrayIndexOutOfBoundsException();
        }
    }

    @SuppressWarnings("unused")
    public static class ExceptionNotCatchedAdvice {

        public static int enter, exit;

        public void foo() throws Exception {
            throw new Exception();
        }

        @Advice.OnMethodEnter
        private static void enter() {
            enter++;
        }

        @Advice.OnMethodExit(onThrowable = RuntimeException.class)
        private static void exit() {
            exit++;
        }
    }

    @SuppressWarnings("unused")
    public static class ExceptionCatchedAdvice {

        public static int enter, exit;

        public void foo() {
            throw new RuntimeException();
        }

        @Advice.OnMethodEnter
        private static void enter() {
            enter++;
        }

        @Advice.OnMethodExit(onThrowable = Exception.class)
        private static void exit() {
            exit++;
        }
    }

    @SuppressWarnings("all")
    public static class ExceptionCatchedWithExchangeAdvice {

        public static int enter, exit;

        public void foo() {
            throw new RuntimeException();
        }

        @Advice.OnMethodEnter
        private static void enter() {
            enter++;
        }

        @Advice.OnMethodExit(onThrowable = RuntimeException.class)
        private static void exit(@Advice.Thrown(readOnly = false) Throwable throwable) {
            exit++;
            throwable = new IOException();
        }
    }

    @SuppressWarnings("all")
    public static class NonAssignableReturnTypeAdvice {

        public String foo() {
            return FOO;
        }

        @Advice.OnMethodExit
        private static void exit(@Advice.BoxedReturn(readOnly = false) Object value) {
            value = new Object();
        }
    }

    @SuppressWarnings("all")
    public static class TrivialReturnTypeAdvice {

        public Object foo() {
            return FOO;
        }

        @Advice.OnMethodExit
        private static void exit(@Advice.BoxedReturn(readOnly = false) Object value) {
            value = BAR;
        }
    }

    @SuppressWarnings("all")
    public static class NonAssignablePrimitiveReturnTypeAdvice {

        public int foo() {
            return 0;
        }

        @Advice.OnMethodExit
        private static void exit(@Advice.BoxedReturn(readOnly = false) Object value) {
            value = new Object();
        }
    }

    @SuppressWarnings("unused")
    public static class FieldAdviceIllegalExplicit {

        @Advice.OnMethodEnter
        private static void enter(@Advice.FieldValue(value = "bar", declaringType = Void.class) String bar) {
            throw new AssertionError();
        }
    }

    @SuppressWarnings("unused")
    public static class FieldAdviceNonExistent {

        @Advice.OnMethodEnter
        private static void enter(@Advice.FieldValue("bar") String bar) {
            throw new AssertionError();
        }
    }

    @SuppressWarnings("unused")
    public static class FieldAdviceNonAssignable {

        @Advice.OnMethodEnter
        private static void enter(@Advice.FieldValue("foo") Void foo) {
            throw new AssertionError();
        }
    }

    @SuppressWarnings("all")
    public static class FieldAdviceWrite {

        @Advice.OnMethodEnter
        private static void enter(@Advice.FieldValue("foo") String foo) {
            foo = BAR;
        }
    }

    @SuppressWarnings("all")
    public static class ExceptionSuppressionAdvice {

        @Advice.OnMethodExit(onThrowable = Exception.class)
        private static void exit(@Advice.Thrown(readOnly = false) Throwable throwable) {
            throwable = null;
        }
    }

    @SuppressWarnings("unused")
    public static class IllegalArgumentAdvice {

        @Advice.OnMethodEnter
        private static void enter(Void argument) {
            throw new AssertionError();
        }
    }

    @SuppressWarnings("unused")
    public static class IllegalArgumentReadOnlyAdvice {

        @SuppressWarnings("unused")
        @Advice.OnMethodEnter
        private static void enter(@Advice.Argument(0) Void argument) {
            throw new AssertionError();
        }
    }

    @SuppressWarnings("unused")
    public static class IllegalArgumentWritableAdvice {

        @Advice.OnMethodEnter
        private static void enter(@Advice.Argument(value = 0, readOnly = false) Object argument) {
            throw new AssertionError();
        }
    }

    @SuppressWarnings("unused")
    public static class DuplicateAdvice {

        @Advice.OnMethodEnter
        private static void enter1() {
            throw new AssertionError();
        }

        @Advice.OnMethodEnter
        private static void enter2() {
            throw new AssertionError();
        }
    }

    @SuppressWarnings("unused")
    public static class NonStaticAdvice {

        @Advice.OnMethodEnter
        private void enter() {
            throw new AssertionError();
        }
    }

    @SuppressWarnings("unused")
    public static class IllegalThisReferenceAdvice {

        @Advice.OnMethodExit
        private static void enter(@Advice.This Void thiz) {
            throw new AssertionError();
        }
    }

    @SuppressWarnings("unused")
    public static class IllegalThisReferenceWritableAdvice {

        @Advice.OnMethodExit
        private static void enter(@Advice.This(readOnly = false) Object thiz) {
            throw new AssertionError();
        }
    }

    @SuppressWarnings("unused")
    public static class AmbiguousAdvice {

        @Advice.OnMethodEnter
        private static void enter(@Advice.Argument(0) @Advice.This Object thiz) {
            throw new AssertionError();
        }
    }

    @SuppressWarnings("unused")
    public static class AmbiguousAdviceDelegation {

        @Advice.OnMethodEnter(inline = false)
        private static void enter(@Advice.Argument(0) @Advice.This Object thiz) {
            throw new AssertionError();
        }
    }

    @SuppressWarnings("unused")
    public static class EnterToEnterAdvice {

        @Advice.OnMethodEnter
        private static void enter(@Advice.Enter Object value) {
            throw new AssertionError();
        }
    }

    @SuppressWarnings("unused")
    public static class EnterToReturnAdvice {

        @Advice.OnMethodEnter
        private static void enter(@Advice.Return Object value) {
            throw new AssertionError();
        }
    }

    @SuppressWarnings("unused")
    public static class NonAssignableEnterAdvice {

        @Advice.OnMethodExit
        private static void exit(@Advice.Enter Object value) {
            throw new AssertionError();
        }
    }

    @SuppressWarnings("unused")
    public static class NonEqualEnterAdvice {

        @Advice.OnMethodExit
        private static void exit(@Advice.Enter(readOnly = false) Object value) {
            throw new AssertionError();
        }
    }

    @SuppressWarnings("unused")
    public static class NonAssignableReturnAdvice {

        @Advice.OnMethodExit
        private static void exit(@Advice.Return Void value) {
            throw new AssertionError();
        }
    }

    @SuppressWarnings("unused")
    public static class NonEqualReturnWritableAdvice {

        @Advice.OnMethodExit
        private static void exit(@Advice.Return(readOnly = false) Object value) {
            throw new AssertionError();
        }
    }

    @SuppressWarnings("unused")
    public static class IllegalThrowableRequestAdvice {

        @Advice.OnMethodExit
        private static void exit(@Advice.Thrown Throwable value) {
            throw new AssertionError();
        }
    }

    @SuppressWarnings("unused")
    public static class IllegalThrowableTypeAdvice {

        @Advice.OnMethodExit
        private static void exit(@Advice.Thrown Object value) {
            throw new AssertionError();
        }
    }

    @SuppressWarnings("unused")
    public static class IllegalOriginType {

        @Advice.OnMethodExit
        private static void exit(@Advice.Origin Void value) {
            throw new AssertionError();
        }
    }

    @SuppressWarnings("unused")
    public static class IllegalOriginPattern {

        @Advice.OnMethodExit
        private static void exit(@Advice.Origin("#x") String value) {
            throw new AssertionError();
        }
    }

    @SuppressWarnings("unused")
    public static class IllegalOriginPatternEnd {

        @Advice.OnMethodExit
        private static void exit(@Advice.Origin("#") String value) {
            throw new AssertionError();
        }
    }

    @SuppressWarnings("all")
    public static class IllegalOriginWriteAdvice {

        @Advice.OnMethodExit
        private static void exit(@Advice.Origin String value) {
            value = null;
        }
    }

    @SuppressWarnings("unused")
    public static class IllegalArgumentWritableInlineAdvice {

        @Advice.OnMethodExit(inline = false)
        private static void exit(@Advice.Argument(value = 0, readOnly = false) Object argument) {
            throw new AssertionError();
        }
    }

    @SuppressWarnings("unused")
    public static class IllegalThisWritableInlineAdvice {

        @Advice.OnMethodExit(inline = false)
        private static void exit(@Advice.This(readOnly = false) Object argument) {
            throw new AssertionError();
        }
    }

    @SuppressWarnings("unused")
    public static class IllegalThrowWritableInlineAdvice {

        @Advice.OnMethodExit(inline = false)
        private static void exit(@Advice.Thrown(readOnly = false) Object argument) {
            throw new AssertionError();
        }
    }

    @SuppressWarnings("unused")
    public static class IllegalReturnWritableInlineAdvice {

        @Advice.OnMethodExit(inline = false)
        private static void exit(@Advice.Return(readOnly = false) Object argument) {
            throw new AssertionError();
        }
    }

    @SuppressWarnings("unused")
    public static class IllegalFieldWritableInlineAdvice {

        @Advice.OnMethodExit(inline = false)
        private static void exit(@Advice.Argument(value = 0, readOnly = false) Object argument) {
            throw new AssertionError();
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    public @interface Custom {
        /* empty */
    }

    @SuppressWarnings("unused")
    public static class CustomAdvice {

        @Advice.OnMethodEnter
        @Advice.OnMethodExit
        private static void advice(@Custom Object value) {
            if (value != Object.class) {
                throw new AssertionError();
            }
        }
    }

    @SuppressWarnings("unused")
    public static class CustomPrimitiveAdvice {

        @Advice.OnMethodEnter
        @Advice.OnMethodExit
        private static void advice(@Custom int value) {
            if (value == 0) {
                throw new AssertionError();
            }
        }
    }

    @SuppressWarnings("unused")
    public static class CustomSerializableAdvice {

        @Advice.OnMethodEnter
        @Advice.OnMethodExit
        private static void advice(@Custom Map<String, String> value) {
            if (value.size() != 1 && !value.get(FOO).equals(BAR)) {
                throw new AssertionError();
            }
        }
    }

    @SuppressWarnings("unused")
    public static class NonVisibleAdvice {

        @Advice.OnMethodEnter(inline = false)
        private static void enter() {
            /* empty */
        }
    }

    @SuppressWarnings("unused")
    public static class SkipIfTrueAdvice {

        public static int enter, exit, foo;

        public String foo() {
            foo++;
            return FOO;
        }

        @Advice.OnMethodEnter(skipIfTrue = true)
        private static boolean enter() {
            enter++;
            return true;
        }

        @Advice.OnMethodExit
        private static void exit(@Advice.Enter boolean enter) {
            if (!enter) {
                throw new AssertionError();
            }
            exit++;
        }
    }

    @SuppressWarnings("unused")
    public static class SkipIfTrueNonInlinedAdvice {

        public static int enter, exit, foo;

        public String foo() {
            foo++;
            return FOO;
        }

        @Advice.OnMethodEnter(skipIfTrue = true, inline = false)
        private static boolean enter() {
            enter++;
            return true;
        }

        @Advice.OnMethodExit(inline = false)
        private static void exit(@Advice.Enter boolean enter) {
            if (!enter) {
                throw new AssertionError();
            }
            exit++;
        }
    }

    @SuppressWarnings("unused")
    public static class SkipIfTrueWithSuppressionAdvice {

        public static int enter, exit, foo;

        public String foo() {
            foo++;
            return FOO;
        }

        @Advice.OnMethodEnter(skipIfTrue = true, suppress = RuntimeException.class)
        private static boolean enter() {
            enter++;
            throw new RuntimeException();
        }

        @Advice.OnMethodExit
        private static void exit(@Advice.Enter boolean enter) {
            if (enter) {
                throw new AssertionError();
            }
            exit++;
        }
    }

    @SuppressWarnings("unused")
    public static class SkipIfTrueNonInlinedWithSuppressionAdvice {

        public static int enter, exit, foo;

        public String foo() {
            foo++;
            return FOO;
        }

        @Advice.OnMethodEnter(skipIfTrue = true, suppress = RuntimeException.class, inline = false)
        private static boolean enter() {
            enter++;
            throw new RuntimeException();
        }

        @Advice.OnMethodExit(inline = false)
        private static void exit(@Advice.Enter boolean enter) {
            if (enter) {
                throw new AssertionError();
            }
            exit++;
        }
    }

    public static class SkipIfTrueNonCompatibleReturnType {

        public String foo() {
            return FOO;
        }

        @Advice.OnMethodEnter(skipIfTrue = true)
        private static void enter() {
            throw new AssertionError();
        }
    }
}
