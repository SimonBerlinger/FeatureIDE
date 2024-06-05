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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.prop4j.And;
import org.prop4j.Implies;
import org.prop4j.Literal;
import org.prop4j.Not;

import de.ovgu.featureide.fm.core.analysis.FeatureProperties.FeatureStatus;

/**
 * This class tests the generation of dead feature resolutions for the {@code Automotive01} feature model.
 *
 * @author Simon Berlinger
 */
public class TestDeadFeatureResolutionsAutomotive extends AbstractResolutionTest {

	// Feature names: The first part indicates the type of defect, the last part indicates the role of the feature and the middle part is the
	// situation of the defect. For example: DF_EXCL_OPT_EXCLUDING is the excluding feature for a dead feature in the situation where an optional is excluded.
	// For the second set of tests, '_B' is appended

	@Test
	public void testExcludedOptionalDeadAutomotiveA() {

		analyzeFeatureModel("automotive01_defects.xml", FeatureStatus.DEAD, "DF_EXCL_OPT_DEAD", "testExcludedOptionalDeadAutomotiveA");

		getDeadFeatureResolutions("DF_EXCL_OPT_DEAD");

		assertTrue(resolutions.contains(new ResolutionMakeOptional(fmManager, featureModel.getFeature("DF_EXCL_OPT_EXCLUDING"), null)));
		assertTrue(resolutions.contains(
				new ResolutionDeleteConstraint(new Implies(new Literal("DF_EXCL_OPT_EXCLUDING"), new Not(new Literal("DF_EXCL_OPT_DEAD"))), fmManager)));
		assertTrue(resolutions.contains(new ResolutionEditConstraint(
				getConstraintForNode(new Implies(new Literal("DF_EXCL_OPT_EXCLUDING"), new Not(new Literal("DF_EXCL_OPT_DEAD")))), fmManager, "")));
	}

	@Test
	public void testAlternativeImplicationDeadAutomotiveA() {

		analyzeFeatureModel("automotive01_defects.xml", FeatureStatus.DEAD, "DF_ALT_IMPLICATION_DEAD", "testAlternativeImplicationDeadAutomotiveA");

		getDeadFeatureResolutions("DF_ALT_IMPLICATION_DEAD");

		assertTrue(resolutions.contains(new ResolutionMakeOptional(fmManager, featureModel.getFeature("DF_ALT_IMPLICATION_IMPLYING"), null)));
		assertTrue(resolutions.contains(
				new ResolutionDeleteConstraint(new Implies(new Literal("DF_ALT_IMPLICATION_IMPLYING"), new Literal("DF_ALT_IMPLICATION_IMPLIED")), fmManager)));
		assertTrue(resolutions.contains(new ResolutionEditConstraint(
				getConstraintForNode(new Implies(new Literal("DF_ALT_IMPLICATION_IMPLYING"), new Literal("DF_ALT_IMPLICATION_IMPLIED"))), fmManager, "")));
		assertTrue(resolutions.contains(new ResolutionConvertAlternativeToOr(fmManager, featureModel.getFeature("DF_EXCL_OPT_EXCLUDING"), "")));
	}

	@Test
	public void testAltImplyAltAutomotiveA() {

		analyzeFeatureModel("automotive01_defects.xml", FeatureStatus.DEAD, "DF_ALT_IMPLY_ALT_DEAD", "testAltImplyAltAutomotiveA");

		getDeadFeatureResolutions("DF_ALT_IMPLY_ALT_DEAD");

		assertTrue(resolutions.contains(
				new ResolutionDeleteConstraint(new Implies(new Literal("DF_ALT_IMPLY_ALT_DEAD"), new Literal("DF_ALT_IMPLY_ALT_IMPLIED")), fmManager)));
		assertTrue(resolutions.contains(new ResolutionConvertAlternativeToOr(fmManager, featureModel.getFeature("DF_ALT_IMPLY_ALT_PARENT"), "")));

		// Exclusion results from the alternative group and cannot be deleted
		assertFalse(resolutions
				.contains(new ResolutionDeleteConstraint(new Implies(new Literal("DF_ALT_IMPLY_ALT_IMPLIED"), new Not("DF_ALT_IMPLY_ALT_DEAD")), fmManager)));
	}

