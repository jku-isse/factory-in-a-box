package capabilities;

public class WiringInformation {


    private String lOCAL_CAPABILITYID;
    private String rEMOTE_ENDPOINT;
    private String remote_NODEID_NameSpace;
    private String remote_NODEID_STRINGID;
    private String rEMOTE_NODEID;
    private String rEMOTE_ROLE;


    public WiringInformation(String LOCAL_CAPABILITYID, String REMOTE_ENDPOINT, String REMOTE_NODEID, String REMOTE_ROLE) {

        this.lOCAL_CAPABILITYID = LOCAL_CAPABILITYID;
        this.rEMOTE_ENDPOINT = REMOTE_ENDPOINT;
        this.rEMOTE_NODEID = REMOTE_NODEID;
        this.rEMOTE_ROLE = REMOTE_ROLE;

        String[] nodeDetailsID = REMOTE_NODEID.split(";");
        remote_NODEID_NameSpace = "";
        remote_NODEID_STRINGID = "";
        if (nodeDetailsID.length > 1) {
            remote_NODEID_NameSpace = nodeDetailsID[0].split("=")[1];
            remote_NODEID_STRINGID = nodeDetailsID[1].split("=")[1];
        }
    }

    public String getlOCAL_CAPABILITYID() {
        return lOCAL_CAPABILITYID;
    }

    public String getrEMOTE_ENDPOINT() {
        return rEMOTE_ENDPOINT;
    }

    public String getrEMOTE_ROLE() {
        return rEMOTE_ROLE;
    }

    public String getRemote_NODEID_NameSpace() {
        return remote_NODEID_NameSpace;
    }

    public String getRemote_NODEID_STRINGID() {
        return remote_NODEID_STRINGID;
    }
}
