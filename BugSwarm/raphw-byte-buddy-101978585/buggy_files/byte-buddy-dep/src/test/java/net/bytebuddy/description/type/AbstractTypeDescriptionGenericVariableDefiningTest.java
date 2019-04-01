package net.bytebuddy.description.type;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.test.utility.JavaVersionRule;
import org.junit.Test;

import java.lang.annotation.Annotation;

import static net.bytebuddy.matcher.ElementMatchers.named;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public abstract class AbstractTypeDescriptionGenericVariableDefiningTest extends AbstractTypeDescriptionGenericTest {

    private static final String FOO = "foo";

    private static final String T = "T", S = "S", U = "U", V = "V", W = "W", X = "X";

    private static final String TYPE_ANNOTATION = "net.bytebuddy.test.precompiled.TypeAnnotation";

    private static final String TYPE_ANNOTATION_SAMPLES = "net.bytebuddy.test.precompiled.TypeAnnotationSamples";

    protected abstract TypeDescription describe(Class<?> type);

    @Test
    @JavaVersionRule.Enforce(8)
    @SuppressWarnings("unchecked")
    public void testTypeVariableT() throws Exception {
        Class<? extends Annotation> typeAnnotation = (Class<? extends Annotation>) Class.forName(TYPE_ANNOTATION);
        MethodDescription.InDefinedShape value = new TypeDescription.ForLoadedType(typeAnnotation).getDeclaredMethods().getOnly();
        TypeDescription typeDescription = describe(Class.forName(TYPE_ANNOTATION_SAMPLES));
        TypeDescription.Generic t = typeDescription.getTypeVariables().filter(named(T)).getOnly();
        assertThat(t.getSort(), is(TypeDefinition.Sort.VARIABLE));
        assertThat(t.getDeclaredAnnotations().size(), is(1));
        assertThat(t.getDeclaredAnnotations().isAnnotationPresent(typeAnnotation), is(true));
        assertThat(t.getDeclaredAnnotations().ofType(typeAnnotation).getValue(value, Integer.class), is(0));
        assertThat(t.getUpperBounds().size(), is(1));
        assertThat(t.getUpperBounds().contains(TypeDescription.Generic.OBJECT), is(true));
    }

    @Test
    @JavaVersionRule.Enforce(8)
    @SuppressWarnings("unchecked")
    public void testTypeVariableS() throws Exception {
        TypeDescription typeDescription = describe(Class.forName(TYPE_ANNOTATION_SAMPLES));
        TypeDescription.Generic t = typeDescription.getTypeVariables().filter(named(S)).getOnly();
        assertThat(t.getSort(), is(TypeDefinition.Sort.VARIABLE));
        assertThat(t.getDeclaredAnnotations().size(), is(0));
    }

    @Test
    @JavaVersionRule.Enforce(8)
    @SuppressWarnings("unchecked")
    public void testTypeVariableU() throws Exception {
        Class<? extends Annotation> typeAnnotation = (Class<? extends Annotation>) Class.forName(TYPE_ANNOTATION);
        MethodDescription.InDefinedShape value = new TypeDescription.ForLoadedType(typeAnnotation).getDeclaredMethods().getOnly();
        TypeDescription typeDescription = describe(Class.forName(TYPE_ANNOTATION_SAMPLES));
        TypeDescription.Generic u = typeDescription.getTypeVariables().filter(named(U)).getOnly();
        assertThat(u.getSort(), is(TypeDefinition.Sort.VARIABLE));
        assertThat(u.getDeclaredAnnotations().size(), is(1));
        assertThat(u.getDeclaredAnnotations().isAnnotationPresent(typeAnnotation), is(true));
        assertThat(u.getDeclaredAnnotations().ofType(typeAnnotation).getValue(value, Integer.class), is(2));
        assertThat(u.getUpperBounds().get(0).getSort(), is(TypeDefinition.Sort.NON_GENERIC));
        assertThat(u.getUpperBounds().get(0).getDeclaredAnnotations().size(), is(0));
        assertThat(u.getUpperBounds().get(1).getSort(), is(TypeDefinition.Sort.PARAMETERIZED));
        assertThat(u.getUpperBounds().get(1).getDeclaredAnnotations().size(), is(1));
        assertThat(u.getUpperBounds().get(1).getDeclaredAnnotations().isAnnotationPresent(typeAnnotation), is(true));
        assertThat(u.getUpperBounds().get(1).getDeclaredAnnotations().ofType(typeAnnotation).getValue(value, Integer.class), is(3));
        assertThat(u.getUpperBounds().get(1).getParameters().get(0).getSort(), is(TypeDefinition.Sort.WILDCARD));
        assertThat(u.getUpperBounds().get(1).getParameters().get(0).getDeclaredAnnotations().size(), is(1));
        assertThat(u.getUpperBounds().get(1).getParameters().get(0).getDeclaredAnnotations().isAnnotationPresent(typeAnnotation), is(true));
        assertThat(u.getUpperBounds().get(1).getParameters().get(0).getDeclaredAnnotations().ofType(typeAnnotation).getValue(value, Integer.class), is(4));
        assertThat(u.getUpperBounds().get(2).getSort(), is(TypeDefinition.Sort.PARAMETERIZED));
        assertThat(u.getUpperBounds().get(2).getDeclaredAnnotations().size(), is(1));
        assertThat(u.getUpperBounds().get(2).getDeclaredAnnotations().isAnnotationPresent(typeAnnotation), is(true));
        assertThat(u.getUpperBounds().get(2).getDeclaredAnnotations().ofType(typeAnnotation).getValue(value, Integer.class), is(5));
        assertThat(u.getUpperBounds().get(2).getParameters().get(0).getSort(), is(TypeDefinition.Sort.WILDCARD));
        assertThat(u.getUpperBounds().get(2).getParameters().get(0).getDeclaredAnnotations().size(), is(1));
        assertThat(u.getUpperBounds().get(2).getParameters().get(0).getDeclaredAnnotations().isAnnotationPresent(typeAnnotation), is(true));
        assertThat(u.getUpperBounds().get(2).getParameters().get(0).getDeclaredAnnotations().ofType(typeAnnotation).getValue(value, Integer.class), is(6));
    }

    @Test
    @JavaVersionRule.Enforce(8)
    @SuppressWarnings("unchecked")
    public void testTypeVariableV() throws Exception {
        Class<? extends Annotation> typeAnnotation = (Class<? extends Annotation>) Class.forName(TYPE_ANNOTATION);
        MethodDescription.InDefinedShape value = new TypeDescription.ForLoadedType(typeAnnotation).getDeclaredMethods().getOnly();
        TypeDescription typeDescription = describe(Class.forName(TYPE_ANNOTATION_SAMPLES));
        TypeDescription.Generic v = typeDescription.getTypeVariables().filter(named(V)).getOnly();
        assertThat(v.getSort(), is(TypeDefinition.Sort.VARIABLE));
        assertThat(v.getDeclaredAnnotations().size(), is(1));
        assertThat(v.getDeclaredAnnotations().isAnnotationPresent(typeAnnotation), is(true));
        assertThat(v.getDeclaredAnnotations().ofType(typeAnnotation).getValue(value, Integer.class), is(7));
        assertThat(v.getUpperBounds().get(0).getSort(), is(TypeDefinition.Sort.PARAMETERIZED));
        assertThat(v.getUpperBounds().get(0).getDeclaredAnnotations().size(), is(0));
        assertThat(v.getUpperBounds().get(0).getParameters().get(0).getSort(), is(TypeDefinition.Sort.WILDCARD));
        assertThat(v.getUpperBounds().get(0).getParameters().get(0).getDeclaredAnnotations().size(), is(1));
        assertThat(v.getUpperBounds().get(0).getParameters().get(0).getDeclaredAnnotations().isAnnotationPresent(typeAnnotation), is(true));
        assertThat(v.getUpperBounds().get(0).getParameters().get(0).getDeclaredAnnotations().ofType(typeAnnotation).getValue(value, Integer.class), is(8));
        assertThat(v.getUpperBounds().get(0).getParameters().get(0).getUpperBounds().getOnly().getSort(), is(TypeDefinition.Sort.NON_GENERIC));
        assertThat(v.getUpperBounds().get(0).getParameters().get(0).getUpperBounds().getOnly().getDeclaredAnnotations().size(), is(1));
        assertThat(v.getUpperBounds().get(0).getParameters().get(0).getUpperBounds().getOnly().getDeclaredAnnotations().isAnnotationPresent(typeAnnotation), is(true));
        assertThat(v.getUpperBounds().get(0).getParameters().get(0).getUpperBounds().getOnly().getDeclaredAnnotations().ofType(typeAnnotation).getValue(value, Integer.class), is(9));
        assertThat(v.getUpperBounds().get(0).getParameters().get(1).getSort(), is(TypeDefinition.Sort.PARAMETERIZED));
        assertThat(v.getUpperBounds().get(0).getParameters().get(1).getDeclaredAnnotations().size(), is(1));
        assertThat(v.getUpperBounds().get(0).getParameters().get(1).getDeclaredAnnotations().isAnnotationPresent(typeAnnotation), is(true));
        assertThat(v.getUpperBounds().get(0).getParameters().get(1).getDeclaredAnnotations().ofType(typeAnnotation).getValue(value, Integer.class), is(10));
        assertThat(v.getUpperBounds().get(0).getParameters().get(1).getParameters().getOnly().getSort(), is(TypeDefinition.Sort.WILDCARD));
        assertThat(v.getUpperBounds().get(0).getParameters().get(1).getParameters().getOnly().getDeclaredAnnotations().size(), is(1));
        assertThat(v.getUpperBounds().get(0).getParameters().get(1).getParameters().getOnly().getDeclaredAnnotations().isAnnotationPresent(typeAnnotation), is(true));
        assertThat(v.getUpperBounds().get(0).getParameters().get(1).getParameters().getOnly().getDeclaredAnnotations().ofType(typeAnnotation)
                .getValue(value, Integer.class), is(11));
        assertThat(v.getUpperBounds().get(0).getParameters().get(1).getParameters().getOnly().getLowerBounds().getOnly().getSort(), is(TypeDefinition.Sort.VARIABLE));
        assertThat(v.getUpperBounds().get(0).getParameters().get(1).getParameters().getOnly().getLowerBounds().getOnly().getDeclaredAnnotations().size(), is(1));
        assertThat(v.getUpperBounds().get(0).getParameters().get(1).getParameters().getOnly().getLowerBounds().getOnly().getDeclaredAnnotations()
                .isAnnotationPresent(typeAnnotation), is(true));
        assertThat(v.getUpperBounds().get(0).getParameters().get(1).getParameters().getOnly().getLowerBounds().getOnly().getDeclaredAnnotations()
                .ofType(typeAnnotation).getValue(value, Integer.class), is(12));
        assertThat(v.getUpperBounds().get(0).getParameters().get(1).getParameters().getOnly().getLowerBounds().getOnly().getUpperBounds().get(0)
                .getSort(), is(TypeDefinition.Sort.NON_GENERIC));
        assertThat(v.getUpperBounds().get(0).getParameters().get(1).getParameters().getOnly().getLowerBounds().getOnly().getUpperBounds().get(0)
                .getDeclaredAnnotations().size(), is(0));
        assertThat(v.getUpperBounds().get(0).getParameters().get(1).getParameters().getOnly().getLowerBounds().getOnly().getUpperBounds().get(1)
                .getSort(), is(TypeDefinition.Sort.PARAMETERIZED));
        assertThat(v.getUpperBounds().get(0).getParameters().get(1).getParameters().getOnly().getLowerBounds().getOnly().getUpperBounds().get(1)
                .getDeclaredAnnotations().size(), is(1));
        assertThat(v.getUpperBounds().get(0).getParameters().get(1).getParameters().getOnly().getLowerBounds().getOnly().getUpperBounds().get(1)
                .getDeclaredAnnotations().isAnnotationPresent(typeAnnotation), is(true));
        assertThat(v.getUpperBounds().get(0).getParameters().get(1).getParameters().getOnly().getLowerBounds().getOnly().getUpperBounds().get(1)
                .getDeclaredAnnotations().getOnly().prepare(typeAnnotation).getValue(value, Integer.class), is(3));
    }

    @Test
    @JavaVersionRule.Enforce(8)
    @SuppressWarnings("unchecked")
    public void testTypeVariableW() throws Exception {
        Class<? extends Annotation> typeAnnotation = (Class<? extends Annotation>) Class.forName(TYPE_ANNOTATION);
        MethodDescription.InDefinedShape value = new TypeDescription.ForLoadedType(typeAnnotation).getDeclaredMethods().getOnly();
        TypeDescription typeDescription = describe(Class.forName(TYPE_ANNOTATION_SAMPLES));
        TypeDescription.Generic t = typeDescription.getTypeVariables().filter(named(W)).getOnly();
        assertThat(t.getSort(), is(TypeDefinition.Sort.VARIABLE));
        assertThat(t.getDeclaredAnnotations().size(), is(1));
        assertThat(t.getDeclaredAnnotations().isAnnotationPresent(typeAnnotation), is(true));
        assertThat(t.getDeclaredAnnotations().ofType(typeAnnotation).getValue(value, Integer.class), is(13));
        assertThat(t.getUpperBounds().size(), is(1));
        assertThat(t.getUpperBounds().getOnly().getSort(), is(TypeDefinition.Sort.VARIABLE));
        assertThat(t.getUpperBounds().getOnly().getDeclaredAnnotations().size(), is(1));
        assertThat(t.getUpperBounds().getOnly().getDeclaredAnnotations().isAnnotationPresent(typeAnnotation), is(true));
        assertThat(t.getUpperBounds().getOnly().getDeclaredAnnotations().ofType(typeAnnotation).getValue(value, Integer.class), is(14));
    }

    @Test
    @JavaVersionRule.Enforce(8)
    @SuppressWarnings("unchecked")
    public void testTypeVariableX() throws Exception {
        Class<? extends Annotation> typeAnnotation = (Class<? extends Annotation>) Class.forName(TYPE_ANNOTATION);
        MethodDescription.InDefinedShape value = new TypeDescription.ForLoadedType(typeAnnotation).getDeclaredMethods().getOnly();
        TypeDescription typeDescription = describe(Class.forName(TYPE_ANNOTATION_SAMPLES));
        TypeDescription.Generic t = typeDescription.getTypeVariables().filter(named(X)).getOnly();
        assertThat(t.getSort(), is(TypeDefinition.Sort.VARIABLE));
        assertThat(t.getDeclaredAnnotations().size(), is(1));
        assertThat(t.getDeclaredAnnotations().isAnnotationPresent(typeAnnotation), is(true));
        assertThat(t.getDeclaredAnnotations().ofType(typeAnnotation).getValue(value, Integer.class), is(15));
        assertThat(t.getUpperBounds().size(), is(1));
        assertThat(t.getUpperBounds().getOnly().getSort(), is(TypeDefinition.Sort.PARAMETERIZED));
        assertThat(t.getUpperBounds().getOnly().getDeclaredAnnotations().size(), is(1));
        assertThat(t.getUpperBounds().getOnly().getDeclaredAnnotations().isAnnotationPresent(typeAnnotation), is(true));
        assertThat(t.getUpperBounds().getOnly().getDeclaredAnnotations().ofType(typeAnnotation).getValue(value, Integer.class), is(16));
        assertThat(t.getUpperBounds().getOnly().getParameters().getOnly().getSort(), is(TypeDefinition.Sort.WILDCARD));
        assertThat(t.getUpperBounds().getOnly().getParameters().getOnly().getDeclaredAnnotations().size(), is(1));
        assertThat(t.getUpperBounds().getOnly().getParameters().getOnly().getDeclaredAnnotations().isAnnotationPresent(typeAnnotation), is(true));
        assertThat(t.getUpperBounds().getOnly().getParameters().getOnly().getDeclaredAnnotations().ofType(typeAnnotation).getValue(value, Integer.class), is(17));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testMethodVariableT() throws Exception {
        Class<? extends Annotation> typeAnnotation = (Class<? extends Annotation>) Class.forName(TYPE_ANNOTATION);
        MethodDescription.InDefinedShape value = new TypeDescription.ForLoadedType(typeAnnotation).getDeclaredMethods().getOnly();
        MethodDescription methodDescription = describe(Class.forName(TYPE_ANNOTATION_SAMPLES)).getDeclaredMethods().filter(named(FOO)).getOnly();
        TypeDescription.Generic t = methodDescription.getTypeVariables().getOnly();
        assertThat(t.getSort(), is(TypeDefinition.Sort.VARIABLE));
        assertThat(t.getDeclaredAnnotations().size(), is(1));
        assertThat(t.getDeclaredAnnotations().isAnnotationPresent(typeAnnotation), is(true));
        assertThat(t.getDeclaredAnnotations().ofType(typeAnnotation).getValue(value, Integer.class), is(26));
        assertThat(t.getUpperBounds().getOnly().getSort(), is(TypeDefinition.Sort.NON_GENERIC));
        assertThat(t.getUpperBounds().getOnly().getDeclaredAnnotations().size(), is(1));
        assertThat(t.getUpperBounds().getOnly().getDeclaredAnnotations().isAnnotationPresent(typeAnnotation), is(true));
        assertThat(t.getUpperBounds().getOnly().getDeclaredAnnotations().ofType(typeAnnotation).getValue(value, Integer.class), is(27));
    }
}
