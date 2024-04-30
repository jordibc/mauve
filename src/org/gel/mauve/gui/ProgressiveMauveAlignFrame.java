package org.gel.mauve.gui;

import java.awt.Dimension;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.FilterWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Vector;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.gel.mauve.MyConsole;

public class ProgressiveMauveAlignFrame extends AlignFrame implements ChangeListener {


	/*
	 * These variables should be private. Before they had no modifier, which
	 * means they were accessible to to everything else in this package.
	 * There is no reason for these members to be directly accessible to
	 * the outside world. If you need to access them, make setters
	 * and getters -- don't be lazy!
	 */
    // member declarations
	protected Dimension d;
	protected JCheckBox refineCheckBox = new JCheckBox();
	protected JCheckBox seedFamiliesCheckBox = new JCheckBox();

	protected JCheckBox sumOfPairsCheckBox = new JCheckBox();
	protected JSlider breakpointWeightScaleSlider = new JSlider();
	protected JLabel breakpointWeightScaleLabel = new JLabel();
	protected JTextField breakpointWeightScaleText = new JTextField(5);
	protected JSlider conservationWeightScaleSlider = new JSlider();
	protected JLabel conservationWeightScaleLabel = new JLabel();
	protected JTextField conservationWeightScaleText = new JTextField(5);


	protected JPanel scorePanel = new JPanel();
	protected JComboBox matrixChoice = new JComboBox(new String[] {"HOXD (default)", "Custom"});
	protected JLabel matrixChoiceLabel = new JLabel();
	protected JTextField[][] scoreText = new JTextField[4][4];
	protected JLabel[] scoreLabelRow = new JLabel[4];
	protected JLabel[] scoreLabelCol = new JLabel[4];
	protected String[] scoreLabels = {"A", "C", "G", "T"};
	protected String[][] hoxd_matrix = {
    		{"91",   "-114", "-31",  "-123"},
    		{"-114", "100",  "-125", "-31" },
    		{"-31",  "-125", "100",  "-114"},
    		{"-123", "-31",  "-114", "91"  }
    };
	protected String hoxd_go = "-400";
	protected String hoxd_ge = "-30";
	protected JTextField gapOpenText = new JTextField();
	protected JLabel gapOpenLabel = new JLabel();
	protected JTextField gapExtendText = new JTextField();
	protected JLabel gapExtendLabel = new JLabel();

    public ProgressiveMauveAlignFrame(Mauve mauve)
    {
    	super(mauve);
    }
    public ProgressiveMauveAlignFrame(Mauve mauve, boolean gui)
    {
    	super(mauve, gui);
    }

