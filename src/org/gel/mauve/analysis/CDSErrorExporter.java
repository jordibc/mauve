package org.gel.mauve.analysis;
 
import java.util.HashMap;
import java.util.Iterator;
import java.util.TreeMap;
import java.util.Vector;

import org.biojava.bio.seq.DNATools;
import org.biojava.bio.seq.FeatureHolder;
import org.biojava.bio.seq.RNATools;
import org.biojava.bio.symbol.IllegalAlphabetException;
import org.biojava.bio.symbol.IllegalSymbolException;
import org.biojava.bio.symbol.SymbolList;
import org.biojava.bio.symbol.SymbolListFactory;
import org.gel.mauve.XmfaViewerModel;

public class CDSErrorExporter {

	/**
	 * A mapping of CDS ids to vectors containing 
	 * a list of all errors found in that CDS
	 */
	private HashMap<LiteWeightFeature,Vector<SNP>> snpErrors;
	
	private HashMap<LiteWeightFeature,Vector<Gap>> gapErrors;
	
	private XmfaViewerModel model;
	
	private HashMap<LiteWeightFeature,BrokenCDS> cds; 
	
	
	public CDSErrorExporter(XmfaViewerModel model, SNP[] snps, Gap[] assGaps){
		snpErrors = new HashMap<LiteWeightFeature,Vector<SNP>>();
		gapErrors = new HashMap<LiteWeightFeature,Vector<Gap>>();
		this.model = model;
		loadBrokenCDS(model, snps, assGaps);
		loadAASubs();
	}
	
	public BrokenCDS[] getBrokenCDS(){
		return cds.values().toArray(new BrokenCDS[cds.size()]);
	}
	
	/**
	 * 
	 * @param model
	 * @param snps
	 * @param assGaps gaps in the assembly
	 * @return
	 */
	private void loadBrokenCDS(XmfaViewerModel model, SNP[] snps, Gap[] assGaps){
		if (model.getGenomes().size() > 2)
			return;
		for (int snpI = 0; snpI < snps.length; snpI++){
			SNP s = snps[snpI];
			FeatureHolder holder = s.getFeatures(0);
			LiteWeightFeature[] feats = OneToOneOrthologExporter.
							getFeaturesByType(0, holder.features(), "CDS");
			
			for (int featI = 0; featI < feats.length; featI++){
				LiteWeightFeature feat = feats[featI];
				if (snpErrors.containsKey(feat)){
					snpErrors.get(feat).add(s);
				} else {
					Vector<SNP> tmp = new Vector<SNP>();
					tmp.add(s);
					snpErrors.put(feat, tmp);
				}
			}
		} 
		
		for (int gapI = 0; gapI < assGaps.length; gapI++){
			Gap g = assGaps[gapI];
			FeatureHolder holder = g.getFeatures(0);
			LiteWeightFeature[] feats = OneToOneOrthologExporter.
							getFeaturesByType(0, holder.features(), "CDS");
			for (int featI = 0; featI < feats.length; featI++){
				LiteWeightFeature feat = feats[featI];
				if (gapErrors.containsKey(feat)){
					gapErrors.get(feat).add(g);
				} else {
					Vector<Gap> tmp = new Vector<Gap>();
					tmp.add(g);
					gapErrors.put(feat, tmp);
				}
			}
		}
	}
	
	private void loadAASubs(){
		Iterator<LiteWeightFeature> it = snpErrors.keySet().iterator();
		while(it.hasNext()){
			LiteWeightFeature feat = it.next();
			if (gapErrors.containsKey(feat)){
				continue;
			}
			Vector<SNP> snps = snpErrors.get(feat);
			int l = feat.getLeft();
			int r = feat.getRight();
			char[] refSeq = model.getSequence(l, r, 0);
			long[] leftLCB = model.getLCBAndColumn(0, l);
			long[] rightLCB = model.getLCBAndColumn(0,r);
			long[] left_pos = new long[model.getGenomes().size()];
			boolean[] gap = new boolean[leftLCB.length];
			long[] right_pos = new long[model.getGenomes().size()];
			model.getColumnCoordinates((int)leftLCB[0], leftLCB[1], left_pos, gap);
			model.getColumnCoordinates((int)rightLCB[0], rightLCB[1], right_pos, gap);
			char[] assSeq = model.getSequence(left_pos[1], right_pos[1], 1);
			try {
				refSeq = RNATools.translate(
						 DNATools.toRNA(
						 DNATools.createDNA(new String(refSeq))))
						 .toString()
						 .toCharArray();
				assSeq = RNATools.translate(
						 DNATools.toRNA(
						 DNATools.createDNA(new String(assSeq))))
						 .toString()
						 .toCharArray();
			} catch (IllegalSymbolException e) {
				e.printStackTrace();
			} catch (IllegalAlphabetException e) {
				e.printStackTrace();
			}
			if (refSeq.length != assSeq.length){
				System.err.println("Different lengths: refSeq = " +refSeq.length + " assSeq = " + assSeq.length);
				continue;
			}
			for (int i = 0; i < assSeq.length; i++){
				if (assSeq[i] != refSeq[i]) {
					if (assSeq[i] == '*') {
						if (cds.containsKey(feat)){
							cds.get(feat).addPrmtrStop(i+1, refSeq[i]);
						} else {
							BrokenCDS tmp = new BrokenCDS(feat);
							tmp.addPrmtrStop(i+1, refSeq[i]);
							cds.put(feat, tmp);
						}
					} else {
						if (cds.containsKey(feat)){
							cds.get(feat).addSubstitution(i+1, refSeq[i], assSeq[i]);
						} else {
							BrokenCDS tmp = new BrokenCDS(feat);
							tmp.addSubstitution(i+1, refSeq[i], assSeq[i]);
							cds.put(feat, tmp);
						}
					}
				}                           
			}
		}
	}
}
