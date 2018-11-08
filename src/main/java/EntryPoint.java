import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/api")
public class EntryPoint {

    @GET
    @Path("/{param}")
    @Produces(MediaType.TEXT_PLAIN)
    public String process(@PathParam("param") String cmd) {
        //String cmdRes = exectuteCmd(cmd);
        System.out.println(cmd);
        return "You told me to run "+cmd;
    }

    private String exectuteCmd(String cmd){
        return "";
    }

    /*@GET
    @Path("ping")
    @Produces(MediaType.TEXT_PLAIN)
    public String ping() {
        return "pong!";
    }

    @GET
    @Path("cd")
    @Produces(MediaType.TEXT_PLAIN)
    public String cd() {
        return "";
    }

    @GET
    @Path("ls")
    @Produces(MediaType.TEXT_PLAIN)
    public String ls() {
        return "";
    }

    @GET
    @Path("pwd")
    @Produces(MediaType.TEXT_PLAIN)
    public String pwd() {
        return "";
    }

    @GET
    @Path("cat")
    @Produces(MediaType.TEXT_PLAIN)
    public String cat() {
        return "";
    }

    @GET
    @Path("mkdir")
    @Produces(MediaType.TEXT_PLAIN)
    public String mkdir() {
        return "";
    }

    @GET
    @Path("rmdir")
    @Produces(MediaType.TEXT_PLAIN)
    public String rmdir() {
        return "";
    }

    @GET
    @Path("echo")
    @Produces(MediaType.TEXT_PLAIN)
    public String echo() {
        return "";
    }

    @GET
    @Path(">")
    @Produces(MediaType.TEXT_PLAIN)
    public String redirectOutputTo() {
        return "";
    }*/

}