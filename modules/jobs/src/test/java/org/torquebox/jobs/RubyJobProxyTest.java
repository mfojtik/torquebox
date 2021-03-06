package org.torquebox.jobs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import java.util.concurrent.TimeUnit;

import org.jruby.Ruby;
import org.junit.Before;
import org.junit.Test;
import org.projectodd.polyglot.core.util.TimeInterval;
import org.quartz.impl.JobDetailImpl;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.torquebox.core.component.ComponentClass;
import org.torquebox.core.component.ComponentResolver;
import org.torquebox.core.runtime.RubyRuntimePool;
import org.torquebox.core.runtime.SharedRubyRuntimePool;
import org.torquebox.jobs.component.JobComponent;
import org.torquebox.test.ruby.AbstractRubyTestCase;

public class RubyJobProxyTest extends AbstractRubyTestCase {

    @Before
    public void setUp() throws Exception {
        this.ruby = createRuby();
        this.componentClass = new ComponentClass();
        this.componentResolver = new ComponentResolver( false );
        this.componentResolver.setComponentInstantiator( this.componentClass );
        this.componentResolver.setComponentWrapperClass( JobComponent.class );
        this.jobDetail = new JobDetailImpl();
        this.jobDetail.setName( "RubyJobProxyTest" );
        this.context = mock( JobExecutionContext.class );
    }

    @Test
    public void testExecution() throws Exception {
        this.componentClass.setClassName( "TestJob" );
        this.componentClass.setRequirePath( "org/torquebox/jobs/test_job" );
        RubyJobProxy jobProxy = createJobProxy();

        jobProxy.execute( this.context );
        Boolean ran = (Boolean) jobProxy.getComponent()._callRubyMethod( "ran?" );
        assertTrue( ran.booleanValue() );
    }

    @Test
    public void testInitialization() throws Exception {
        this.componentClass.setClassName( "TestJob" );
        this.componentClass.setRequirePath( "org/torquebox/jobs/test_job" );
        this.componentResolver.setInitializeParams( ruby.evalScriptlet( "{'foo'=>42}" ).convertToHash() );
        RubyJobProxy jobProxy = createJobProxy();

        jobProxy.execute( this.context );
        Long foo = (Long) jobProxy.getComponent()._callRubyMethod( "[]", new Object[] { "foo" } );
        assertEquals( new Long( 42 ), foo );
    }

    @Test
    public void testOnError() throws Exception {
        this.componentClass.setClassName( "ErrorJob" );
        this.componentClass.setRequirePath( "org/torquebox/jobs/error_job" );
        RubyJobProxy jobProxy = createJobProxy();

        jobProxy.execute( this.context );
        String error = (String) jobProxy.getComponent()._callRubyMethod( "error" );
        assertTrue( error.contains( "an error" ) );
    }

    @Test
    public void testExceptionButNoOnError() throws Exception {
        this.componentClass.setClassName( "TestJob" );
        this.componentClass.setRequirePath( "org/torquebox/jobs/test_job" );
        this.componentResolver.setInitializeParams( ruby.evalScriptlet( "{'raise_error'=>true}" ).convertToHash() );
        RubyJobProxy jobProxy = createJobProxy();
        boolean caughtException = false;

        try {
            jobProxy.execute( this.context );
        } catch (JobExecutionException ex) {
            caughtException = true;
            assertEquals( IllegalStateException.class, ex.getCause().getClass() );
        }
        assertTrue( caughtException );
    }

    private RubyJobProxy createJobProxy() {
        RubyRuntimePool runtimePool = new SharedRubyRuntimePool( this.ruby );
        return new RubyJobProxy( runtimePool, this.componentResolver, this.jobDetail );
    }

    private Ruby ruby;
    private ComponentClass componentClass;
    private ComponentResolver componentResolver;
    private JobExecutionContext context;
    private JobDetailImpl jobDetail;

}