	@Test
	public void testImplyMultiAltAutomotiveA() {

		analyzeFeatureModel("automotive01_defects.xml", FeatureStatus.DEAD, "DF_IMPLY_MULTI_ALT_DEAD", "testImplyMultiAltAutomotiveA");

		getDeadFeatureResolutions("DF_IMPLY_MULTI_ALT_DEAD");

		assertTrue(resolutions.contains(new ResolutionDeleteConstraint(new Implies(new Literal("DF_IMPLY_MULTI_ALT_DEAD"),
				new And(new Literal("DF_IMPLY_MULTI_ALT_IMPLIED"), new Literal("DF_IMPLY_MULTI_ALT_IMPLIED2"))), fmManager)));
		assertTrue(
				resolutions
						.contains(
								new ResolutionEditConstraint(
										getConstraintForNode(new Implies(new Literal("DF_IMPLY_MULTI_ALT_DEAD"),
												new And(new Literal("DF_IMPLY_MULTI_ALT_IMPLIED"), new Literal("DF_IMPLY_MULTI_ALT_IMPLIED2")))),
										fmManager, "")));

	}

	@Test
	public void testSimultaneousImplyExcludeAutomotiveA() {
		analyzeFeatureModel("automotive01_defects.xml", FeatureStatus.DEAD, "DF_SIMULTANEOUS_IMPLY_EXCL_DEAD", "testSimultaneousImplyExcludeAutomotiveA");

		getDeadFeatureResolutions("DF_SIMULTANEOUS_IMPLY_EXCL_DEAD");

		assertTrue(resolutions.contains(new ResolutionDeleteConstraint(
				new Implies(new Literal("DF_SIMULTANEOUS_IMPLY_EXCL_DEAD"), new Literal("DF_SIMULTANEOUS_IMPLY_EXCL_IMPLIED")), fmManager)));
		assertTrue(resolutions.contains(new ResolutionDeleteConstraint(
				new Implies(new Literal("DF_SIMULTANEOUS_IMPLY_EXCL_IMPLIED"), new Not("DF_SIMULTANEOUS_IMPLY_EXCL_DEAD")), fmManager)));
		assertTrue(resolutions.contains(new ResolutionEditConstraint(
				getConstraintForNode(new Implies(new Literal("DF_SIMULTANEOUS_IMPLY_EXCL_DEAD"), new Literal("DF_SIMULTANEOUS_IMPLY_EXCL_IMPLIED"))), fmManager,
				"")));
		assertTrue(resolutions.contains(new ResolutionEditConstraint(
				getConstraintForNode(new Implies(new Literal("DF_SIMULTANEOUS_IMPLY_EXCL_IMPLIED"), new Not("DF_SIMULTANEOUS_IMPLY_EXCL_DEAD"))), fmManager,
				"")));
	}

	@Test
	public void testExcludeParentAutomotiveA() {
		analyzeFeatureModel("automotive01_defects.xml", FeatureStatus.DEAD, "DF_EXCL_PARENT_DEAD", "testExcludeParentAutomotiveA");

		getDeadFeatureResolutions("DF_EXCL_PARENT_DEAD");

		assertTrue(resolutions
				.contains(new ResolutionDeleteConstraint(new Implies(new Literal("DF_EXCL_PARENT_DEAD"), new Literal("DF_EXCL_PARENT_EXCLUDING")), fmManager)));
		assertTrue(resolutions
				.contains(new ResolutionDeleteConstraint(new Implies(new Literal("DF_EXCL_PARENT_EXCLUDING"), new Not("DF_EXCL_PARENT_PARENT")), fmManager)));
		assertTrue(resolutions.contains(new ResolutionEditConstraint(
				getConstraintForNode(new Implies(new Literal("DF_EXCL_PARENT_DEAD"), new Literal("DF_EXCL_PARENT_EXCLUDING"))), fmManager, "")));
		assertTrue(resolutions.contains(new ResolutionEditConstraint(
				getConstraintForNode(new Implies(new Literal("DF_EXCL_PARENT_EXCLUDING"), new Not("DF_EXCL_PARENT_PARENT"))), fmManager, "")));

	}

