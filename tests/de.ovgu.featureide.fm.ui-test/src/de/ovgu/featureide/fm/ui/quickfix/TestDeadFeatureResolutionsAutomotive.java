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
 * TODO description
 *
 * @author Simon Berlinger
 */
public class TestDeadFeatureResolutionsAutomotive extends AbstractResolutionTest {

	@Test
	public void testExcludedOptionalDeadAutomotiveA() {

		analyzeFeatureModel("automotive01_defects.xml", FeatureStatus.DEAD, "N_100002__F_100004", "testExcludedOptionalDeadAutomotiveA");

		getDeadFeatureResolutions("N_100002__F_100004");

		assertTrue(resolutions.contains(new ResolutionMakeOptional(fmManager, featureModel.getFeature("N_100002__F_100106"), null)));
		assertTrue(resolutions.contains(
				new ResolutionDeleteConstraint(new Implies(new Literal("N_100002__F_100106"), new Not(new Literal("N_100002__F_100004"))), fmManager)));
		assertTrue(resolutions.contains(new ResolutionEditConstraint(
				getConstraintForNode(new Implies(new Literal("N_100002__F_100106"), new Not(new Literal("N_100002__F_100004")))), fmManager, "")));
	}

	@Test
	public void testAlternativeImplicationDeadAutomotiveA() {

		analyzeFeatureModel("automotive01_defects.xml", FeatureStatus.DEAD, "N_100002__F_100108", "testAlternativeImplicationDeadAutomotiveA");

		getDeadFeatureResolutions("N_100002__F_100108");

		assertTrue(resolutions.contains(new ResolutionMakeOptional(fmManager, featureModel.getFeature("N_100002__F_100012"), null)));
		assertTrue(resolutions
				.contains(new ResolutionDeleteConstraint(new Implies(new Literal("N_100002__F_100012"), new Literal("N_100002__F_100107")), fmManager)));
		assertTrue(resolutions.contains(new ResolutionEditConstraint(
				getConstraintForNode(new Implies(new Literal("N_100002__F_100012"), new Literal("N_100002__F_100107"))), fmManager, "")));
		assertTrue(resolutions.contains(new ResolutionConvertAlternativeToOr(fmManager, featureModel.getFeature("N_100002__F_100106"), "")));
	}

	@Test
	public void testAltImplyAltAutomotive() {

		analyzeFeatureModel("automotive01_defects.xml", FeatureStatus.DEAD, "N_100002__F_100114", "testAltImplyAltAutomotiveA");

		getDeadFeatureResolutions("N_100002__F_100114");

		assertTrue(resolutions
				.contains(new ResolutionDeleteConstraint(new Implies(new Literal("N_100002__F_100114"), new Literal("N_100002__F_100129")), fmManager)));
		assertTrue(resolutions.contains(new ResolutionConvertAlternativeToOr(fmManager, featureModel.getFeature("N_100002__F_100113"), "")));

		// Exclusion results from the alternative group and cannot be deleted
		assertFalse(
				resolutions.contains(new ResolutionDeleteConstraint(new Implies(new Literal("N_100002__F_100129"), new Not("N_100002__F_100114")), fmManager)));
	}

	@Test
	public void testImplyMultiAltAutomotive() {

		analyzeFeatureModel("automotive01_defects.xml", FeatureStatus.DEAD, "N_100002__F_100110", "testImplyMultiAltAutomotiveA");

		getDeadFeatureResolutions("N_100002__F_100110");

		assertTrue(resolutions.contains(new ResolutionDeleteConstraint(
				new Implies(new Literal("N_100002__F_100110"), new And(new Literal("N_100002__F_100108"), new Literal("N_100002__F_100109"))), fmManager)));
		assertTrue(resolutions.contains(new ResolutionEditConstraint(
				getConstraintForNode(
						new Implies(new Literal("N_100002__F_100110"), new And(new Literal("N_100002__F_100108"), new Literal("N_100002__F_100109")))),
				fmManager, "")));

	}

