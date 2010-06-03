package org.gel.mauve.assembly;

import java.io.File;
import org.gel.mauve.Genome;


import java.io.IOException;
import java.io.PrintStream;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.TreeSet;
import java.util.Vector;

import org.gel.mauve.XmfaViewerModel;
import org.gel.mauve.analysis.BrokenCDS;
import org.gel.mauve.analysis.CDSErrorExporter;
import org.gel.mauve.analysis.Gap;
import org.gel.mauve.analysis.PermutationExporter;
import org.gel.mauve.analysis.SNP;
import org.gel.mauve.analysis.SnpExporter;
import org.gel.mauve.contigs.ContigOrderer;
import org.gel.mauve.dcjx.Adjacency;
import org.gel.mauve.dcjx.DCJ;
import org.gel.mauve.gui.AlignmentProcessListener;

public class AssemblyScorer implements AlignmentProcessListener {

	private ContigOrderer co;
	
	private File alnmtFile;
	
	private File outputDir;
	
	private boolean batch;
	
	private String basename;
	
	private XmfaViewerModel model; 
	
	private int[][] subs;
	
	private SNP[] snps;
	
	private Gap[] refGaps;
	
	private Gap[] assGaps; 
	
	private BrokenCDS[] cds;
	
	private CDSErrorExporter cdsEE;
	
	private DCJ dcj;
	
	private boolean getBrokenCDS;
	
	/** extra adjacencies */
	private Vector<Adjacency> typeI;
	
	/** missing adjacencies */
	private Vector<Adjacency> typeII;
	
	public AssemblyScorer(XmfaViewerModel model, boolean getBrokenCDS){
		this.model = model;
		this.getBrokenCDS = getBrokenCDS;
		loadInfo();
	}
	
	public AssemblyScorer(File alnmtFile, File outDir, boolean getBrokenCDS) {
		this.alnmtFile = alnmtFile;
		this.outputDir = outDir;
		basename = alnmtFile.getName();
		basename = basename.substring(0,basename.lastIndexOf("."));
		batch = false;
	}
	
	public AssemblyScorer(File alnmtFile, File outDir, String basename, boolean getBrokenCDS) {
		this(alnmtFile,outDir,getBrokenCDS);
		this.basename = basename;
	}
	
	public AssemblyScorer(ContigOrderer co, File outDir, boolean getBrokenCDS) {
		this.co = co;
		this.outputDir = outDir;
		batch = false;
	}
	
	public AssemblyScorer(ContigOrderer co, File outDir, String basename, boolean getBrokenCDS) {
		this(co,outDir,getBrokenCDS);
		this.basename = basename;
	}
	
	public void completeAlignment(int retcode){
		if (retcode == 0) {
			if (co != null){
				alnmtFile = co.getAlignmentFile();
				if (basename == null) {
					basename = alnmtFile.getName();
					basename = basename.substring(0,basename.lastIndexOf("."));
				}
			}
			try {
				this.model = new XmfaViewerModel(alnmtFile,null);
			} catch (IOException e) {
				System.err.println("Couldn't load alignment file " 
									+ alnmtFile.getAbsolutePath());
				e.printStackTrace();
			}
			if (model != null) {
				loadInfo();
				printInfo(this, outputDir, basename, batch);
			}
			
		} else { 
			System.err.println("Alignment failed with error code "+ retcode);
		}
			
	}
	
