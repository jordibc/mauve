package org.gel.mauve.gui.sequence;

import org.gel.mauve.MauveConstants;

public interface FlatFileFeatureConstants extends MauveConstants {

	/**
	 * strings representing required information for general flat file feature
	 * type
	 */
	public final static String TYPE_STRING = "type";

	public final static String LABEL_STRING = "label";

	public final static String CONTIG_STRING = "contig";

	public final static String STRAND_STRING = "strand";

	public final static String LEFT_STRING = "left_end";

	public final static String RIGHT_STRING = "right_end";

	public final static String FORWARD = "forward";
	
	public final static String COMPLEMENT = "complement";

	/**
	 * integers that programmatically represent the required information in the
	 * file
	 */
	public final static int TYPE = 0;

	public final static int LABEL = 1;

	public final static int CONTIG = 2;

	public final static int STRAND = 3;

	public final static int LEFT = 4;

	public final static int RIGHT = 5;
	
	/**
	 * known feature types
	 */
	public static final String CDS = "cds";

	/**
	 * Array that converts String to numeric representation of required fields
	 * in flat file
	 */
	public final static String [] FLAT_FEATURE_REQ_INFO = {TYPE_STRING,
			LABEL_STRING, CONTIG_STRING, STRAND_STRING, LEFT_STRING,
			RIGHT_STRING};
	
	/**
	 * array index for FilterCacheSpecs in filter_specs
	 */
	public static final int FILTER_SPEC_INDEX = 0;

	/**
	 * array index for OverlayRendererWrappers in filter_specs
	 */
	public static final int OVERLAY_REND_INDEX = 1;
	
	public static final double NO_OFFSET = -1.0;

}