	@Test
	public void testSimultaneousImplyExcludeAutomotiveA() {
		analyzeFeatureModel("automotive01_defects.xml", FeatureStatus.DEAD, "N_100002__F_100073", "testSimultaneousImplyExcludeAutomotiveA");

		getDeadFeatureResolutions("N_100002__F_100073");

		assertTrue(resolutions
				.contains(new ResolutionDeleteConstraint(new Implies(new Literal("N_100002__F_100073"), new Literal("N_100002__F_100104")), fmManager)));
		assertTrue(
				resolutions.contains(new ResolutionDeleteConstraint(new Implies(new Literal("N_100002__F_100104"), new Not("N_100002__F_100073")), fmManager)));
		assertTrue(resolutions.contains(new ResolutionEditConstraint(
				getConstraintForNode(new Implies(new Literal("N_100002__F_100073"), new Literal("N_100002__F_100104"))), fmManager, "")));
		assertTrue(resolutions.contains(new ResolutionEditConstraint(
				getConstraintForNode(new Implies(new Literal("N_100002__F_100104"), new Not("N_100002__F_100073"))), fmManager, "")));
	}

	@Test
	public void testExcludeParentAutomotiveA() {
		analyzeFeatureModel("automotive01_defects.xml", FeatureStatus.DEAD, "N_100002__F_100081", "testExcludeParentAutomotiveA");

		getDeadFeatureResolutions("N_100002__F_100081");

		assertTrue(resolutions
				.contains(new ResolutionDeleteConstraint(new Implies(new Literal("N_100002__F_100081"), new Literal("N_100002__F_100087")), fmManager)));
		assertTrue(
				resolutions.contains(new ResolutionDeleteConstraint(new Implies(new Literal("N_100002__F_100087"), new Not("N_100002__F_100080")), fmManager)));
		assertTrue(resolutions.contains(new ResolutionEditConstraint(
				getConstraintForNode(new Implies(new Literal("N_100002__F_100081"), new Literal("N_100002__F_100087"))), fmManager, "")));
		assertTrue(resolutions.contains(new ResolutionEditConstraint(
				getConstraintForNode(new Implies(new Literal("N_100002__F_100087"), new Not("N_100002__F_100080"))), fmManager, "")));

	}

	@Test
	public void testDeadFeatureImplicationChainAutomotiveA() {
		analyzeFeatureModel("automotive01_defects.xml", FeatureStatus.DEAD, "N_104649__F_104756", "testDeadFeatureImplicationChainAutomotiveA");

		getDeadFeatureResolutions("N_104649__F_104756");

		assertTrue(resolutions
				.contains(new ResolutionDeleteConstraint(new Implies(new Literal("N_104649__F_104756"), new Literal("N_104649__F_104750")), fmManager)));
		assertTrue(
				resolutions.contains(new ResolutionDeleteConstraint(new Implies(new Literal("N_104700__F_104701"), new Not("N_104649__F_104727")), fmManager)));

		assertTrue(resolutions.contains(new ResolutionEditConstraint(
				getConstraintForNode(new Implies(new Literal("N_104649__F_104756"), new Literal("N_104649__F_104750"))), fmManager, "")));
		assertTrue(resolutions.contains(new ResolutionEditConstraint(
				getConstraintForNode(new Implies(new Literal("N_104700__F_104701"), new Not("N_104649__F_104727"))), fmManager, "")));

		assertTrue(resolutions.contains(new ResolutionMakeOptional(fmManager, featureModel.getFeature("N_104700__F_104701"), "")));
	}

	@Test
	public void testExcludedByFalseOptionalAutomotiveA() {
		analyzeFeatureModel("automotive01_defects.xml", FeatureStatus.DEAD, "N_100130__F_100265", "testExcludedByFalseOptionalAutomotiveA");

		getDeadFeatureResolutions("N_100130__F_100265");

		assertTrue(resolutions
				.contains(new ResolutionDeleteConstraint(new Implies(new Literal("N_100130__F_100133"), new Literal("N_100130__F_100170")), fmManager)));
		assertTrue(resolutions.contains(new ResolutionEditConstraint(
				getConstraintForNode(new Implies(new Literal("N_100130__F_100133"), new Literal("N_100130__F_100170"))), fmManager, "")));
		assertTrue(resolutions.contains(new ResolutionMakeOptional(fmManager, featureModel.getFeature("N_100130__F_100133"), "")));

		assertFalse(resolutions.contains(new ResolutionMakeMandatory(fmManager, "N_100130__F_100170", "N_100130__F_100132", "")));
	}

