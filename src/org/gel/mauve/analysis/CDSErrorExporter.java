package org.gel.mauve.analysis;

import java.util.Arrays;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeMap;
import java.util.HashSet;
import java.util.Vector;

import org.biojava.bio.seq.DNATools;
import org.biojava.bio.seq.FeatureHolder;
import org.biojava.bio.seq.RNATools;
import org.biojava.bio.symbol.AtomicSymbol;
import org.biojava.bio.symbol.IllegalAlphabetException;
import org.biojava.bio.symbol.IllegalSymbolException;
import org.biojava.bio.symbol.Symbol;
import org.biojava.bio.symbol.SymbolList;
import org.biojava.bio.symbol.SymbolListFactory;
import org.gel.mauve.MauveHelperFunctions;
import org.gel.mauve.XmfaViewerModel;

/**
 * A class for extracting broken CDS features from a draft genome.
 *
 * @author atritt
 *
 */
public class CDSErrorExporter {

	private static final char BAD_CODON = '!';

	/**
	 * A mapping of CDS ids to vectors containing
	 * a list of all errors found in that CDS
	 */
	private HashMap<LiteWeightFeature,Vector<SNP>> snpErrors;

	private HashMap<LiteWeightFeature,Vector<Gap>> gapErrors;

	private XmfaViewerModel model;

	private HashMap<LiteWeightFeature,BrokenCDS> brokenCDS;

	private int numCDS;

	private int numBrokenCDS;


	public CDSErrorExporter(XmfaViewerModel model, SNP[] snps, Gap[] assGaps, Gap[] refGaps){
		snpErrors = new HashMap<LiteWeightFeature,Vector<SNP>>();
		gapErrors = new HashMap<LiteWeightFeature,Vector<Gap>>();
		this.model = model;
		brokenCDS = new HashMap<LiteWeightFeature, BrokenCDS>();
		projectBrokenCDS(model, snps, assGaps, refGaps);
		refineBrokenCDS();
		HashSet<LiteWeightFeature> bcds = new HashSet<LiteWeightFeature>(snpErrors.keySet());
		Iterator<LiteWeightFeature> it = gapErrors.keySet().iterator();
		while(it.hasNext())
			bcds.add(it.next());
		bcds.addAll(gapErrors.keySet());
		numBrokenCDS = bcds.size();
	}

	public BrokenCDS[] getBrokenCDS(){
		return brokenCDS.values().toArray(new BrokenCDS[brokenCDS.size()]);
	}

	public int numBrokenCDS(){
		return numBrokenCDS;
	}

	public int numCompleteCDS(){
		return numCDS - numBrokenCDS;
	}

