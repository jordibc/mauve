package org.gel.mauve.format;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;

import org.biojava.bio.seq.SequenceIterator;
import org.biojava.bio.seq.io.SeqIOTools;
import org.biojavax.bio.seq.RichSequence;

class EmblFormat extends GenbankEmblFormat {
	public SequenceIterator readFile (File file) {
		BufferedReader reader;
		try {
			reader = new BufferedReader (new FileReader (file));
		} catch (FileNotFoundException e) {
			// This exception is not expected, because file is required to
			// exist.
			throw new RuntimeException (e);
		}
		return SeqIOTools.readEmbl (reader);
	}

    public boolean isRich(){ return false; }
}