	private void computeAdjacencyErrors(){
		Adjacency[] ref = dcj.getAdjacencyGraph().getGenomeA();
		Adjacency[] ass = dcj.getAdjacencyGraph().getGenomeB();
		Comparator<Adjacency> comp = new Comparator<Adjacency>(){
			public int compare(Adjacency o1, Adjacency o2) {
				String s1 = new String(min(o1.getFirstBlockEnd(),o1.getSecondBlockEnd()));
				s1 = s1 + max(o1.getFirstBlockEnd(),o1.getSecondBlockEnd());
				String s2 = new String(min(o2.getFirstBlockEnd(),o2.getSecondBlockEnd()));
				s2 = s2 + max(o2.getFirstBlockEnd(),o2.getSecondBlockEnd());
				return s1.compareTo(s2);
			}
			private String min(String s1, String s2){
				if (s1.compareTo(s2) > 0)
					return s2;
				else 
					return s1;
			}
			private String max(String s1, String s2){
				if (s1.compareTo(s2) > 0)
					return s1;
				else 
					return s2;
			}
		};
		// false positives : adjacencies in assembly that aren't in the reference
		typeI = new Vector<Adjacency>();
		// false negatives : adjacencies in reference that aren't in the assembly
		typeII = new Vector<Adjacency>();
		
		TreeSet<Adjacency> refSet = new TreeSet<Adjacency>(comp);
		for (Adjacency a: ref) { refSet.add(a);}
		
		System.err.println("\nnum assembly adjacencies: " + ass.length);
		
		for (Adjacency a: ass){
			if (!refSet.contains(a)) 
				typeI.add(a);
		}
		
		System.err.println("num typeI errors: " + typeI.size());
		
		TreeSet<Adjacency> assSet = new TreeSet<Adjacency>(comp);
		for (Adjacency a: ass) { assSet.add(a);}
		
		System.err.println("num ref adjacencies: " + ref.length);
		
		for (Adjacency a: ref) {
			if (!assSet.contains(a))
				typeII.add(a);
		}
		System.err.println("num typeII errors: " + typeII.size());
	}
	
	/**
	 * computes info. sorts gaps and snps
	 */
	private synchronized void loadInfo(){
		model.setReference(model.getGenomeBySourceIndex(0));
		
		System.out.print("Computing signed permutations....");
		String[] perms = PermutationExporter.getPermStrings(model, true); 
		System.out.print("done!\n");
		
		System.out.print("Performing DCJ rearrangement analysis...");
		this.dcj = new DCJ(perms[0], perms[1]);
		System.out.print("done!\n");
		
		System.out.print("Computing adjacency errors...");
		computeAdjacencyErrors();
		System.out.print("done!\n");
		
		System.out.print("Getting SNPs...");
		this.snps = SnpExporter.getSNPs(model);
		System.out.print("done!\n");
		
		System.out.print("Counting base substitutions...");
		this.subs = ScoreAssembly.countSubstitutions(snps);
		System.out.print("done!\n");
		
		System.out.print("Counting gaps...");
		Gap[][] tmp = SnpExporter.getGaps(model);
		System.out.print("done!\n");
		
		refGaps = tmp[0];
		assGaps = tmp[1];
		Arrays.sort(assGaps);
		Arrays.sort(refGaps);
		System.out.flush();
		if (getBrokenCDS){
			computeBrokenCDS();
		}
	}
	
	private void computeBrokenCDS(){
		Iterator<Genome> it = model.getGenomes().iterator();
		boolean haveAnnotations = true;
		while(it.hasNext()){
			haveAnnotations = haveAnnotations &&
				(it.next().getAnnotationSequence() != null);
		}
		haveAnnotations = model.getGenomeBySourceIndex(0).getAnnotationSequence() != null;
		if (haveAnnotations){
			System.out.print("Getting broken CDS...");
			System.out.flush();
			this.cdsEE = new CDSErrorExporter(model, snps, assGaps, refGaps);
			try {
				cds = cdsEE.getBrokenCDS();
				System.out.print("done!\n");
			} catch (Exception e){
				System.err.println("\n\nfailed to compute broken CDS. Reason given below");
				System.err.println(e.getMessage());
				e.printStackTrace();
			} 
		}
	}
	
	public XmfaViewerModel getModel(){
		return this.model;
	}

	public DCJ getDCJ(){
		return dcj;
	}
	
	public Gap[] getReferenceGaps(){
		return refGaps;
	}
	
	public Gap[] getAssemblyGaps(){
		return assGaps;
	}
	
	public SNP[] getSNPs(){
		return snps;
	}
	
	public int[][] getSubs(){
		return subs;
	}
	
	public boolean hasBrokenCDS(){
		if (cds == null) 
			return false;
		else {
			return cds.length > 0;
		}
	}
	
	public BrokenCDS[] getBrokenCDS(){
		return cds;
	}
	
	public int getSCJdist(){
		return dcj.scjDistance();
	}
	
	public int getDCJdist(){
		return dcj.dcjDistance();
	}
	
	public int getBPdist(){
		return dcj.bpDistance();
	}
	
	public int numBlocks(){
		return dcj.numBlocks();
	}

	public double typeIadjErr(){
		return ((double) typeI.size()) / 
			((double) dcj.getAdjacencyGraph()
						 .getGenomeB().length);
	}
	