	/**
	 * Finds all CDS that overlap with any of the SNPs or Gaps passed in.
	 *
	 * @param model
	 * @param snpsAr
	 * @param assGaps gaps in the assembly
	 * @return
	 */
	private void projectBrokenCDS(XmfaViewerModel model, SNP[] snpsAr, Gap[] assGaps, Gap[] refGaps){
		if (model.getGenomes().size() > 2)
			return;
		LiteWeightFeature[] cds = OneToOneOrthologExporter.getFeaturesByType(0,
				model.getGenomeBySourceIndex(0).getAnnotationSequence().features(), "CDS");
		numCDS = cds.length;
		System.err.println("numCDS: " + numCDS);
		SNP[] snps = new SNP[snpsAr.length];
		System.arraycopy(snpsAr, 0, snps, 0, snps.length);
		Arrays.sort(snps, SNP.getLoopingComparator(0));
		Arrays.sort(cds, LiteWeightFeature.getLoopingComparator());
		int snpI = 0;
		int cdsI = 0;
		while (cdsI < cds.length && snpI < snps.length){
			int comp = snps[snpI].relativePos(cds[cdsI]);
			if (comp > 0){
				cdsI++;
			} else if (comp < 0){
				snpI++;
			} else {// add this SNP and get the next one
				if (snpErrors.containsKey(cds[cdsI])) {
					snpErrors.get(cds[cdsI]).add(snps[snpI]);
				} else {
					Vector<SNP> v = new Vector<SNP>();
					snpErrors.put(cds[cdsI], v);
				}
				snpI++;
			}
		}

		Gap[] gaps = new Gap[assGaps.length];
		System.arraycopy(assGaps, 0, gaps, 0, gaps.length);
		Arrays.sort(gaps, Gap.getAlnmtPosComparator());
		int gapI = 0;
		cdsI = 0;
		while (cdsI < cds.length && gapI < gaps.length){
			int comp = gaps[gapI].relativePos(cds[cdsI]);
			if (comp > 0){
				cdsI++;
			} else if (comp < 0) {
				gapI++;
			} else {
				if (gapErrors.containsKey(cds[cdsI])){
					gapErrors.get(cds[cdsI]).add(gaps[gapI]);
				} else {
					Vector<Gap> v = new Vector<Gap>();
					gapErrors.put(cds[cdsI], v);
				}
				gapI++;
			}
		}

		gaps = new Gap[refGaps.length];
		System.arraycopy(refGaps, 0, gaps, 0, refGaps.length);
		gapI = 0;
		cdsI = 0;
		while(cdsI < cds.length && gapI < gaps.length){
			int comp = gaps[gapI].relativePos(cds[cdsI]);
			if (comp > 0) {
				cdsI++;
			} else if (comp < 0){
				gapI++;
			} else {
				if (gapErrors.containsKey(cds[cdsI])){
					gapErrors.get(cds[cdsI]).add(gaps[gapI]);
				} else {
					Vector<Gap> v = new Vector<Gap>();
					gapErrors.put(cds[cdsI],v);
				}
				gapI++;
			}
		}

	}

	private void refineBrokenCDS(){
		loadSnpErrors();
		loadDelErrors();
	}

	/**
	 *
	 */
	private void loadSnpErrors(){
		Iterator<LiteWeightFeature> it = snpErrors.keySet().iterator();
		while(it.hasNext()){
			LiteWeightFeature feat = it.next();
			if (gapErrors.containsKey(feat)){
				continue;
			} else {
				char[] refSeq = model.getSequence(feat.getLeft(), feat.getRight(), 0);
				long[] leftLCB = model.getLCBAndColumn(0, feat.getLeft());
				long[] rightLCB = model.getLCBAndColumn(0,feat.getRight());
				long[] left_pos = new long[model.getGenomes().size()];
				boolean[] gap = new boolean[leftLCB.length];
				long[] right_pos = new long[model.getGenomes().size()];
				model.getColumnCoordinates((int)leftLCB[0], leftLCB[1], left_pos, gap);
				model.getColumnCoordinates((int)rightLCB[0], rightLCB[1], right_pos, gap);
				if (left_pos[1] == 0 || right_pos[1] == 0) {
//					System.err.println("BAD_SNP_ERROR : feature " + feat.getID());
					continue;
				} else if (left_pos[0] != right_pos[0]) {
//					System.err.println("Different LCBs");
				} else {
					char[] assSeq = model.getSequence(left_pos[1], right_pos[1], 1);
					computeSubstitutions(feat,refSeq,assSeq);
				}
			}
		}
	}

	private void loadDelErrors(){
		Iterator<LiteWeightFeature> it = gapErrors.keySet().iterator();

		BrokenCDS bcds = null;
		while (it.hasNext()) {
			LiteWeightFeature feat = it.next();
			try{
				refineBrokenCDS(feat);
			}catch(Exception e){
				System.err.println("Warning, unable to process reference gene " + feat.getID());
			}
		}

		it = snpErrors.keySet().iterator();
		while(it.hasNext()) {
			LiteWeightFeature feat = it.next();
			try{
				refineBrokenCDS(feat);
			}catch(Exception e){
				System.err.println("Warning, unable to process reference gene " + feat.getID());
			}
		}

	}

