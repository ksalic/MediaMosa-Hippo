import nl.uva.mediamosa.MediaMosaService;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.config.impl.JcrPluginConfig;
import org.hippoecm.repository.HippoRepository;
import org.hippoecm.repository.HippoRepositoryFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.onehippo.forge.externalresource.api.HippoMediaMosaResourceManager;
import org.onehippo.forge.externalresource.api.scheduale.synchronization.FakeMassSync;
import org.onehippo.forge.externalresource.api.scheduale.synchronization.FakeSync;
import org.onehippo.forge.externalresource.api.utils.ExresDaemonModule;
import org.onehippo.forge.externalresource.api.utils.ResourceInvocationType;
import org.quartz.*;
import org.quartz.core.QuartzScheduler;
import org.quartz.core.SchedulingContext;
import org.quartz.impl.StdScheduler;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.util.UUID;

/**
 * @version $Id$
 */
@Ignore(value = "no external stuff for tests")
public class QuartzTest {
    @SuppressWarnings({"UnusedDeclaration"})
    private static Logger log = LoggerFactory.getLogger(QuartzTest.class);

    MediaMosaService mediaMosaService;
    private Session session;

    @Before
    public void setUp() throws Exception {
        if (session == null) {
            try {
                final HippoRepository repository = HippoRepositoryFactory.getHippoRepository("rmi://localhost:1099/hipporepository");
                session = repository.login("admin", "admin".toCharArray());
            } catch (RepositoryException e) {
                log.error("Error connecting to the hippo repository", e);
            }
        }
        scheduler = StdSchedulerFactory.getDefaultScheduler();
        scheduler.start();
    }

    @After
    public void tearDown() throws Exception {
        scheduler.shutdown(false);
        session.logout();
    }

    private static final String SYNCHRONIZATION_GROUP = "synchronization-group";

    private Scheduler scheduler;

    @Test
    public void testSchedualing() throws Exception {
        ExresDaemonModule daemonModule = new ExresDaemonModule();
        daemonModule.initialize(session);
        Node resourcemanagerNode = session.getNode("/hippo:configuration/hippo:frontend/cms/cms-services/externalResourceService/hippomediamosa:resource");
        HippoMediaMosaResourceManager resourceManager = new HippoMediaMosaResourceManager(new JcrPluginConfig(new JcrNodeModel(resourcemanagerNode)), ResourceInvocationType.CMS);

        String jobId = UUID.randomUUID().toString();

        System.out.println(jobId);

        JobDetail job = new JobDetail(jobId, SYNCHRONIZATION_GROUP, FakeSync.class);

        JobDataMap dataMap = new JobDataMap();
        dataMap.put("identifier", jobId);

        job.setJobDataMap(dataMap);

        Trigger trigger = TriggerUtils.makeImmediateTrigger(0, 1);
        trigger.setName("runonce");

        resourceManager.scheduleNowOnce(FakeSync.class, dataMap);
        Thread.sleep(120000);
    }

    @Test
    public void testStuff() throws Exception {
        final String MassSyncJobName = "MSJN";
        final String MassSyncJobGroup = "MSJG";

        String MassSyncJobTriggerName = "MSJTN";
        String MassSyncJobTriggerGROUP = "MSJTG";

        JobDetail massSyncJob = new JobDetail(MassSyncJobName, MassSyncJobGroup, FakeMassSync.class);

        Trigger trigger =
                new CronTrigger(MassSyncJobTriggerName,
                        MassSyncJobTriggerGROUP,
                        MassSyncJobName,
                        MassSyncJobGroup,
                        "0/20 * * 1/1 * ? *"
                );

        scheduler.scheduleJob(massSyncJob, trigger);

        System.out.println("waiting 62 seconds to excecute it twice");

        Thread.sleep(42000);

        //case
        if (scheduler.getTrigger(MassSyncJobTriggerName, MassSyncJobTriggerGROUP) != null) {


        }

        System.out.println("waiting enought going round 2, once every 10 seconds");

        Trigger trigger2 =
                new CronTrigger(MassSyncJobTriggerName,
                        MassSyncJobTriggerGROUP,
                        MassSyncJobName,
                        MassSyncJobGroup,
                        "0/5 * * 1/1 * ? *"
                );

        scheduler.rescheduleJob(MassSyncJobTriggerName, MassSyncJobTriggerGROUP, trigger2);

        System.out.println("waiting 20 seconds to excecute it 4 times");

        Thread.sleep(20000);

        System.out.println("unschedualing");

        scheduler.unscheduleJob(MassSyncJobTriggerName, MassSyncJobTriggerGROUP);

        System.out.println("waiting 20 seconds to excecute it 0 times");

        Thread.sleep(20000);

        System.out.println("Did it work?");
    }

    @Test
    public void testReschedualing() throws Exception {

        Scheduler scheduler = StdSchedulerFactory.getDefaultScheduler();

        CronTrigger cronTrigger = new CronTrigger("crontrigger", SYNCHRONIZATION_GROUP, "0 0/1 * 1/1 * ? *");

        scheduler.rescheduleJob("22e5f1bc-c1c9-4fd6-9036-371ada0a5506", SYNCHRONIZATION_GROUP, cronTrigger);

    }

    private class ExternalResourceSchedualer extends StdScheduler implements Scheduler {

        /**
         * <p>
         * Construct a <code>StdScheduler</code> instance to proxy the given
         * <code>QuartzScheduler</code> instance, and with the given <code>SchedulingContext</code>.
         * </p>
         */
        public ExternalResourceSchedualer(QuartzScheduler sched, SchedulingContext schedCtxt) {
            super(sched, schedCtxt);
        }

        public ExternalResourceSchedualer(QuartzScheduler sched, ExternalResourceSchedualingContext schedCtxt) {
            super(sched, schedCtxt);
        }


    }

    private class ExternalResourceSchedualingContext extends SchedulingContext {

        private Session session;

        public ExternalResourceSchedualingContext() {
            super();
        }

        public ExternalResourceSchedualingContext(Session session) {
            super();
            this.session = session;
        }

        public Session getSession() {
            return session;
        }

        public void setSession(Session session) {
            this.session = session;
        }
    }
}