	@Test
	public void testDeadFeatureImplicationChainAutomotiveA() {
		analyzeFeatureModel("automotive01_defects.xml", FeatureStatus.DEAD, "DF_DEAD_IMPLY_CHAIN_DEAD", "testDeadFeatureImplicationChainAutomotiveA");

		getDeadFeatureResolutions("DF_DEAD_IMPLY_CHAIN_DEAD");

		assertTrue(resolutions.contains(
				new ResolutionDeleteConstraint(new Implies(new Literal("DF_DEAD_IMPLY_CHAIN_DEAD"), new Literal("DF_DEAD_IMPLY_CHAIN_IMPLIED1")), fmManager)));
		assertTrue(resolutions.contains(
				new ResolutionDeleteConstraint(new Implies(new Literal("DF_DEAD_IMPLY_CHAIN_EXCLUDING"), new Not("DF_DEAD_IMPLY_CHAIN_IMPLIED2")), fmManager)));

		assertTrue(resolutions.contains(new ResolutionEditConstraint(
				getConstraintForNode(new Implies(new Literal("DF_DEAD_IMPLY_CHAIN_DEAD"), new Literal("DF_DEAD_IMPLY_CHAIN_IMPLIED1"))), fmManager, "")));
		assertTrue(resolutions.contains(new ResolutionEditConstraint(
				getConstraintForNode(new Implies(new Literal("DF_DEAD_IMPLY_CHAIN_EXCLUDING"), new Not("DF_DEAD_IMPLY_CHAIN_IMPLIED2"))), fmManager, "")));

		assertTrue(resolutions.contains(new ResolutionMakeOptional(fmManager, featureModel.getFeature("DF_DEAD_IMPLY_CHAIN_EXCLUDING"), "")));
	}

	@Test
	public void testExcludedByFalseOptionalAutomotiveA() {
		analyzeFeatureModel("automotive01_defects.xml", FeatureStatus.DEAD, "DF_EXCL_BY_FALSE_OPT_DEAD", "testExcludedByFalseOptionalAutomotiveA");

		getDeadFeatureResolutions("DF_EXCL_BY_FALSE_OPT_DEAD");

		assertTrue(resolutions.contains(new ResolutionDeleteConstraint(
				new Implies(new Literal("DF_EXCL_BY_FALSE_OPT_IMPLYING"), new Literal("DF_EXCL_BY_FALSE_OPT_FALSE-OPTIONAL")), fmManager)));
		assertTrue(resolutions.contains(new ResolutionEditConstraint(
				getConstraintForNode(new Implies(new Literal("DF_EXCL_BY_FALSE_OPT_IMPLYING"), new Literal("DF_EXCL_BY_FALSE_OPT_FALSE-OPTIONAL"))), fmManager,
				"")));
		assertTrue(resolutions.contains(new ResolutionMakeOptional(fmManager, featureModel.getFeature("DF_EXCL_BY_FALSE_OPT_IMPLYING"), "")));
		assertTrue(resolutions.contains(new ResolutionMakeMandatory(fmManager, featureModel.getFeature("DF_EXCL_BY_FALSE_OPT_FALSE-OPTIONAL"),
				featureModel.getFeature("DF_EXCL_BY_FALSE_OPT_FO-PARENT"), "")));
	}

	@Test
	public void testExcludedOptionalDeadAutomotiveB() {

		analyzeFeatureModel("automotive01_defects.xml", FeatureStatus.DEAD, "DF_EXCLUDED_OPTIONAL_DEAD_B", "testExcludedOptionalDeadAutomotiveB");

		getDeadFeatureResolutions("DF_EXCLUDED_OPTIONAL_DEAD_B");

		assertTrue(resolutions.contains(new ResolutionMakeOptional(fmManager, featureModel.getFeature("DF_EXCLUDED_OPTIONAL_EXCLUDING_B"), null)));
		assertTrue(resolutions.contains(new ResolutionDeleteConstraint(
				new Implies(new Literal("DF_EXCLUDED_OPTIONAL_EXCLUDING_B"), new Not(new Literal("DF_EXCLUDED_OPTIONAL_DEAD_B"))), fmManager)));
		assertTrue(resolutions.contains(new ResolutionEditConstraint(
				getConstraintForNode(new Implies(new Literal("DF_EXCLUDED_OPTIONAL_EXCLUDING_B"), new Not(new Literal("DF_EXCLUDED_OPTIONAL_DEAD_B")))),
				fmManager, "")));
	}