	/**
	 * Runs over the gene alignment with a fine tooth comb.
	 * Here, we look for frameshift errors, AA substitution errors,
	 * including premature stops, and gaps in the AA sequence.
	 *
	 * @param feat
	 */
	private void refineBrokenCDS(LiteWeightFeature feat) {
	//	LiteWeightFeature feat = it.next();
		BrokenCDS bcds = null;
		if (brokenCDS.containsKey(feat)) {
			bcds = brokenCDS.get(feat);
		} else {
			bcds = new BrokenCDS(feat);
			brokenCDS.put(feat, bcds);
		}
		byte[][] alnmt = model.getSequenceRange(0, feat.getLeft(), feat.getRight());
		if (alnmt[0].length != alnmt[1].length) {
//			System.err.println("Different sequence lengths");
		}

		byte[][][] codons = splitOnRefCodons(alnmt);

		int numCodons = 0;
		int numBadCodons = 0;
		int frameShift = 0;
		boolean inFrame = true;
		int lastInFrame = 1;
		boolean inGap = false;
		int lastNoGap = 1;
		int assNACount = 0;

		for (int cdnI = 0; cdnI < codons.length; cdnI++) {
			byte[][] codon = codons[cdnI];
			String ref = new String(codon[0]);
			String ass = new String(codon[1]);
			int refGapCount = numGaps(codon[0]);
			int assGapCount = numGaps(codon[1]);
			assNACount = assNACount + codon[1].length - assGapCount;
			// FIXME
			if (frameShift % 3 == 0){ // in-frame


				if (refGapCount == 0){ // make sure this isn't an inter-codon gap
					numCodons++;
					if (isCodon(codon[1])){ // make sure we don't have an intra-codon gap
						char aa_ref = translate(trimGaps(codon[0]));
						char aa_ass = translate(trimGaps(codon[1]));
						if (aa_ref != aa_ass){
							numBadCodons++;
							if (aa_ass == '*' && aa_ref != '*'){ // check for nonsense mutation
								bcds.addPrmtrStop(numCodons, aa_ref);
							} else {
								bcds.addAASubstitution(numCodons, aa_ref, aa_ass);
							}
						} else {
							lastNoGap = numCodons;
						}
						if (inGap) { // ending the stretch of gap
							inGap = false;
							int[] tmp = {lastNoGap, numCodons-1};
							bcds.addGapSegment(tmp);
						}
					} else { // bad codon
						numBadCodons++;
						if (inGap) {
							if (assGapCount % 3 == 0) { // still in frame

							} else { // not a gap. just a stretch of broken frame

							}
						} else {
							inGap = true;
							lastNoGap = numCodons;

						}
					}
					lastInFrame = numCodons;
				} else {  // we will enter here if the reference codon is a blank
					if (codon[0].length == 3) {
						if (isCodon(codon[1])){
							char aa = translate(codon[1]);
							if (aa == '*')
								bcds.addInsertionStop(assNACount/3);
						}
					}
				}
			} else { // out of frame, so this codon won't be correct

				if (isCodon(codon[0])){
					numCodons++;
					numBadCodons++;
				}else {
					System.out.flush();
				}
				// AJT0403: check for stop codon due to frameshift here

			}
			frameShift += assGapCount - refGapCount;

			if (inFrame) {
				if (frameShift % 3 != 0) {
					inFrame = false;
					lastInFrame = numCodons;
					// starting a stretch of broken frame
				}
			} else {
				if (frameShift % 3 == 0 || cdnI == codons.length - 1) {
					inFrame = true;
					int[] tmp = {lastInFrame+1 , numCodons};
					bcds.addBFSegment(tmp);
					// ending a stretch of broken frame
				}
			}


		}
		bcds.setAASubRate(((double)numBadCodons)/((double)numCodons));
	}

