package xyz.iconc.dev.api;
//https://github.com/restlet/restlet-tutorial/blob/8a334e9b4d7a9dced807a3dd64e5e718c8160afc/modules/org.restlet.tutorial.webapi/src/main/java/org/restlet/tutorial/WebApiTutorial.java#L49
import org.restlet.Application;
import org.restlet.Component;
import org.restlet.Restlet;
import org.restlet.data.ChallengeScheme;
import org.restlet.data.Protocol;
import org.restlet.routing.Router;
import org.restlet.security.ChallengeAuthenticator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.iconc.dev.api.server.serverResources.BulkMessageServerResource;
import xyz.iconc.dev.api.server.serverResources.MessageServerResource;
import xyz.iconc.dev.api.server.serverResources.UserServerResource;
import xyz.iconc.dev.server.DatabaseManager;


public class ServerAPI  extends Application {
    private static Logger LOGGER = LoggerFactory.getLogger(ServerAPI.class);
    private final Router router = new Router();
    private static DatabaseManager DATABASE_MANAGER = null;

    public ServerAPI(DatabaseManager databaseManager) {
        super();
        LOGGER.info("Application starting...");


        LOGGER.info("Attempting connection with database...");
        DATABASE_MANAGER = databaseManager;

        LOGGER.info("Connected to database!");


        // Attach application to http://localhost:9000/v1
        Component c = new Component();
        c.getServers().add(Protocol.HTTP, 9000);


        c.getDefaultHost().attach("/v1", this);


        try {
            c.start();
        } catch (Exception e) {
            LOGGER.error(e.toString());
            System.exit(1);
        }

        LOGGER.info("Sample Web API started");
        LOGGER.info("URL: http://localhost:9000/v1");
    }



    private ChallengeAuthenticator createApiGuard() {

        ChallengeAuthenticator apiGuard = new ChallengeAuthenticator(
                getContext(), ChallengeScheme.HTTP_BASIC, "realm");
        DatabaseVerifier databaseVerifier = new DatabaseVerifier();

        // - Verifier : checks authentication
        // - Enroler : to check authorization (roles)
        apiGuard.setVerifier(databaseVerifier);
        apiGuard.setEnroler(new DatabaseEnroler());

        // Provide your own authentication checks by extending SecretVerifier or
        // LocalVerifier classes
        // Extend the Enroler class in order to assign roles for an
        // authenticated user

        return apiGuard;
    }

    private Router createApiRouter() {

        // Attach server resources to the given URL template.
        // For instance, CompanyListServerResource is attached
        // to http://localhost:9000/v1/companies
        // and to http://localhost:9000/v1/companies/
        Router router = new Router(getContext());

        router.attach("/user/{identifier}", UserServerResource.class);

        router.attach("/message/{identifier}", MessageServerResource.class);
        router.attach("/message", MessageServerResource.class);
        router.attach("/message/", MessageServerResource.class);

        return router;
    }

    public Router publicResources() {
        Router router = new Router();

        //router.attach("/ping", PingServerResource.class);
        router.attach("/messages/{query}", BulkMessageServerResource.class);

        return router;
    }

    @Override
    public Restlet createInboundRoot() {
        Router publicRouter = publicResources();

        // Create the api router, protected by a guard
        ChallengeAuthenticator apiGuard = createApiGuard();
        Router apiRouter = createApiRouter();
        apiGuard.setNext(apiRouter);

        publicRouter.attachDefault(apiGuard);

        return publicRouter;
    }

    public static DatabaseManager getDatabaseManager() {
        return DATABASE_MANAGER;
    }

    public static void main(String[] args) throws Exception {
        DatabaseManager databaseManager = new DatabaseManager(true);
        ServerAPI server = new ServerAPI(databaseManager);
    }

}
