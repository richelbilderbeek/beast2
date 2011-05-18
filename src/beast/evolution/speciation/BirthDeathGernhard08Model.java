/*
 * BirthDeathGernhard08Model.java
 *
 * Copyright (C) 2002-2009 Alexei Drummond and Andrew Rambaut
 *
 * This file is part of BEAST.
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership and licensing.
 *
 * BEAST is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * BEAST is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with BEAST; if not, write to the
 * Free Software Foundation, Inc., 51 Franklin St, Fifth Floor,
 * Boston, MA  02110-1301  USA
 */

package beast.evolution.speciation;


import java.util.Arrays;

import beast.core.Citation;
import beast.core.Description;
import beast.core.Input;
import beast.core.Input.Validate;
import beast.core.parameter.RealParameter;
import beast.evolution.tree.Tree;

import static org.apache.commons.math.special.Gamma.logGamma;

/* Ported from Beast 1.6
 * @author Joseph Heled
 *         Date: 24/02/2008
 */
@Description("Birth Death model based on Gernhard 2008. <br/>" +
		"This derivation conditions directly on fixed N taxa. <br/>"+
		"The inference is directly on b-d (strictly positive) and d/b (constrained in [0,1)) <br/>"+
		"Vefified using simulated trees generated by Klass tree sample. (http://www.klaashartmann.com/treesample/) <br/>"+
		"Sampling proportion not verified via simulation. Proportion set by default to 1, an assignment which makes "+
 		"the expressions identical to the expressions before the change.")

@Citation(value="Gernhard 2008. The conditioned reconstructed process. Journal of Theoretical Biology Volume 253, " +
		"Issue 4, 21 August 2008, Pages 769-778",
		DOI="doi:10.1016/j.jtbi.2008.04.005")// (http://dx.doi.org/10.1016/j.jtbi.2008.04.005)

public class BirthDeathGernhard08Model extends YuleModel {
	
	final static String [] TYPES = {"unscaled", "timesonly", "oriented", "labeled"};
	
	public Input<RealParameter> relativeDeathRateParameter =
            new Input<RealParameter>("relativeDeathRate", "relative death rate parameter, mu/lambda in birth death model", Validate.REQUIRED);
    public Input<RealParameter> sampleProbability =
            new Input<RealParameter>("sampleProbability", "sample probability, rho in birth/death model");
    public Input<String> m_pType =
            new Input<String>("type", "tree type, should be one of " + Arrays.toString(TYPES)+" (default unscaled)",
                    "unscaled", TYPES);
    
    @Override
    public void initAndValidate() throws Exception {
    	super.initAndValidate();
    	String sType = m_pType.get().toLowerCase();
    	if (sType.equals("unscaled")) {
    		type = TreeType.UNSCALED;
    	} else if (sType.equals("timesonly")) {
    		type = TreeType.TIMESONLY;
    	} else if (sType.equals("oriented")) {
    		type = TreeType.ORIENTED;
    	} else if (sType.equals("labeled")) {
    		type = TreeType.LABELED;
    	} else {
    		throw new Exception("type '" + sType +"' is not recognized. Should be one of unscaled, timesonly, oriented and labeled.");
    	}
    }
    
    @Override
    public double calculateTreeLogLikelihood(Tree tree) {
        final int taxonCount = tree.getNodeCount()/2;
        final double r = birthDiffRateParameter.get().getValue();
        final double rho = relativeDeathRateParameter.get().getValue();
        final double a = (sampleProbability.get() == null ? 0 : sampleProbability.get().getValue());
        
        double logL = logTreeProbability(taxonCount, r, rho, a);

        logL += logNodeProbability(tree.getRoot(), r, rho, a, taxonCount);
        
        return logL;
    }

    private TreeType type;
    public enum TreeType {
        UNSCALED,     // no coefficient 
        TIMESONLY,    // n!
        ORIENTED,     // n
        LABELED,      // 2^(n-1)/(n-1)!  (conditional on root: 2^(n-1)/n!(n-1) )
    }

    /** scaling coefficient of tree **/
    protected double logCoeff(int taxonCount) {
        switch( type ) {
            case UNSCALED: break;
            case TIMESONLY: return logGamma(taxonCount + 1);
            case ORIENTED:  return Math.log(taxonCount);
            case LABELED:   {
                final double two2nm1 = (taxonCount - 1) * Math.log(2.0);
                if( ! conditionalOnRoot ) {
                    return two2nm1 - logGamma(taxonCount);
                } else {
                    return two2nm1 - Math.log(taxonCount-1) - logGamma(taxonCount+1);
                }
            }
        }
        return 0.0;
    }
    
    @Override
    protected boolean requiresRecalculation() {
    	return super.requiresRecalculation() || relativeDeathRateParameter.get().somethingIsDirty() || sampleProbability.get().somethingIsDirty();
    }
} // class BirthDeathGernhard08Model
