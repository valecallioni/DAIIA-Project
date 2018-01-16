package Mobility.Helpers;

public class Ontology {
	public static final String TOUR_GUIDE_REQUEST = "TOUR_GUIDE_REQUEST";
	public static final String TOUR_GUIDE_RESPONSE = "TOUR_GUIDE_RESPONSE";

	public static final String BUILD_TOUR_REQUEST = "MAKE_TOUR_REQUEST";
	public static final String BUILD_TOUR_RESPONSE = "MAKE_TOUR_RESPONSE";

	public static final String ARTIFACT_DETAILS_REQUEST = "ARTIFACT_DETAILS_REQUEST";
	public static final String ARTIFACT_DETAILS_RESPONSE = "ARTIFACT_DETAILS_RESPONSE";

	public static final String REQUEST_TOUR = "REQUEST_TOUR"; // from ProfilerAgent to TourGuideAgent
	public static final String REQUEST_MAKE_TOUR = "REQUEST_MAKE_TOUR"; // from TourGuideAgent to CuratorAgent
	public static final String REQUEST_ARTIFACT_INFO = "REQUEST_ARTIFACT_INFO"; // to Curator

	public static final String AUCTION = "AUCTION";
	public static final String STATUS_OF_AUCTION = "STATUS_OF_AUCTION";
	public static final String ENDED_AUCTION = "ENDED_AUCTION";
}
