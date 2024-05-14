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

import java.util.HashSet;
import java.util.Set;

import org.eclipse.ui.IMarkerResolution;
import org.prop4j.Node;

import de.ovgu.featureide.Commons;
import de.ovgu.featureide.fm.core.FeatureModelAnalyzer;
import de.ovgu.featureide.fm.core.analysis.FeatureProperties.FeatureStatus;
import de.ovgu.featureide.fm.core.base.IConstraint;
import de.ovgu.featureide.fm.core.base.IFeatureModel;
import de.ovgu.featureide.fm.core.explanations.Reason;
import de.ovgu.featureide.fm.core.io.manager.FeatureModelManager;

/**
 * TODO description
 *
 * @author Simon Berlinger
 */
public class AbstractResolutionTest {

	FeatureModelManager fmManager;
	IFeatureModel featureModel;
	FeatureModelAnalyzer analyzer;
	Set<Reason<?>> reasons;
	DefectResolutionProvider resolutionProvider;
	Set<IMarkerResolution> resolutions;
	DefectQuickFixHandler dqh;

	protected void analyzeFeatureModel(String filename, FeatureStatus defectType, String defectFeatureName, String title) {
		System.out.println("BEGIN: " + title);
		loadFeatureModelIfNotPresent(filename);
		featureModel = fmManager.getPersistentFormula().getFeatureModel();
		analyzer = fmManager.getPersistentFormula().getAnalyzer();
		analyzer.analyzeFeatureModel(null);

		switch (defectType) {
		case DEAD:
			reasons = analyzer.getDeadFeatureExplanation(featureModel.getFeature(defectFeatureName)).getReasons();
			break;
		case FALSE_OPTIONAL:
			reasons = analyzer.getFalseOptionalFeatureExplanation(featureModel.getFeature(defectFeatureName)).getReasons();
			System.out.println("FO: " + analyzer.getFalseOptionalFeatureExplanation(featureModel.getFeature(defectFeatureName)));
			break;
		default:
			return;
		}

		resolutions = new HashSet<>();
		dqh = new DefectQuickFixHandler();
		dqh.analyzer = analyzer;
		dqh.featureModelFormula = fmManager.getPersistentFormula();
	}

	protected void analyzeFeatureModelRedundancy(String filename, Node constraintNode, String title) {
		System.out.println("BEGIN: " + title);
		loadFeatureModelIfNotPresent(filename);
		featureModel = fmManager.getPersistentFormula().getFeatureModel();
		analyzer = fmManager.getPersistentFormula().getAnalyzer();
		analyzer.analyzeFeatureModel(null);

		reasons = analyzer.getRedundantConstraintExplanation(getConstraintForNode(constraintNode)).getReasons();

		resolutions = new HashSet<>();
		dqh = new DefectQuickFixHandler();
		dqh.analyzer = analyzer;
		dqh.featureModelFormula = fmManager.getPersistentFormula();
	}

	/**
	 * @param filename
	 */
	protected void loadFeatureModelIfNotPresent(String filename) {
		if ((fmManager == null) || !fmManager.getPath().endsWith(filename)) {
			fmManager = Commons.loadTestFeatureModelFromFile(filename);
		}
	}

	protected IConstraint getConstraintForNode(Node node) {
		return (IConstraint) featureModel.getConstraints().stream().filter(x -> x.getNode().equals(node)).toArray()[0];
	}

	protected void getDeadFeatureResolutions(String featureName) {
		final long t0 = System.nanoTime();
		resolutionProvider =
			new DefectResolutionProvider(fmManager.getPersistentFormula().getFeatureModel(), fmManager, fmManager.getPersistentFormula().getAnalyzer(), dqh);
		dqh.setOriginalDefectName(featureName);
		dqh.getDeadFeatureResolutions(resolutionProvider, resolutions, featureModel.getFeature(featureName));
		System.out.println("    END - Duration: " + ((System.nanoTime() - t0) / 1000000.0) + " ms\n" + resolutions + "\n");

	}

	protected void getFalseOptionalResolutions(String featureName) {
		final long t0 = System.nanoTime();
		resolutionProvider =
			new DefectResolutionProvider(fmManager.getPersistentFormula().getFeatureModel(), fmManager, fmManager.getPersistentFormula().getAnalyzer(), dqh);
		dqh.setOriginalDefectName(featureName);
		dqh.getFalseOptionalResolutions(resolutionProvider, resolutions, featureModel.getFeature(featureName));
		System.out.println("    END - Duration: " + ((System.nanoTime() - t0) / 1000000.0) + " ms\n " + resolutions + "\n");

	}

	protected void getRedundancyResolutions(Node constraintNode) {
		final long t0 = System.nanoTime();
		resolutionProvider =
			new DefectResolutionProvider(fmManager.getPersistentFormula().getFeatureModel(), fmManager, fmManager.getPersistentFormula().getAnalyzer(), dqh);
		dqh.getRedundancyResolutions(resolutionProvider, resolutions,
				(IConstraint) featureModel.getConstraints().stream().filter(x -> x.getNode().equals(constraintNode)).toList().get(0));
		System.out.println("    END - Duration: " + ((System.nanoTime() - t0) / 1000000.0) + " ms\n" + resolutions + "\n");

	}

}
