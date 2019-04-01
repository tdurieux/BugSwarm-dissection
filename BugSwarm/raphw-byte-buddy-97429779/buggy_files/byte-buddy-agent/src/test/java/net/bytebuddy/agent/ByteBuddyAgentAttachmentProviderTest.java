package net.bytebuddy.agent;

import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Test;

import java.util.Arrays;
import java.util.Iterator;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.isA;
import static org.hamcrest.MatcherAssert.assertThat;

public class ByteBuddyAgentAttachmentProviderTest {

    private static final String FOO = "foo";

    @Test
    @SuppressWarnings("unchecked")
    public void testSimpleAccessor() throws Exception {
        ByteBuddyAgent.AttachmentProvider.Accessor accessor = new ByteBuddyAgent.AttachmentProvider.Accessor.Simple(Void.class, FOO);
        assertThat(accessor.isAvailable(), is(true));
        assertThat(accessor.getVirtualMachineType(), isA((Class) Void.class));
        assertThat(accessor.getProcessId(), is(FOO));
    }

    @Test
    public void testUnavailableAccessor() throws Exception {
        assertThat(ByteBuddyAgent.AttachmentProvider.Accessor.Unavailable.INSTANCE.isAvailable(), is(false));
    }

    @Test(expected = IllegalStateException.class)
    public void testUnavailableAccessorThrowsExceptionForType() throws Exception {
        ByteBuddyAgent.AttachmentProvider.Accessor.Unavailable.INSTANCE.getVirtualMachineType();
    }

    @Test(expected = IllegalStateException.class)
    public void testUnavailableAccessorThrowsExceptionForProcessId() throws Exception {
        ByteBuddyAgent.AttachmentProvider.Accessor.Unavailable.INSTANCE.getProcessId();
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(ByteBuddyAgent.AttachmentProvider.ForJigsawVm.class).apply();
        ObjectPropertyAssertion.of(ByteBuddyAgent.AttachmentProvider.ForToolsJarVm.class).apply();
        ObjectPropertyAssertion.of(ByteBuddyAgent.AttachmentProvider.ForToolsJarVm.ClassLoaderCreationAction.class).apply();
        ObjectPropertyAssertion.of(ByteBuddyAgent.AttachmentProvider.Compound.class).apply();
        final Iterator<Class<?>> types = Arrays.<Class<?>>asList(Void.class, Object.class).iterator();
        ObjectPropertyAssertion.of(ByteBuddyAgent.AttachmentProvider.Accessor.Simple.class).create(new ObjectPropertyAssertion.Creator<Class<?>>() {
            @Override
            public Class<?> create() {
                return types.next();
            }
        }).apply();
        ObjectPropertyAssertion.of(ByteBuddyAgent.AttachmentProvider.Accessor.Unavailable.class).apply();
    }
}