	@Test
	public void testAlternativeImplicationDeadAutomotiveB() {

		analyzeFeatureModel("automotive01_defects.xml", FeatureStatus.DEAD, "DF_ALT_IMPLY_DEAD_B", "testAlternativeImplicationDeadAutomotiveB");

		getDeadFeatureResolutions("DF_ALT_IMPLY_DEAD_B");

		assertTrue(resolutions.contains(new ResolutionMakeOptional(fmManager, featureModel.getFeature("DF_ALT_IMPLY_IMPLYING_B"), null)));
		assertTrue(resolutions.contains(
				new ResolutionDeleteConstraint(new Implies(new Literal("DF_ALT_IMPLY_IMPLYING_B"), new Literal("DF_ALT_IMPLY_IMPLIED_B")), fmManager)));
		assertTrue(resolutions.contains(new ResolutionEditConstraint(
				getConstraintForNode(new Implies(new Literal("DF_ALT_IMPLY_IMPLYING_B"), new Literal("DF_ALT_IMPLY_IMPLIED_B"))), fmManager, "")));
		assertTrue(resolutions.contains(new ResolutionConvertAlternativeToOr(fmManager, featureModel.getFeature("DF_ALT_IMPLY_PARENT_B"), "")));
	}

	@Test
	public void testAltImplyAltAutomotiveB() {
		analyzeFeatureModel("automotive01_defects.xml", FeatureStatus.DEAD, "DF_ALT_IMPLY_ALT_DEAD_B", "testAltImplyAltAutomotiveB");

		getDeadFeatureResolutions("DF_ALT_IMPLY_ALT_DEAD_B");

		assertTrue(resolutions.contains(
				new ResolutionDeleteConstraint(new Implies(new Literal("DF_ALT_IMPLY_ALT_DEAD_B"), new Literal("DF_ALT_IMPLY_ALT_IMPLIED_B")), fmManager)));
		assertTrue(resolutions.contains(new ResolutionConvertAlternativeToOr(fmManager, featureModel.getFeature("DF_ALT_IMPLY_ALT_PARENT_B"), "")));

		// Exclusion results from the alternative group and cannot be deleted
		assertFalse(resolutions.contains(
				new ResolutionDeleteConstraint(new Implies(new Literal("DF_ALT_IMPLY_ALT_IMPLIED_B"), new Not("DF_ALT_IMPLY_ALT_DEAD_B")), fmManager)));
	}

	@Test
	public void testImplyMultiAltAutomotiveB() {

		analyzeFeatureModel("automotive01_defects.xml", FeatureStatus.DEAD, "DF_IMPLY_MULTI_ALT_DEAD_B", "testImplyMultiAltAutomotiveB");

		getDeadFeatureResolutions("DF_IMPLY_MULTI_ALT_DEAD_B");

		assertTrue(resolutions.contains(new ResolutionDeleteConstraint(new Implies(new Literal("DF_IMPLY_MULTI_ALT_DEAD_B"),
				new And(new Literal("DF_IMPLY_MULTI_ALT_IMPLIED_B"), new Literal("DF_IMPLY_MULTI_ALT_IMPLIED2_B"))), fmManager)));
		assertTrue(
				resolutions
						.contains(
								new ResolutionEditConstraint(
										getConstraintForNode(new Implies(new Literal("DF_IMPLY_MULTI_ALT_DEAD_B"),
												new And(new Literal("DF_IMPLY_MULTI_ALT_IMPLIED_B"), new Literal("DF_IMPLY_MULTI_ALT_IMPLIED2_B")))),
										fmManager, "")));

	}

	@Test
	public void testSimultaneousImplyExcludeAutomotiveB() {
		analyzeFeatureModel("automotive01_defects.xml", FeatureStatus.DEAD, "DF_SIMULTANEOUS_IMPLY_EXCL_DEAD_B", "testSimultaneousImplyExcludeAutomotiveB");

		getDeadFeatureResolutions("DF_SIMULTANEOUS_IMPLY_EXCL_DEAD_B");

		assertTrue(resolutions.contains(new ResolutionDeleteConstraint(
				new Implies(new Literal("DF_SIMULTANEOUS_IMPLY_EXCL_DEAD_B"), new Literal("DF_SIMULTANEOUS_IMPLY_EXCL_IMPLIED_B")), fmManager)));
		assertTrue(resolutions.contains(new ResolutionDeleteConstraint(
				new Implies(new Literal("DF_SIMULTANEOUS_IMPLY_EXCL_EXCLUDING_B"), new Not("DF_SIMULTANEOUS_IMPLY_EXCL_DEAD_B")), fmManager)));
		assertTrue(resolutions.contains(new ResolutionEditConstraint(
				getConstraintForNode(new Implies(new Literal("DF_SIMULTANEOUS_IMPLY_EXCL_DEAD_B"), new Literal("DF_SIMULTANEOUS_IMPLY_EXCL_IMPLIED_B"))),
				fmManager, "")));
		assertTrue(resolutions.contains(new ResolutionEditConstraint(
				getConstraintForNode(new Implies(new Literal("DF_SIMULTANEOUS_IMPLY_EXCL_EXCLUDING_B"), new Not("DF_SIMULTANEOUS_IMPLY_EXCL_DEAD_B"))),
				fmManager, "")));
	}

