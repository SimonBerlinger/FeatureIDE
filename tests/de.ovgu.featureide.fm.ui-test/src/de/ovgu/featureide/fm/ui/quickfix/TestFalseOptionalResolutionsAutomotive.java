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
 * This class tests the generation of false-optional resolutions for the {@code Automotive01} feature model.
 *
 * @author Simon Berlinger
 */
public class TestFalseOptionalResolutionsAutomotive extends AbstractResolutionTest {

	@Test
	public void testImpliedByMandatoryAutomotiveA() {
		analyzeFeatureModel("automotive01_defects.xml", FeatureStatus.FALSE_OPTIONAL, "FO_IMPLY_OPTIONAL_FALSE-OPTIONAL", "testImpliedByMandatoryAutomotiveA");

		getFalseOptionalResolutions("FO_IMPLY_OPTIONAL_FALSE-OPTIONAL");

		assertTrue(resolutions.contains(new ResolutionDeleteConstraint(
				new Implies(new Literal("FO_IMPLY_OPTIONAL_IMPLYING"), new Literal("FO_IMPLY_OPTIONAL_FALSE-OPTIONAL")), fmManager)));
		assertTrue(resolutions.contains(new ResolutionEditConstraint(
				getConstraintForNode(new Implies(new Literal("FO_IMPLY_OPTIONAL_IMPLYING"), new Literal("FO_IMPLY_OPTIONAL_FALSE-OPTIONAL"))), fmManager, "")));
		assertTrue(resolutions.contains(new ResolutionMakeOptional(fmManager, featureModel.getFeature("FO_IMPLY_OPTIONAL_IMPLYING"), "")));
		assertTrue(resolutions.contains(new ResolutionMakeMandatory(fmManager, featureModel.getFeature("FO_IMPLY_OPTIONAL_FALSE-OPTIONAL"),
				featureModel.getFeature("N_100000__F_100001"), "")));
	}

	@Test
	public void testExcludeOtherOptionsAutomotiveA() {
		analyzeFeatureModel("automotive01_defects.xml", FeatureStatus.FALSE_OPTIONAL, "FO_EXCLUDE_OTHER_OPTIONS_FALSE-OPTIONAL",
				"testExcludeOtherOptionsAutomotiveA");

		getFalseOptionalResolutions("FO_EXCLUDE_OTHER_OPTIONS_FALSE-OPTIONAL");

		assertTrue(resolutions.contains(new ResolutionMakeOptional(fmManager, featureModel.getFeature("FO_EXCLUDE_OTHER_OPTIONS_EXCLUDING"), "")));
		assertTrue(resolutions.contains(new ResolutionDeleteConstraint(
				new Implies(new Literal("FO_EXCLUDE_OTHER_OPTIONS_EXCLUDING"), new Not("FO_EXCLUDE_OTHER_OPTIONS_EXCLUDED1")), fmManager)));
		assertTrue(resolutions.contains(new ResolutionDeleteConstraint(
				new Implies(new Literal("FO_EXCLUDE_OTHER_OPTIONS_EXCLUDING"), new Not("FO_EXCLUDE_OTHER_OPTIONS_EXCLUDED2")), fmManager)));
		assertTrue(resolutions.contains(new ResolutionEditConstraint(
				getConstraintForNode(new Implies(new Literal("FO_EXCLUDE_OTHER_OPTIONS_EXCLUDING"), new Not("FO_EXCLUDE_OTHER_OPTIONS_EXCLUDED1"))), fmManager,
				"")));
		assertTrue(resolutions.contains(new ResolutionEditConstraint(
				getConstraintForNode(new Implies(new Literal("FO_EXCLUDE_OTHER_OPTIONS_EXCLUDING"), new Not("FO_EXCLUDE_OTHER_OPTIONS_EXCLUDED2"))), fmManager,
				"")));
	}

	@Test
	public void testImplicationChainAutomotiveA() {
		analyzeFeatureModel("automotive01_defects.xml", FeatureStatus.FALSE_OPTIONAL, "FO_IMPLICATION_CHAIN_FALSE-OPTIONAL", "testImplicationChainAutomotiveA");

		getFalseOptionalResolutions("FO_IMPLICATION_CHAIN_FALSE-OPTIONAL");

		assertTrue(resolutions.contains(new ResolutionMakeOptional(fmManager, featureModel.getFeature("FO_IMPLICATION_CHAIN_IMPLYING-MANDATORY"), "")));
		assertTrue(resolutions.contains(new ResolutionMakeMandatory(fmManager, featureModel.getFeature("FO_IMPLICATION_CHAIN_IMPLIED"),
				featureModel.getFeature("N_100300__F_100301"), "")));
		assertTrue(resolutions.contains(new ResolutionDeleteConstraint(
				new Implies(new Literal("FO_IMPLICATION_CHAIN_IMPLYING-MANDATORY"), new Literal("FO_IMPLICATION_CHAIN_IMPLIED")), fmManager)));
		assertTrue(resolutions.contains(new ResolutionEditConstraint(
				getConstraintForNode(new Implies(new Literal("FO_IMPLICATION_CHAIN_IMPLYING-MANDATORY"), new Literal("FO_IMPLICATION_CHAIN_IMPLIED"))),
				fmManager, "")));
	}