	public double typeIIadjErr(){
		return ((double) typeII.size()) /
			((double) dcj.getAdjacencyGraph()
						 .getGenomeA().length);
	}
	
	public int numLCBs(){
		return (int) model.getLcbCount();
	}
	
	public int numContigs(){
		return model.getGenomeBySourceIndex(1).getChromosomes().size();
	}

	public long numReplicons(){
		return model.getGenomeBySourceIndex(0).getChromosomes().size();
	}
	
	
	public long numBasesAssembly(){
		return model.getGenomeBySourceIndex(1).getLength();
	}
	
	public long numBasesReference(){
		return model.getGenomeBySourceIndex(0).getLength();
	}
	
	
	public double percentMissedBases(){
		double totalBases = model.getGenomeBySourceIndex(0).getLength();
		double missedBases = 0;
		for(int i = 0; i < assGaps.length; i++){
			missedBases += assGaps[i].getLength();
		}
		return missedBases/totalBases;
	}
	
	public long totalMissedBases(){
		long missedBases = 0;
		for(int i = 0; i < assGaps.length; i++){
			missedBases += assGaps[i].getLength();
		}
		return missedBases;
	}
	
	/**
	 * 
	 * @return numExtraBases/totalNumBases
	 */
	public double percentExtraBases(){
		double totalBases = model.getGenomeBySourceIndex(1).getLength();
		double extraBases = 0;
		for (int i = 0; i < refGaps.length; i++){
			extraBases += refGaps[i].getLength();
		}
		return extraBases/totalBases;
	}
	
	public long totalExtraBases(){
		long extraBases = 0;
		for (int i = 0; i < refGaps.length; i++){
			extraBases += refGaps[i].getLength();
		}
		return extraBases;
	}
	
	public static void printInfo(AssemblyScorer sa, File outDir, String baseName, boolean batch){
		PrintStream gapOut = null;
		PrintStream snpOut = null;
		PrintStream sumOut = null;
		try {
			File gapFile = new File(outDir, baseName+"__gaps.txt");
			gapFile.createNewFile();
			gapOut = new PrintStream(gapFile);
			File snpFile = new File(outDir, baseName+"__snps.txt");
			snpFile.createNewFile();
			snpOut = new PrintStream(snpFile);
			File sumFile = new File(outDir, baseName+"__sum.txt");
			sumFile.createNewFile();
			sumOut = new PrintStream(sumFile);
			
		} catch (IOException e){
			e.printStackTrace();
			System.exit(-1);    
		}
		printInfo(sa,snpOut,gapOut);
		if (batch){
		    sumOut.print(ScoreAssembly.getSumText(sa, false, true));	
		}else {
		    sumOut.print(ScoreAssembly.getSumText(sa, true, true));
		}
		
		gapOut.close();
		snpOut.close();
		sumOut.close();
	}
	
	/**
	 * Prints the SNP and gap data from this AssemblyScorer object out to the 
	 * respective <code>PrintStream</code>s. 
	 * <br>
	 * NOTE: <code>snpOut</code> and <code>gapOut</code> 
	 * can take null values
	 * </br>
	 * @param sa the AssemblyScorer to print info for
	 * @param snpOut stream to print SNP info to
	 * @param gapOut stream to print gap info to
	 */
	public static void printInfo(AssemblyScorer sa,  PrintStream snpOut, PrintStream gapOut){
		if (snpOut!=null){
			StringBuilder sb = new StringBuilder();
			sb.append("SNP_Pattern\tRef_Contig\tRef_PosInContig\tRef_PosGenomeWide\tAssembly_Contig\tAssembly_PosInContig\tAssembly_PosGenomeWide\n");
			for (int i = 0; i < sa.snps.length; i++)
				sb.append(sa.snps[i].toString()+"\n");
			snpOut.print(sb.toString());
			snpOut.flush();
		}
		if (gapOut!=null){
			StringBuilder sb = new StringBuilder();
			sb.append("Sequence\tContig\tPosition_in_Contig\tGenomeWide_Position\tLength\n");
			for (int i = 0; i < sa.refGaps.length; i++)
				sb.append(sa.refGaps[i].toString("reference")+"\n");
			for (int i = 0; i < sa.assGaps.length; i++)
				sb.append(sa.assGaps[i].toString("assembly")+"\n");
			gapOut.print(sb.toString());
			gapOut.flush();
		}
		
	}
}
