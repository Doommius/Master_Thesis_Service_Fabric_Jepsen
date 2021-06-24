package statefulservice;

import java.time.Duration;
import java.util.logging.Logger;
import java.util.logging.Level;

import microsoft.servicefabric.services.runtime.ServiceRuntime;

public class JepsentestServiceHost {

    private static final Logger logger = Logger.getLogger(JepsentestServiceHost.class.getName());

    public static void main(String[] args) throws Exception {
        try {
            ServiceRuntime.registerStatefulServiceAsync("JepsentestServiceType", (context) -> new JepsentestService(context), Duration.ofSeconds(10));
            logger.log(Level.INFO, "Registered stateful service of type JepsentestServiceType. ");
            Thread.sleep(Long.MAX_VALUE);
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Exception occured", ex);
            throw ex;
        }
    }
}