	@Test
	public void testExcludeParentAutomotiveB() {
		analyzeFeatureModel("automotive01_defects.xml", FeatureStatus.DEAD, "DF_EXCLUDE_PARENT_DEAD_B", "testExcludeParentAutomotiveB");

		getDeadFeatureResolutions("DF_EXCLUDE_PARENT_DEAD_B");

		assertTrue(resolutions.contains(
				new ResolutionDeleteConstraint(new Implies(new Literal("DF_EXCLUDE_PARENT_DEAD_B"), new Literal("DF_EXCLUDE_PARENT_EXCLUDING_B")), fmManager)));
		assertTrue(resolutions.contains(
				new ResolutionDeleteConstraint(new Implies(new Literal("DF_EXCLUDE_PARENT_EXCLUDING_B"), new Not("DF_EXCLUDE_PARENT_PARENT_B")), fmManager)));
		assertTrue(resolutions.contains(new ResolutionEditConstraint(
				getConstraintForNode(new Implies(new Literal("DF_EXCLUDE_PARENT_DEAD_B"), new Literal("DF_EXCLUDE_PARENT_EXCLUDING_B"))), fmManager, "")));
		assertTrue(resolutions.contains(new ResolutionEditConstraint(
				getConstraintForNode(new Implies(new Literal("DF_EXCLUDE_PARENT_EXCLUDING_B"), new Not("DF_EXCLUDE_PARENT_PARENT_B"))), fmManager, "")));

	}

	@Test
	public void testDeadFeatureImplicationChainAutomotiveB() {
		analyzeFeatureModel("automotive01_defects.xml", FeatureStatus.DEAD, "DF_DEAD_IMPLY_CHAIN_DEAD_B", "testDeadFeatureImplicationChainAutomotiveB");

		getDeadFeatureResolutions("DF_DEAD_IMPLY_CHAIN_DEAD_B");

		assertTrue(resolutions.contains(new ResolutionDeleteConstraint(
				new Implies(new Literal("DF_DEAD_IMPLY_CHAIN_DEAD_B"), new Literal("DF_DEAD_IMPLY_CHAIN_IMPLIED1_B")), fmManager)));
		assertTrue(resolutions.contains(new ResolutionDeleteConstraint(
				new Implies(new Literal("DF_DEAD_IMPLY_CHAIN_EXCLUDING_B"), new Not("DF_DEAD_IMPLY_CHAIN_EXCLUDED_B")), fmManager)));

		assertTrue(resolutions.contains(new ResolutionEditConstraint(
				getConstraintForNode(new Implies(new Literal("DF_DEAD_IMPLY_CHAIN_DEAD_B"), new Literal("DF_DEAD_IMPLY_CHAIN_IMPLIED1_B"))), fmManager, "")));
		assertTrue(resolutions.contains(new ResolutionEditConstraint(
				getConstraintForNode(new Implies(new Literal("DF_DEAD_IMPLY_CHAIN_EXCLUDING_B"), new Not("DF_DEAD_IMPLY_CHAIN_EXCLUDED_B"))), fmManager, "")));

		assertTrue(resolutions.contains(new ResolutionMakeOptional(fmManager, featureModel.getFeature("DF_DEAD_IMPLY_CHAIN_EXCLUDING_B"), "")));
	}

