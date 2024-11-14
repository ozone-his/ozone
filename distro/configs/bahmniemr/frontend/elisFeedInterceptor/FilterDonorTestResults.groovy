import org.openmrs.*
import org.bahmni.module.elisatomfeedclient.api.domain.OpenElisAccession;
import org.bahmni.module.elisatomfeedclient.api.domain.OpenElisTestDetail;
import org.bahmni.module.elisatomfeedclient.api.elisFeedInterceptor.ElisFeedAccessionInterceptor;
import java.util.*


public class FiterDonorTestResults implements ElisFeedAccessionInterceptor {

    public ArrayList<String> donorTests = ["Haemoglobin (Relative)","VDRL Rapid (Relative)", "VDRL ELISA (Relative)",
                                   "Blood Group (Relative)","Malaria Parasite (Relative)","HIV Tridot (Relative)","HIV ELISA (Blood) (Relative)",
                                    "HCV Tridot (Relative)","Malaria Parasite (Relative)","VDRL ELISA (Relative)","DRL Rapid (Relative)"];
    @Override
    public void run(OpenElisAccession openElisAccession) {
        Iterator<OpenElisTestDetail> iter = openElisAccession.getTestDetails().iterator();
        while(iter.hasNext()){
            if(donorTests.contains(iter.next().getTestName())) {
                iter.remove();
            }
        }
    }

}