	@Test
	public void testExcludedOptionalDeadAutomotiveB() {

		analyzeFeatureModel("automotive01_defects.xml", FeatureStatus.DEAD, "N_100353__F_100392", "testExcludedOptionalDeadAutomotiveB");

		getDeadFeatureResolutions("N_100353__F_100392");

		assertTrue(resolutions.contains(new ResolutionMakeOptional(fmManager, featureModel.getFeature("N_102385__F_102406"), null)));
		assertTrue(resolutions.contains(
				new ResolutionDeleteConstraint(new Implies(new Literal("N_102385__F_102406"), new Not(new Literal("N_100353__F_100392"))), fmManager)));
		assertTrue(resolutions.contains(new ResolutionEditConstraint(
				getConstraintForNode(new Implies(new Literal("N_102385__F_102406"), new Not(new Literal("N_100353__F_100392")))), fmManager, "")));
	}

	@Test
	public void testAlternativeImplicationDeadAutomotiveB() {

		analyzeFeatureModel("automotive01_defects.xml", FeatureStatus.DEAD, "N_100469__I_100473_i_F_100477", "testAlternativeImplicationDeadAutomotiveB");

		getDeadFeatureResolutions("N_100469__I_100473_i_F_100477");

		assertTrue(resolutions.contains(new ResolutionMakeOptional(fmManager, featureModel.getFeature("N_102383__F_102501"), null)));
		assertTrue(resolutions.contains(
				new ResolutionDeleteConstraint(new Implies(new Literal("N_102383__F_102501"), new Literal("N_100469__I_100473_i_F_100474")), fmManager)));
		assertTrue(resolutions.contains(new ResolutionEditConstraint(
				getConstraintForNode(new Implies(new Literal("N_102383__F_102501"), new Literal("N_100469__I_100473_i_F_100474"))), fmManager, "")));
		assertTrue(resolutions.contains(new ResolutionConvertAlternativeToOr(fmManager, featureModel.getFeature("N_100469__I_100473_i_F_100471"), "")));
	}

	@Test
	public void testSimultaneousImplyExcludeAutomotiveB() {
		analyzeFeatureModel("automotive01_defects.xml", FeatureStatus.DEAD, "N_102383__I_103792_i_F_103796", "testSimultaneousImplyExcludeAutomotiveB");

		getDeadFeatureResolutions("N_102383__I_103792_i_F_103796");

		assertTrue(resolutions.contains(
				new ResolutionDeleteConstraint(new Implies(new Literal("N_102383__I_103792_i_F_103796"), new Literal("N_100576__F_100577")), fmManager)));
		assertTrue(resolutions
				.contains(new ResolutionDeleteConstraint(new Implies(new Literal("N_102383__F_102789"), new Not("N_102383__I_103792_i_F_103796")), fmManager)));
		assertTrue(resolutions.contains(new ResolutionEditConstraint(
				getConstraintForNode(new Implies(new Literal("N_102383__I_103792_i_F_103796"), new Literal("N_100576__F_100577"))), fmManager, "")));
		assertTrue(resolutions.contains(new ResolutionEditConstraint(
				getConstraintForNode(new Implies(new Literal("N_102383__F_102789"), new Not("N_102383__I_103792_i_F_103796"))), fmManager, "")));
	}

	@Test
	public void testExcludeParentAutomotiveB() {
		analyzeFeatureModel("automotive01_defects.xml", FeatureStatus.DEAD, "N_102383__I_103792_i_F_103835", "testExcludeParentAutomotiveB");

		getDeadFeatureResolutions("N_102383__I_103792_i_F_103835");

		assertTrue(resolutions.contains(
				new ResolutionDeleteConstraint(new Implies(new Literal("N_102383__I_103792_i_F_103835"), new Literal("N_100000__F_104854")), fmManager)));
		assertTrue(
				resolutions.contains(new ResolutionDeleteConstraint(new Implies(new Literal("N_100000__F_104854"), new Not("N_102383__F_102384")), fmManager)));
		assertTrue(resolutions.contains(new ResolutionEditConstraint(
				getConstraintForNode(new Implies(new Literal("N_102383__I_103792_i_F_103835"), new Literal("N_100000__F_104854"))), fmManager, "")));
		assertTrue(resolutions.contains(new ResolutionEditConstraint(
				getConstraintForNode(new Implies(new Literal("N_100000__F_104854"), new Not("N_102383__F_102384"))), fmManager, "")));

	}