	@Test
	public void testExcludedByFalseOptionalAutomotiveB() {
		analyzeFeatureModel("automotive01_defects.xml", FeatureStatus.DEAD, "DF_EXCLUDED_BY_FALSE_OPT_DEAD_B", "testExcludedByFalseOptionalAutomotiveB");

		getDeadFeatureResolutions("DF_EXCLUDED_BY_FALSE_OPT_DEAD_B");

		assertTrue(resolutions.contains(new ResolutionDeleteConstraint(
				new Implies(new Literal("DF_EXCLUDED_BY_FALSE_OPT_FALSE-OPTIONAL_IMPLYING_B"), new Literal("DF_EXCLUDED_BY_FALSE_OPT_FALSE-OPTIONAL_B")),
				fmManager)));
		assertTrue(resolutions.contains(new ResolutionEditConstraint(getConstraintForNode(
				new Implies(new Literal("DF_EXCLUDED_BY_FALSE_OPT_FALSE-OPTIONAL_IMPLYING_B"), new Literal("DF_EXCLUDED_BY_FALSE_OPT_FALSE-OPTIONAL_B"))),
				fmManager, "")));
		assertTrue(
				resolutions.contains(new ResolutionMakeOptional(fmManager, featureModel.getFeature("DF_EXCLUDED_BY_FALSE_OPT_FALSE-OPTIONAL_IMPLYING_B"), "")));

		assertFalse(resolutions.contains(new ResolutionMakeMandatory(fmManager, featureModel.getFeature("DF_EXCLUDED_BY_FALSE_OPT_FALSE-OPTIONAL_B"),
				featureModel.getFeature("DF_EXCLUDED_BY_FALSE_OPT_FO-PARENT_B"), "")));
	}

	@Test
	public void testDeactivatedFeatureAutomotive() {
		analyzeFeatureModel("automotive01_defects.xml", FeatureStatus.DEAD, "DF_DEACTIVATED_DEAD", "testDeactivatedFeatureAutomotive");

		getDeadFeatureResolutions("DF_DEACTIVATED_DEAD");

		assertTrue(resolutions.size() == 1);
		assertTrue(resolutions.contains(new ResolutionDeleteConstraint(new Not("DF_DEACTIVATED_DEAD"), fmManager)));
	}

	@Test
	public void testDeactivateExcludeSelfAutomotive() {

		analyzeFeatureModel("automotive01_defects.xml", FeatureStatus.DEAD, "DF_EXCLUDE_SELF_DEACTIVATED", "testDeactivateExcludeSelfAutomotive");
		getDeadFeatureResolutions("DF_EXCLUDE_SELF_DEACTIVATED");
		assertTrue(resolutions.contains(new ResolutionReplaceConstraint(fmManager,
				new Implies(new Literal("DF_EXCLUDE_SELF_DEACTIVATED"), new Not("DF_EXCLUDE_SELF_DEACTIVATED")), new Not("DF_EXCLUDE_SELF_DEACTIVATED"), "")));
	}

	@Test
	public void testDeactivateExcludeRootAutomotive() {
		analyzeFeatureModel("automotive01_defects.xml", FeatureStatus.DEAD, "DF_EXCLUDE_ROOT_DEACTIVATED", "testDeactivateExcludeRootAutomotive");
		getDeadFeatureResolutions("DF_EXCLUDE_ROOT_DEACTIVATED");
		assertTrue(resolutions.contains(new ResolutionReplaceConstraint(fmManager,
				new Implies(new Literal("DF_EXCLUDE_ROOT_DEACTIVATED"), new Not("N_100000__F_100001")), new Not("DF_EXCLUDE_ROOT_DEACTIVATED"), "")));
	}

	@Test
	public void testDeactivateExcludedByRootAutomotive() {
		analyzeFeatureModel("automotive01_defects.xml", FeatureStatus.DEAD, "DF_EXCLUDED_BY_ROOT_DEACTIVATED", "testDeactivateExcludedByRootAutomotive");
		getDeadFeatureResolutions("DF_EXCLUDED_BY_ROOT_DEACTIVATED");
		assertTrue(resolutions.contains(new ResolutionReplaceConstraint(fmManager,
				new Implies(new Literal("N_100000__F_100001"), new Not("DF_EXCLUDED_BY_ROOT_DEACTIVATED")), new Not("DF_EXCLUDED_BY_ROOT_DEACTIVATED"), "")));
	}

	@Test
	public void testDeactivateExcludedByDeactivatedAutomotive() {
		analyzeFeatureModel("automotive01_defects.xml", FeatureStatus.DEAD, "DF_IMPLY_DEACTIVATED_DEAD", "testDeactivateExcludedByDeactivatedAutomotive");
		getDeadFeatureResolutions("DF_IMPLY_DEACTIVATED_DEAD");
		assertTrue(resolutions.stream().anyMatch(x -> (x instanceof ResolutionCreateConstraint)
			&& ((ResolutionCreateConstraint) x).toCreateNode.toString().equals("-DF_IMPLY_DEACTIVATED_DEAD")));
	}
}