	@Test
	public void testImpliedByMandatoryAutomotiveB() {
		analyzeFeatureModel("automotive01_defects.xml", FeatureStatus.FALSE_OPTIONAL, "FO_IMPLIED_OPTIONAL_FALSE-OPTIONAL_B",
				"testImpliedByMandatoryAutomotiveB");

		getFalseOptionalResolutions("FO_IMPLIED_OPTIONAL_FALSE-OPTIONAL_B");

		assertTrue(resolutions.contains(new ResolutionDeleteConstraint(
				new Implies(new Literal("FO_IMPLIED_OPTIONAL_IMPLYING_B"), new Literal("FO_IMPLIED_OPTIONAL_FALSE-OPTIONAL_B")), fmManager)));
		assertTrue(resolutions.contains(new ResolutionEditConstraint(
				getConstraintForNode(new Implies(new Literal("FO_IMPLIED_OPTIONAL_IMPLYING_B"), new Literal("FO_IMPLIED_OPTIONAL_FALSE-OPTIONAL_B"))),
				fmManager, "")));
		assertTrue(resolutions.contains(new ResolutionMakeOptional(fmManager, featureModel.getFeature("FO_IMPLIED_OPTIONAL_IMPLYING_B"), "")));
		assertTrue(resolutions.contains(new ResolutionMakeMandatory(fmManager, featureModel.getFeature("FO_IMPLIED_OPTIONAL_FALSE-OPTIONAL_B"),
				featureModel.getFeature("N_100000__F_100001"), "")));
	}

	@Test
	public void testExcludeOtherOptionsAutomotiveB() {
		analyzeFeatureModel("automotive01_defects.xml", FeatureStatus.FALSE_OPTIONAL, "FO_EXCLUDE_OTHER_OPTIONS_FALSE-OPTIONAL_B",
				"testExcludeOtherOptionsAutomotiveB");

		getFalseOptionalResolutions("FO_EXCLUDE_OTHER_OPTIONS_FALSE-OPTIONAL_B");

		assertTrue(resolutions.contains(new ResolutionMakeOptional(fmManager, featureModel.getFeature("FO_EXCLUDE_OTHER_OPTIONS_EXCLUDING1_B"), "")));
		assertTrue(resolutions.contains(new ResolutionMakeOptional(fmManager, featureModel.getFeature("FO_EXCLUDE_OTHER_OPTIONS_EXCLUDING2_B"), "")));
		assertTrue(resolutions.contains(new ResolutionDeleteConstraint(
				new Implies(new Literal("FO_EXCLUDE_OTHER_OPTIONS_EXCLUDING1_B"), new Not("FO_EXCLUDE_OTHER_OPTIONS_EXCLUDED1_B")), fmManager)));
		assertTrue(resolutions.contains(new ResolutionDeleteConstraint(
				new Implies(new Literal("FO_EXCLUDE_OTHER_OPTIONS_EXCLUDING2_B"), new Not("FO_EXCLUDE_OTHER_OPTIONS_EXCLUDED2_B")), fmManager)));
		assertTrue(resolutions.contains(new ResolutionEditConstraint(
				getConstraintForNode(new Implies(new Literal("FO_EXCLUDE_OTHER_OPTIONS_EXCLUDING1_B"), new Not("FO_EXCLUDE_OTHER_OPTIONS_EXCLUDED1_B"))),
				fmManager, "")));
		assertTrue(resolutions.contains(new ResolutionEditConstraint(
				getConstraintForNode(new Implies(new Literal("FO_EXCLUDE_OTHER_OPTIONS_EXCLUDING2_B"), new Not("FO_EXCLUDE_OTHER_OPTIONS_EXCLUDED2_B"))),
				fmManager, "")));
	}

	@Test
	public void testImplicationChainAutomotiveB() {
		analyzeFeatureModel("automotive01_defects.xml", FeatureStatus.FALSE_OPTIONAL, "FO_IMPLICATION_CHAIN_FALSE-OPTIONAL_B",
				"testImplicationChainAutomotiveB");

		getFalseOptionalResolutions("FO_IMPLICATION_CHAIN_FALSE-OPTIONAL_B");

		assertTrue(resolutions.contains(new ResolutionMakeOptional(fmManager, featureModel.getFeature("FO_IMPLICATION_CHAIN_IMPLYING-MANDATORY_B"), "")));
		assertTrue(resolutions.contains(new ResolutionMakeMandatory(fmManager, featureModel.getFeature("FO_IMPLICATION_CHAIN_IMPLIED_B"),
				featureModel.getFeature("N_100000__F_100467"), "")));
		assertTrue(resolutions.contains(new ResolutionDeleteConstraint(
				new Implies(new Literal("FO_IMPLICATION_CHAIN_IMPLYING-MANDATORY_B"), new Literal("FO_IMPLICATION_CHAIN_IMPLIED_B")), fmManager)));
		assertTrue(resolutions.contains(new ResolutionEditConstraint(
				getConstraintForNode(new Implies(new Literal("FO_IMPLICATION_CHAIN_IMPLYING-MANDATORY_B"), new Literal("FO_IMPLICATION_CHAIN_IMPLIED_B"))),
				fmManager, "")));
	}
}