	private void computeSubstitutions(LiteWeightFeature feat, char[] refSeq, char[] assSeq){
		if (refSeq.length != assSeq.length){
			throw new IllegalArgumentException("Sequences must be the same length: ref length = "
										+ refSeq.length + " assembly length = " + assSeq.length);
		}
		try {
			char[] tmp = RNATools.translate(
					  	 DNATools.toRNA(
					  	 DNATools.createDNA(new String(refSeq))))
					  	 .toString()
					  	 .toCharArray();
			refSeq = tmp;
		} catch (IllegalSymbolException e) {
			System.err.println(e.getMessage());
			System.err.println("Bad Symbol in the reference sequence: \n >>" + new String(assSeq)+"<<");
			e.printStackTrace();
		} catch (IllegalAlphabetException e) {
			e.printStackTrace();
		}
		try {
			char[] tmp = RNATools.translate(
					 DNATools.toRNA(
					 DNATools.createDNA(new String(assSeq))))
					 .toString()
					 .toCharArray();
			assSeq = tmp;
		} catch (IllegalSymbolException e) {
			System.err.println(e.getMessage());
			System.err.println("Bad Symbol in the assembly sequence: \n >>" + new String(assSeq)+"<<");
			e.printStackTrace();
		} catch (IllegalAlphabetException e) {
			e.printStackTrace();
		}

		for (int i = 0; i < assSeq.length; i++){
			if (assSeq[i] != refSeq[i]) {
				if (assSeq[i] == '*') {
					if (brokenCDS.containsKey(feat)){
						brokenCDS.get(feat).addPrmtrStop(i+1, refSeq[i]);
					} else {
						BrokenCDS tmp = new BrokenCDS(feat);
						tmp.addPrmtrStop(i+1, refSeq[i]);
						brokenCDS.put(feat, tmp);
					}
				} else {
					if (brokenCDS.containsKey(feat)){
						brokenCDS.get(feat).addNASubstitution(i+1, refSeq[i], assSeq[i]);
					} else {
						BrokenCDS tmp = new BrokenCDS(feat);
						tmp.addNASubstitution(i+1, refSeq[i], assSeq[i]);
						brokenCDS.put(feat, tmp);
					}
				}
			}
		}
	}

	/**
	 * Returns true if we have 3 valid bases in this byte array, false otherwise
	 * @param ar
	 * @return
	 */
	public static boolean isCodon(byte[] ar) {
		int numBases = 0;
		for (byte b: ar)
			if (isValidNucAcid(b)) numBases++;
		return numBases == 3;
	}

	public static boolean hasGap(byte[] ar){
		for (int i = 0; i < ar.length; i++){
			if (ar[i] == '-')
				return true;
		}
		return false;
	}

	public static int numGaps(byte[] ar){
		int ret = 0;
		for (byte b: ar)
			if (b == '-') ret++;
		return ret;
	}

	public static byte[] trimGaps(byte[] ar){
		int numBases = ar.length - numGaps(ar);
		byte[] ret = new byte[numBases];
		int newBaseI = 0;
		for (int i = 0; i < ar.length; i++){
			if (ar[i] != '-')
				ret[newBaseI++] = ar[i];
		}
		return ret;
	}

	//public byte[] subArray(byte[] in, )

	/**
	 * <br>
	 * Returns an array of 2D byte arrays.
	 * The first index of one of these 2D byte arrays will
	 * contain the reference codon, and the
	 * second index will contain the assembly
	 * codon.
	 *</br>
	 *
	 *
	 * <br>
	 * Reference arrays always length [1,3] and
	 * always have either 0 or 3 nucleic acid codes.
	 * </br>
	 * <br>
	 * Assembly arrays always length [1,3] and
	 * can have any number of nucleic acid codes.
	 * </br>
	 *
	 */
	public static byte[][][] splitOnRefCodons(byte[][] ar){
		byte[] ref = ar[0];
		byte[] ass = ar[1];
		// first, make sure we have a valid coding sequence
		int numBases = 0;
		for (int i = 0; i < ref.length; i++){
			if (isValidNucAcid(ref[i])) numBases++;
		}
		if (numBases % 3 != 0) {
			throw
				new IllegalArgumentException("Number of references bases - "+
											numBases+" - not divisible by 3");
		}

		Vector<byte[][]> vect = new Vector<byte[][]>();
		int codonStart = 0;
		int baseI = 0;
		int codonPos = 0; // should only take on values 0,1,2,3
		int prevCodonEnd = 0; // assume the first base in the sequence is a valid nuc. acid
		boolean lookForNewCodonStart = false;
		int nGaps = 0;
		while (baseI < ref.length) {
			if (isValidNucAcid(ref[baseI])) {
				if (lookForNewCodonStart) {
					if (prevCodonEnd != baseI-1){
						/*  cut out the gap: everything between
						     prevCodonEnd (exc.) and baseI (exc.)  */
						byte[][] tmp = new byte[2][baseI-prevCodonEnd-1];
						System.arraycopy(ref, prevCodonEnd+1, tmp[0], 0, tmp[0].length);
						System.arraycopy(ass, prevCodonEnd+1, tmp[1], 0, tmp[1].length);
						vect.add(tmp);
					}
					codonStart = baseI;
					lookForNewCodonStart = false;
				}
				codonPos++;
			} else {
				if (ref[baseI] == '-') {
					nGaps++;
				}

				if (nGaps == 3) {
					nGaps = 0;
					byte[][] tmp = new byte[2][3];
					System.arraycopy(ref, baseI-2, tmp[0], 0, 3);
					System.arraycopy(ass, baseI-2, tmp[1], 0, 3);
					vect.add(tmp);
					prevCodonEnd = baseI;
				}

				if (lookForNewCodonStart)
					codonStart++;
				if (baseI == 0)
					throw new IllegalArgumentException(ref[baseI]+
							" : Sequence must start with a valid nucleic acid code.");
			}
			if (codonPos == 3) {
				/*  cut out everything between
				    prevCodonEnd and baseI   */
				byte[][] tmp = new byte[2][baseI-codonStart+1];
				System.arraycopy(ref, codonStart, tmp[0], 0, tmp[0].length);
				System.arraycopy(ass, codonStart, tmp[1], 0, tmp[1].length);
				vect.add(tmp);
				codonPos = 0;
				lookForNewCodonStart = true;
				prevCodonEnd = baseI;
			}
			baseI++;
		}
		return vect.toArray(new byte[vect.size()][][]);
	}

