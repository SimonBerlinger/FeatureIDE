/* FeatureIDE - A Framework for Feature-Oriented Software Development
 * Copyright (C) 2005-2017  FeatureIDE team, University of Magdeburg, Germany
 *
 * This file is part of FeatureIDE.
 *
 * FeatureIDE is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FeatureIDE is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with FeatureIDE.  If not, see <http://www.gnu.org/licenses/>.
 *
 * See http://featureide.cs.ovgu.de/ for further information.
 */
package de.ovgu.featureide.fm.ui.quickfix;

import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.prop4j.Implies;
import org.prop4j.Literal;
import org.prop4j.Not;

import de.ovgu.featureide.fm.core.analysis.FeatureProperties.FeatureStatus;

/**
 * This class tests the generation of false-optional resolutions for the {@code HelloWorld} feature models.
 *
 * @author Simon Berlinger
 */
public class TestFalseOptionalResolutions extends AbstractResolutionTest {

	@Test
	public void testImpliedByMandatory() {
		analyzeFeatureModel("false_optional_imply_optional.xml", FeatureStatus.FALSE_OPTIONAL, "Adjective", "testImpliedByMandatory");

		getFalseOptionalResolutions("Adjective");

		assertTrue(resolutions.contains(new ResolutionDeleteConstraint(new Implies(new Literal("Hello"), new Literal("Adjective")), fmManager)));
		assertTrue(resolutions
				.contains(new ResolutionEditConstraint(getConstraintForNode(new Implies(new Literal("Hello"), new Literal("Adjective"))), fmManager, "")));
		assertTrue(resolutions.contains(new ResolutionMakeOptional(fmManager, featureModel.getFeature("Hello"), "")));
		assertTrue(resolutions.contains(new ResolutionMakeMandatory(fmManager, featureModel.getFeature("Adjective"), featureModel.getFeature("Sentence"), "")));
	}

	@Test
	public void testExcludeOtherOptions() {
		analyzeFeatureModel("false_optional_exclude_alternatives.xml", FeatureStatus.FALSE_OPTIONAL, "Period", "testExcludeOtherOptions");

		getFalseOptionalResolutions("Period");

		assertTrue(resolutions.contains(new ResolutionMakeOptional(fmManager, featureModel.getFeature("Hello"), "")));
		assertTrue(resolutions.contains(new ResolutionDeleteConstraint(new Implies(new Literal("Hello"), new Not("Exclamation Mark")), fmManager)));
		assertTrue(resolutions.contains(new ResolutionDeleteConstraint(new Implies(new Literal("Hello"), new Not("Question Mark")), fmManager)));
		assertTrue(resolutions
				.contains(new ResolutionEditConstraint(getConstraintForNode(new Implies(new Literal("Hello"), new Not("Exclamation Mark"))), fmManager, "")));
		assertTrue(resolutions
				.contains(new ResolutionEditConstraint(getConstraintForNode(new Implies(new Literal("Hello"), new Not("Question Mark"))), fmManager, "")));
	}

	@Test
	public void testImplicationChain() {
		analyzeFeatureModel("false_optional_implication_chain.xml", FeatureStatus.FALSE_OPTIONAL, "Period", "testImplicationChain");

		getFalseOptionalResolutions("Period");

		assertTrue(resolutions.contains(new ResolutionMakeOptional(fmManager, featureModel.getFeature("Hello"), "")));
		assertTrue(resolutions.contains(new ResolutionDeleteConstraint(new Implies(new Literal("Hello"), new Literal("Beautiful")), fmManager)));
		assertTrue(resolutions
				.contains(new ResolutionEditConstraint(getConstraintForNode(new Implies(new Literal("Hello"), new Literal("Beautiful"))), fmManager, "")));
	}
}