	@Test
	public void testDeadFeatureImplicationChainAutomotiveB() {
		analyzeFeatureModel("automotive01_defects.xml", FeatureStatus.DEAD, "N_100000__F_104355", "testDeadFeatureImplicationChainAutomotiveB");

		getDeadFeatureResolutions("N_100000__F_104355");

		System.out.println(resolutions);

		assertTrue(resolutions
				.contains(new ResolutionDeleteConstraint(new Implies(new Literal("N_100000__F_104355"), new Literal("N_100353__F_100456")), fmManager)));
		assertTrue(resolutions
				.contains(new ResolutionDeleteConstraint(new Implies(new Literal("N_100002__F_100014_xor"), new Not("N_100002__F_100020")), fmManager)));

		assertTrue(resolutions.contains(new ResolutionEditConstraint(
				getConstraintForNode(new Implies(new Literal("N_100000__F_104355"), new Literal("N_100353__F_100456"))), fmManager, "")));
		assertTrue(resolutions.contains(new ResolutionEditConstraint(
				getConstraintForNode(new Implies(new Literal("N_100002__F_100014_xor"), new Not("N_100002__F_100020"))), fmManager, "")));

		assertTrue(resolutions.contains(new ResolutionMakeOptional(fmManager, featureModel.getFeature("N_100002__F_100014_xor"), "")));
	}

	@Test
	public void testExcludedByFalseOptionalAutomotiveB() {
		analyzeFeatureModel("automotive01_defects.xml", FeatureStatus.DEAD, "N_102383__I_103300_i_F_103340", "testExcludedByFalseOptionalAutomotiveB");

		getDeadFeatureResolutions("N_102383__I_103300_i_F_103340");

		assertTrue(resolutions
				.contains(new ResolutionDeleteConstraint(new Implies(new Literal("N_100002__F_100106"), new Literal("N_104642__F_104647")), fmManager)));
		assertTrue(resolutions.contains(new ResolutionEditConstraint(
				getConstraintForNode(new Implies(new Literal("N_100002__F_100106"), new Literal("N_104642__F_104647"))), fmManager, "")));
		assertTrue(resolutions.contains(new ResolutionMakeOptional(fmManager, featureModel.getFeature("N_100002__F_100106"), "")));

		assertFalse(resolutions.contains(new ResolutionMakeMandatory(fmManager, "N_104642__F_104647", "N_104642__F_104643", "")));
	}

	public void testDeactivatedFeatureAutomotive() {
		analyzeFeatureModel("automotive01_defects.xml", FeatureStatus.DEAD, "N_104282__F_104283", "testDeactivatedFeatureAutomotive");

		getDeadFeatureResolutions("N_104282__F_104283");

		assertTrue(resolutions.size() == 1);
		assertTrue(resolutions.contains(new ResolutionDeleteConstraint(new Not("N_104282__F_104283"), fmManager)));
	}

	public void testConvertToDeactivatedFeatureAutomotive() {
		analyzeFeatureModel("automotive01_defects.xml", FeatureStatus.DEAD, "N_104282__F_104283", "testConvertToDeactivatedFeatureAutomotive");
		getDeadFeatureResolutions("N_104282__F_104283");
		assertTrue(resolutions.contains(new ResolutionCreateConstraint(new Not("N_104282__F_104283"), fmManager)));

		analyzeFeatureModel("automotive01_defects.xml", FeatureStatus.DEAD, "N_100002__F_100021", "");
		getDeadFeatureResolutions("N_100002__F_100021");
		assertTrue(resolutions.contains(new ResolutionCreateConstraint(new Not("N_100002__F_100021"), fmManager)));

		analyzeFeatureModel("automotive01_defects.xml", FeatureStatus.DEAD, "N_100002__F_100022", "");
		getDeadFeatureResolutions("N_100002__F_100022");
		assertTrue(resolutions.contains(new ResolutionCreateConstraint(new Not("N_100002__F_100022"), fmManager)));

		analyzeFeatureModel("automotive01_defects.xml", FeatureStatus.DEAD, "N_100002__F_100023", "");
		getDeadFeatureResolutions("N_100002__F_100023");
		assertTrue(resolutions.contains(new ResolutionCreateConstraint(new Not("N_100002__F_100023"), fmManager)));

		analyzeFeatureModel("automotive01_defects.xml", FeatureStatus.DEAD, "N_100002__F_100015", "");
		getDeadFeatureResolutions("N_100002__F_100015");
		assertTrue(resolutions.contains(new ResolutionCreateConstraint(new Not("N_100002__F_100015"), fmManager)));

	}
}