	public static char translate(byte[] codon){
		if (hasGap(codon)) {
			codon = trimGaps(codon);
		}
		try {
			return 	RNATools.translate(
					DNATools.toRNA(
					DNATools.createDNA(
					new String(codon))))
					.seqString().charAt(0);

		} catch (IndexOutOfBoundsException e) {
			e.printStackTrace();
		} catch (IllegalAlphabetException e) {
			e.printStackTrace();
		} catch (IllegalSymbolException e) {
			e.printStackTrace();
		}
		return '-';
	}



	private static String getBadChars(byte[] ar){
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < ar.length; i++){
			if (!isValidChar(ar[i])){
				sb.append((i+1)+":"+Character.valueOf((char)ar[i]) + " ");
			}
		}
		return sb.toString();
	}

	public static  boolean isValidChar(byte b){
			switch(b){
			case 'a': return true;
			case 'A': return true;
			case 'c': return true;
			case 'C': return true;
			case 't': return true;
			case 'T': return true;
			case 'g': return true;
			case 'G': return true;
			case 'k': return true;
			case 'K': return true;
			case 'm': return true;
			case 'M': return true;
			case 'r': return true;
			case 'R': return true;
			case 'y': return true;
			case 'Y': return true;
			case 's': return true;
			case 'S': return true;
			case 'w': return true;
			case 'W': return true;
			case 'b': return true;
			case 'B': return true;
			case 'v': return true;
			case 'V': return true;
			case 'h': return true;
			case 'H': return true;
			case 'd': return true;
			case 'D': return true;
			case 'x': return true;
			case 'X': return true;
			case 'n': return true;
			case 'N': return true;
			case '-': return true;
				default : return false;
			}
	}

	public static  boolean isValidNucAcid(byte b){
		switch(b){
		case 'a': return true;
		case 'A': return true;
		case 'c': return true;
		case 'C': return true;
		case 't': return true;
		case 'T': return true;
		case 'g': return true;
		case 'G': return true;
		case 'k': return true;
		case 'K': return true;
		case 'm': return true;
		case 'M': return true;
		case 'r': return true;
		case 'R': return true;
		case 'y': return true;
		case 'Y': return true;
		case 's': return true;
		case 'S': return true;
		case 'w': return true;
		case 'W': return true;
		case 'b': return true;
		case 'B': return true;
		case 'v': return true;
		case 'V': return true;
		case 'h': return true;
		case 'H': return true;
		case 'd': return true;
		case 'D': return true;
		case 'x': return true;
		case 'X': return true;
		case 'n': return true;
		case 'N': return true;
			default : return false;
		}
	}
}
