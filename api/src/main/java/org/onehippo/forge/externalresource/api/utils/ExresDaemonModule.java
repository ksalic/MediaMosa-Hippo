package org.onehippo.forge.externalresource.api.utils;

import org.hippoecm.repository.ext.DaemonModule;
import org.hippoecm.repository.quartz.JCRJobStore;
import org.hippoecm.repository.quartz.JCRScheduler;
import org.hippoecm.repository.quartz.JCRSchedulingContext;
import org.onehippo.forge.externalresource.api.scheduale.ExternalResourceSchedular;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.core.QuartzScheduler;
import org.quartz.core.QuartzSchedulerResources;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.util.Properties;

/**
 * @version $Id$
 */
public class ExresDaemonModule implements DaemonModule {

    @SuppressWarnings({"UnusedDeclaration"})
    private static Logger log = LoggerFactory.getLogger(ExresDaemonModule.class);

    static Session session = null;
    static ExternalResourceSchedular scheduler = null;
    static ExternalResourceSchedulerFactory schedFactory = null;

    public void initialize(final Session s) throws RepositoryException {
        ExresDaemonModule.session = s;
        log.info("***Initialized ExresDaemonModule for quartz schedualar***");
        Properties properties = new Properties();
        try {
            properties.put("org.quartz.scheduler.instanceName", "External Resource Quartz Job Scheduler");
            properties.put("org.quartz.scheduler.instanceName", "EXRES1");
            properties.put("org.quartz.scheduler.instanceId", "AUTO");
            properties.put("org.quartz.scheduler.skipUpdateCheck", "true");
            properties.put("org.quartz.threadPool.class", "org.quartz.simpl.SimpleThreadPool");
            properties.put("org.quartz.threadPool.threadCount", "1");
            properties.put("org.quartz.threadPool.threadPriority", "5");
            schedFactory = new ExternalResourceSchedulerFactory(session);
            schedFactory.initialize(properties);
            scheduler = (ExternalResourceSchedular) schedFactory.getScheduler();
            scheduler.start();
        } catch (SchedulerException ex) {
            ex.printStackTrace(System.err);
        }
    }


    public static Session getSession() {
        return session;
    }


    public static ExternalResourceSchedular getScheduler() {
        return new ExternalResourceSchedular(scheduler, session);
    }

    public void shutdown() {
        if (scheduler != null) {
            scheduler.shutdown(true);
        }
        session.logout();
    }


    public static class ExternalResourceSchedulerFactory extends StdSchedulerFactory {
        private Properties props;
        private Session session;

        public ExternalResourceSchedulerFactory(Session session) throws SchedulerException {
            this.session = session;
        }

        public ExternalResourceSchedulerFactory(Properties props, Session session) throws SchedulerException {
            super(props);
            this.props = new Properties(props);
            this.session = session;
        }

        public ExternalResourceSchedulerFactory(ExternalResourceSchedulerFactory factory, Session session) throws SchedulerException {
            super(factory.props);
            this.session = session;
        }

        @Override
        public void initialize(Properties props) throws SchedulerException {
            this.props = new Properties(props);
            super.initialize(props);
        }

        @Override
        protected Scheduler instantiate(QuartzSchedulerResources rsrcs, QuartzScheduler qs) {
            JCRSchedulingContext schedCtxt = new JCRSchedulingContext(session);
            schedCtxt.setInstanceId(rsrcs.getInstanceId());
            schedCtxt.setSession(session);
            Scheduler scheduler = new ExternalResourceSchedular(qs, schedCtxt);
            return scheduler;
        }
    }
}