    public void initComponents()
    {
    	super.initComponents();

        // the following code sets the frame's initial state
        defaultSeedCheckBox.setLocation(new java.awt.Point(10, 10));
        determineLCBsCheckBox.setLocation(new java.awt.Point(10, 50));
        seedLengthSlider.setLocation(new java.awt.Point(200, 30));
        seedLengthLabel.setLocation(new java.awt.Point(210, 10));
        recursiveCheckBox.setLocation(new java.awt.Point(10, 90));
        collinearCheckBox.setLocation(new java.awt.Point(10, 70));
        d = minLcbWeightLabel.getPreferredSize();
        minLcbWeightLabel.setLocation(new java.awt.Point(265 - d.width, 90));
        minLcbWeightText.setLocation(new java.awt.Point(270, 90));
//        alignButton.setLocation(new java.awt.Point(220, 320));

        //
        // add a panel to define MUSCLE behavior
        //
        scorePanel.setSize(new java.awt.Dimension(350, 150));
        scorePanel.setLocation(new java.awt.Point(0, 210));
        scorePanel.setVisible(true);
        scorePanel.setLayout(null);

        d = matrixChoice.getPreferredSize();
        matrixChoice.setSize(d);
        matrixChoice.setLocation(new java.awt.Point(130, 10));
        matrixChoice.setVisible(true);
    	scorePanel.add(matrixChoice);
        matrixChoiceLabel.getPreferredSize();
        matrixChoiceLabel.setText("Scoring matrix:");
        matrixChoiceLabel.setSize(new Dimension(120, 15));
        matrixChoiceLabel.setLocation(10, 15);
        matrixChoiceLabel.setVisible(true);
    	scorePanel.add(matrixChoiceLabel);

        // layout substitution scoring matrix
        int score_matrix_left = 15;
        int score_matrix_top = 35;
        int score_left = score_matrix_left + 25;
        int score_top = score_matrix_top + 25;
        int score_w_offset = 40;
        int score_w = score_w_offset - 5;
        int score_h_offset = 25;
        int score_h = score_h_offset - 5;

        int t = score_top;
    	for( int sI = 0; sI < 4; sI++ )
        {
        	int l = score_left;
        	for( int sJ = 0; sJ < 4; sJ++ )
        	{
        		scoreText[sI][sJ] = new JTextField();
                scoreText[sI][sJ].setVisible(true);
                scoreText[sI][sJ].setSize(new java.awt.Dimension(score_w, score_h));
                scoreText[sI][sJ].setLocation(new java.awt.Point(l, t));
                scoreText[sI][sJ].setHorizontalAlignment(JTextField.RIGHT);
                scoreText[sI][sJ].addActionListener(new java.awt.event.ActionListener()
                        {
                            public void actionPerformed(java.awt.event.ActionEvent e)
                            {
                                scoreTextActionPerformed(e);
                            }
                        });
            	scorePanel.add(scoreText[sI][sJ]);
                l += score_w_offset;
        	}
        	t += score_h_offset;
        }
    	setScoreEditable(false);
    	setMatrixValues(hoxd_matrix);
        int score_label_top = score_matrix_top;
        int score_label_left = score_matrix_left;
        t = score_label_top;
        int l = score_label_left;
        for( int sI = 0; sI < 4; sI++ )
        {
        	t += score_h_offset;
        	l += score_w_offset;
        	scoreLabelRow[sI] = new JLabel();
        	scoreLabelRow[sI].setSize(new java.awt.Dimension(20, 20));
        	scoreLabelRow[sI].setLocation(new java.awt.Point(l, score_label_top + 5));
        	scoreLabelRow[sI].setVisible(true);
        	scoreLabelRow[sI].setText(scoreLabels[sI]);
        	scorePanel.add(scoreLabelRow[sI]);
        	scoreLabelCol[sI] = new JLabel();
        	scoreLabelCol[sI].setSize(new java.awt.Dimension(20, 20));
        	scoreLabelCol[sI].setLocation(new java.awt.Point(score_label_left + 10, t));
        	scoreLabelCol[sI].setVisible(true);
        	scoreLabelCol[sI].setText(scoreLabels[sI]);
        	scorePanel.add(scoreLabelCol[sI]);
        }

        gapOpenText.setVisible(true);
        gapOpenText.setSize(new java.awt.Dimension(50, 20));
        gapOpenText.setLocation(new java.awt.Point(150, 160));
        gapOpenText.setText(hoxd_go);
        gapOpenText.setHorizontalAlignment(JTextField.RIGHT);
    	scorePanel.add(gapOpenText);
    	gapOpenLabel.setSize(new java.awt.Dimension(200, 20));
        gapOpenLabel.setLocation(new java.awt.Point(15, 160));
    	gapOpenLabel.setVisible(true);
    	gapOpenLabel.setText("Gap open score:");
    	scorePanel.add(gapOpenLabel);

        gapExtendText.setVisible(true);
        gapExtendText.setSize(new java.awt.Dimension(50, 20));
        gapExtendText.setLocation(new java.awt.Point(150, 185));
        gapExtendText.setText(hoxd_ge);
        gapExtendText.setHorizontalAlignment(JTextField.RIGHT);
    	scorePanel.add(gapExtendText);
    	gapExtendLabel.setSize(new java.awt.Dimension(200, 20));
        gapExtendLabel.setLocation(new java.awt.Point(15, 185));
    	gapExtendLabel.setVisible(true);
    	gapExtendLabel.setText("Gap extend score:");
    	scorePanel.add(gapExtendLabel);

    	// allow use of custom scoring matrices
        alignmentOptionPane.addTab("Scoring", scorePanel);

        // initialize progressiveMauve-specific configuration options
        seedFamiliesCheckBox.setVisible(true);
        seedFamiliesCheckBox.setSize(new java.awt.Dimension(180, 20));
        seedFamiliesCheckBox.setText("Use seed families");
        seedFamiliesCheckBox.setSelected(false);
        seedFamiliesCheckBox.setLocation(new java.awt.Point(10, 30));
        seedFamiliesCheckBox.setToolTipText("<html>Uses multiple spaced seed patterns to identify potential homology.<br>Can substantially improve sensitivity and accuracy on divergent genomes.</html>");

        refineCheckBox.setVisible(true);
        refineCheckBox.setSize(new java.awt.Dimension(180, 20));
        refineCheckBox.setText("Iterative refinement");
        refineCheckBox.setSelected(true);
        refineCheckBox.setLocation(new java.awt.Point(10, 110));
        refineCheckBox.setToolTipText("Iteratively refines the alignment, significantly improving accuracy");

        sumOfPairsCheckBox.setVisible(true);
        sumOfPairsCheckBox.setSize(new java.awt.Dimension(220, 20));
        sumOfPairsCheckBox.setText("Sum-of-pairs LCB scoring");
        sumOfPairsCheckBox.setSelected(true);
        sumOfPairsCheckBox.setLocation(new java.awt.Point(10, 135));
        sumOfPairsCheckBox.setToolTipText("Set to use sum-of-pairs scoring instead of scoring LCBs against an inferred ancestral order");

        breakpointWeightScaleLabel.setSize(new java.awt.Dimension(215, 20));
        breakpointWeightScaleLabel.setVisible(true);
        breakpointWeightScaleLabel.setText("Breakpoint dist. weight scaling:");
        breakpointWeightScaleLabel.setLocation(new java.awt.Point(10, 155));

        breakpointWeightScaleSlider.setMinimum(0);
        breakpointWeightScaleSlider.setMaximum(100);
        breakpointWeightScaleSlider.setValue(50);
        breakpointWeightScaleSlider.setMinorTickSpacing(5);
        breakpointWeightScaleSlider.setMajorTickSpacing(10);
        breakpointWeightScaleSlider.setToolTipText("Set the pairwise breakpoint distance scaling for LCB weight");
        breakpointWeightScaleSlider.setPaintTicks(true);
        breakpointWeightScaleSlider.setPaintLabels(false);
        breakpointWeightScaleSlider.setSnapToTicks(false);
        d = breakpointWeightScaleSlider.getPreferredSize();
//        d.setSize(125, d.getHeight());
        breakpointWeightScaleSlider.setPreferredSize(d);
        breakpointWeightScaleSlider.setMaximumSize(d);
        breakpointWeightScaleSlider.addChangeListener(this);
        breakpointWeightScaleSlider.setLocation(new java.awt.Point(10, 175));
        breakpointWeightScaleSlider.setVisible(true);
        breakpointWeightScaleSlider.setEnabled(true);

        d = breakpointWeightScaleText.getPreferredSize();
        d.setSize(40, d.getHeight());
        breakpointWeightScaleText.setPreferredSize(d);
        breakpointWeightScaleText.setMaximumSize(d);
        breakpointWeightScaleText.setHorizontalAlignment(JTextField.RIGHT);
        breakpointWeightScaleText.setText("0.500");
        breakpointWeightScaleText.setLocation(new java.awt.Point(200, 175));


        conservationWeightScaleLabel.setSize(new java.awt.Dimension(215, 20));
        conservationWeightScaleLabel.setVisible(true);
        conservationWeightScaleLabel.setText("Conservation dist. weight scaling:");
        conservationWeightScaleLabel.setLocation(new java.awt.Point(10, 210));

        conservationWeightScaleSlider.setMinimum(0);
        conservationWeightScaleSlider.setMaximum(100);
        conservationWeightScaleSlider.setValue(50);
        conservationWeightScaleSlider.setMinorTickSpacing(5);
        conservationWeightScaleSlider.setMajorTickSpacing(10);
        conservationWeightScaleSlider.setToolTipText("Set the pairwise conservation distance scaling for LCB weight");
        conservationWeightScaleSlider.setPaintTicks(true);
        conservationWeightScaleSlider.setPaintLabels(false);
        conservationWeightScaleSlider.setSnapToTicks(false);
        d = conservationWeightScaleSlider.getPreferredSize();
        d.setSize(125, d.getHeight());
        conservationWeightScaleSlider.setPreferredSize(d);
        conservationWeightScaleSlider.setMaximumSize(d);
        conservationWeightScaleSlider.addChangeListener(this);
        conservationWeightScaleSlider.setLocation(new java.awt.Point(10, 230));
        conservationWeightScaleSlider.setVisible(true);
        conservationWeightScaleSlider.setEnabled(true);

        d = conservationWeightScaleText.getPreferredSize();
        d.setSize(40, d.getHeight());
        conservationWeightScaleText.setPreferredSize(d);
        conservationWeightScaleText.setMaximumSize(d);
        conservationWeightScaleText.setHorizontalAlignment(JTextField.RIGHT);
        conservationWeightScaleText.setText("0.500");
        conservationWeightScaleText.setLocation(new java.awt.Point(200, 230));

        parameterPanel.add(seedFamiliesCheckBox);
        parameterPanel.add(refineCheckBox);
        parameterPanel.add(sumOfPairsCheckBox);
// this stuff isn't working yet:
/*
        parameterPanel.add(breakpointWeightScaleLabel);
        parameterPanel.add(breakpointWeightScaleSlider);
        parameterPanel.add(breakpointWeightScaleText);

        parameterPanel.add(conservationWeightScaleLabel);
        parameterPanel.add(conservationWeightScaleSlider);
        parameterPanel.add(conservationWeightScaleText);
*/
        // event handling
        determineLCBsCheckBox.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent e)
            {
                determineLCBsCheckBoxActionPerformed(e);
            }
        });

        recursiveCheckBox.addActionListener(new java.awt.event.ActionListener()
                {
                    public void actionPerformed(java.awt.event.ActionEvent e)
                    {
                        recursiveCheckBoxActionPerformed(e);
                    }
                });
        collinearCheckBox.addActionListener(new java.awt.event.ActionListener()
                {
                    public void actionPerformed(java.awt.event.ActionEvent e)
                    {
                    	collinearCheckBoxActionPerformed(e);
                    }
                });
        matrixChoice.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent e)
            {
                matrixChoiceActionPerformed(e);
            }
        });
    }

    File getDefaultFile() throws IOException
    {
        return File.createTempFile("mauve", ".xmfa");
    }

    public void stateChanged(ChangeEvent e)
    {
        if (e.getSource() == breakpointWeightScaleSlider)
        {
            double w = (double)breakpointWeightScaleSlider.getValue();
            Double d = Double.valueOf(w/100);
            breakpointWeightScaleText.setText(d.toString());
        }
        if (e.getSource() == breakpointWeightScaleText)
        {
        }
    }

    protected String[] makeAlignerCommand()
    {
        Vector cmd_vec = new Vector();
        read_filename = null;
        String cur_cmd;
        boolean detect_lcbs = true;

        String pname = getBinaryPath("progressiveMauve");
        cmd_vec.addElement(pname);

        if (getSeedFamilies())
        {
        	cmd_vec.addElement("--seed-family");
        }

        if (getSeedWeight() > 0)
        {
            cur_cmd = "--seed-weight=";
            cur_cmd += Integer.toString(getSeedWeight());
            cmd_vec.addElement(cur_cmd);
        }

        // get a good output file name
        String output_file = getOutput();
        cur_cmd = "--output=";
        cur_cmd += output_file;
        cmd_vec.addElement(cur_cmd);

        read_filename = output_file;

        detect_lcbs = isLCBSearchEnabled();
        if (detect_lcbs)
        {

            if (!getRecursive())
            {
                cmd_vec.addElement("--skip-gapped-alignment");
            }
            if(!getRefine())
            {
            	cmd_vec.addElement("--skip-refinement");
            }
            if(!getSumOfPairs())
            {
            	cmd_vec.addElement("--scoring-scheme=ancestral");
            }

            if( getCollinear() )
            {
            	cmd_vec.addElement("--collinear");
            }

            if (getMinLcbWeight() != -1)
            {
                cur_cmd = "--weight=";
                cur_cmd += Integer.toString(getMinLcbWeight());
                cmd_vec.addElement(cur_cmd);
            }

            // make a guide tree file name
            cur_cmd = "--output-guide-tree=" + output_file + ".guide_tree";
            cmd_vec.addElement(cur_cmd);

            cmd_vec.addElement("--backbone-output=" + output_file + ".backbone");
        }
        else if (!detect_lcbs)
        {
            cur_cmd = "--mums";
            cmd_vec.addElement(cur_cmd);
        }

        if(!(getScoreMatrixName().indexOf("default") > 0))
        {
        	File mat_file = null;
        	String[][] mat = getScoreMatrix();
        	// create a score matrix file
        	try{
        		mat_file = File.createTempFile("scoremat", ".txt");
        		FileWriter outtie = new FileWriter(mat_file);
        		outtie.write("# user-defined scoring matrix\n");
        		for(int i = 0; i < 4; i++)
        		{
					outtie.write("     ");
					outtie.write(scoreLabels[i]);
        		}
				outtie.write("     N");
        		outtie.write("\n");
        		for(int i = 0; i < 4; i++)
        		{
        			for(int j = 0; j < 4; j++)
        			{
        				if(j == 0)
        					outtie.write(scoreLabels[i]);

        				// space pad the score value
        				String space_str = new String();
        				for( int sI = 0; sI < 6 - mat[i][j].length(); ++sI)
        					space_str += " ";
    					outtie.write(space_str);

    					// write the score value
        				outtie.write(mat[i][j]);
        			}
        			outtie.write("     0");	// for the N column
        			outtie.write("\n");
        		}
        		outtie.write("N     0     0     0     0     0");
        		outtie.flush();
        		outtie.close();
        	}catch(IOException ioe)
        	{
        		System.err.println("Error creating score matrix file");
        	}
        	if(mat_file != null)
        	{
        		cmd_vec.addElement("--substitution-matrix=" + mat_file.getAbsolutePath());
        		cmd_vec.addElement("--gap-open=" + getGapOpen());
        		cmd_vec.addElement("--gap-extend=" + getGapExtend());
        	}
        }

        String[] sequences = getSequences();
        for (int seqI = 0; seqI < sequences.length; seqI++)
        {
            cmd_vec.addElement(sequences[seqI]);
            // preemptively delete SMLs to avoid crashes
            File sml_file = new File(sequences[seqI] + ".sslist");
            if(sml_file.exists())  sml_file.delete();
        }

        String[] mauve_cmd = new String[cmd_vec.size()];
        mauve_cmd = (String[]) (cmd_vec.toArray(mauve_cmd));

        return mauve_cmd;
    }

    public void updateEnabledStates()
    {
        if (determineLCBsCheckBox.isSelected())
        {
            refineCheckBox.setEnabled(true);
        }
        else
        {
        	refineCheckBox.setEnabled(false);
        }
    	if(collinearCheckBox.isSelected() || !determineLCBsCheckBox.isSelected())
    	{
        	sumOfPairsCheckBox.setEnabled(false);
    		breakpointWeightScaleText.setEnabled(false);
    		breakpointWeightScaleSlider.setEnabled(false);
    		breakpointWeightScaleLabel.setEnabled(false);
    		conservationWeightScaleText.setEnabled(false);
    		conservationWeightScaleSlider.setEnabled(false);
    		conservationWeightScaleLabel.setEnabled(false);
    	}else if(determineLCBsCheckBox.isSelected())
    	{
        	sumOfPairsCheckBox.setEnabled(true);
    		breakpointWeightScaleText.setEnabled(true);
    		breakpointWeightScaleSlider.setEnabled(true);
    		breakpointWeightScaleLabel.setEnabled(true);
    		conservationWeightScaleText.setEnabled(true);
    		conservationWeightScaleSlider.setEnabled(true);
    		conservationWeightScaleLabel.setEnabled(true);
    	}
    }
    public void determineLCBsCheckBoxActionPerformed(java.awt.event.ActionEvent e)
    {
    	super.determineLCBsCheckBoxActionPerformed(e);
    	updateEnabledStates();
    }

    public void collinearCheckBoxActionPerformed(java.awt.event.ActionEvent e)
    {
    	super.collinearCheckBoxActionPerformed(e);
    	updateEnabledStates();
    }
    public void recursiveCheckBoxActionPerformed(java.awt.event.ActionEvent e)
    {
    	updateEnabledStates();
    }
    public void setMatrixValues(String[][] mat)
    {
    	for(int i = 0; i < 4; i++)
    		for(int j = 0; j < 4; j++)
    			scoreText[i][j].setText(mat[i][j]);
    }
    public void setScoreEditable(boolean edit)
    {
    	gapOpenText.setEditable(edit);
    	gapExtendText.setEditable(edit);
		for(int i = 0; i < 4; i++)
			for(int j = 0; j < 4; j++)
				scoreText[i][j].setEditable(edit);
    }
    public void matrixChoiceActionPerformed(java.awt.event.ActionEvent e)
    {
    	if(matrixChoice.getSelectedIndex() == 0)
    	{
    		setMatrixValues(hoxd_matrix);
    		gapOpenText.setText(hoxd_go);
    		gapExtendText.setText(hoxd_ge);
    		setScoreEditable(false);
    	}
    	if(matrixChoice.getSelectedIndex() == matrixChoice.getItemCount()-1)
    	{
    		// last item is custom matrix
    		setScoreEditable(true);
    	}
    }
    public void scoreTextActionPerformed(java.awt.event.ActionEvent e)
    {
    	matrixChoice.setSelectedIndex(matrixChoice.getItemCount()-1);
    }

    public boolean getSeedFamilies()
    {
    	if(seedFamiliesCheckBox.isEnabled())
    		return seedFamiliesCheckBox.isSelected();
    	return false;
    }

    public boolean getRefine()
    {
    	if(refineCheckBox.isEnabled())
    		return refineCheckBox.isSelected();
    	return false;
    }

    public boolean getSumOfPairs()
    {
    	if(sumOfPairsCheckBox.isEnabled())
    		return sumOfPairsCheckBox.isSelected();
    	return false;
    }

    public String getScoreMatrixName()
    {
    	return matrixChoice.getSelectedItem().toString();
    }
    public String[][] getScoreMatrix()
    {
    	String[][] mat = new String[4][4];
    	for(int i = 0; i < 4; i++)
    		for(int j = 0; j < 4; j++)
    			mat[i][j] = scoreText[i][j].getText();
    	return mat;
    }
    public String getGapOpen()
    {
    	return gapOpenText.getText();
    }
    public String getGapExtend()
    {
    	return gapExtendText.getText();
    }
}
