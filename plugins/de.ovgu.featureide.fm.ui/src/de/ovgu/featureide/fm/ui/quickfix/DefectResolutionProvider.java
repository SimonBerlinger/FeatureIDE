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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ui.IMarkerResolution;

import de.ovgu.featureide.core.IFeatureProject;
import de.ovgu.featureide.fm.core.FeatureModelAnalyzer;
import de.ovgu.featureide.fm.core.Logger;
import de.ovgu.featureide.fm.core.PluginID;
import de.ovgu.featureide.fm.core.analysis.cnf.formula.FeatureModelFormula;
import de.ovgu.featureide.fm.core.base.IConstraint;
import de.ovgu.featureide.fm.core.base.IFeature;
import de.ovgu.featureide.fm.core.explanations.Reason;
import de.ovgu.featureide.fm.core.io.manager.FeatureModelManager;

/**
 * This class provides methods to check, what kind of resolutions can be offered for reasons of a defect-explanation.
 *
 * @author Simon Berlinger
 */
public class DefectResolutionProvider {

	private final FeatureModelFormula featureModelFormula;
	private final IMarker marker;
	private final FeatureModelManager fmManager;
	private final FeatureModelAnalyzer analyzer;
	private final DefectQuickFixHandler quickFixHandler;
	private final IFile modelFile;
	private static final String MODEL_MARKER = PluginID.PLUGIN_ID + ".featureModelMarker";

	public DefectResolutionProvider(FeatureModelFormula featureModelFormula, IMarker marker, FeatureModelManager fmManager, FeatureModelAnalyzer analyzer,
			DefectQuickFixHandler quickFixHandler, IFile modelFile) {
		this.featureModelFormula = featureModelFormula;
		this.marker = marker;
		this.fmManager = fmManager;
		this.analyzer = analyzer;
		this.quickFixHandler = quickFixHandler;
		this.modelFile = modelFile;
	}

	/**
	 * Checks, if a reason consists of a mandatory relation and if so, adds the resolution to {@code offeredResolutions}
	 *
	 * @param reason The reason to check.
	 * @param offeredResolutions The set of resolutions for the current fix.
	 *
	 *
	 * @return The mandatory feature if the reason consists of a feature being mandatory.
	 */
	IFeature checkMandatoryChildReason(Reason<?> reason, Set<IMarkerResolution> offeredResolutions) {

		if ((reason.toNode().getContainedFeatures().size() == 2)) {
			final IFeature featureA = featureModelFormula.getFeatureModel().getFeature(reason.toNode().getContainedFeatures().get(0));
			final IFeature featureB = featureModelFormula.getFeatureModel().getFeature(reason.toNode().getContainedFeatures().get(1));

			if ((featureA.getStructure().getParent() != null) && featureA.getStructure().getParent().getFeature().equals(featureB)) {
				if (featureA.getStructure().isMandatory()) {
					offeredResolutions.add(new ResolutionMakeOptional(marker, featureA.getName(), fmManager));
					return featureA;
				}
			} else if ((featureB.getStructure().getParent() != null) && featureB.getStructure().getParent().getFeature().equals(featureA)) {
				if (featureB.getStructure().isMandatory()) {
					offeredResolutions.add(new ResolutionMakeOptional(marker, featureB.getName(), fmManager));
				}
				return featureB;
			}
		}
		return null;
	}

	/**
	 *
	 * @param reason
	 * @param offeredResolutions
	 * @param possiblyExcluded
	 * @param possibleReason
	 */
	void checkMandatoryExclusionConstraint(Reason<?> reason, Set<IMarkerResolution> offeredResolutions, IFeature possiblyExcluded, IFeature possibleReason) {
		if (possibleReason.getStructure().isMandatory()
			&& (checkForExclusion(possiblyExcluded.getName(), List.of(possibleReason.getName()), reason).size() > 0)) {
			offeredResolutions.add(new ResolutionDeleteConstraint(marker, reason.toNode(), fmManager));

			IConstraint editConstraint = null;

			for (final IConstraint c : featureModelFormula.getFeatureModel().getConstraints()) {
				if (c.getNode().equals(reason.toNode())) {
					editConstraint = c;
				}
			}
			if (editConstraint != null) {
				offeredResolutions.add(new ResolutionEditConstraint(marker, editConstraint, fmManager));
			} else {
				Logger.logWarning("Constraint " + reason.toNode() + " was not found");
			}
		}
	}

	/**
	 * Check, if a feature is excluded by false-optional features and add their fixes to the list.
	 *
	 * @param reasons
	 * @param affectedFeature
	 * @param offeredResolutions
	 */
	void checkForExcludingFalseOptionals(Set<Reason<?>> reasons, IFeature affectedFeature, Set<IMarkerResolution> offeredResolutions) {
		final Set<String> falseOptionals = getExcludingFalseOptionals(reasons, affectedFeature.getName());
		if (falseOptionals.size() > 0) {
			offeredResolutions.add(new ResolutionAwaitOther(marker, affectedFeature.getName(), falseOptionals));
		}
		for (final String featureName : falseOptionals) {
			IMarker[] markers = null;
			try {
				markers = modelFile.findMarkers(MODEL_MARKER, false, 0);
				if ((markers != null) && (markers.length > 0)) {
					for (final IMarker marker : markers) {
						final String message = (String) marker.getAttribute(IMarker.MESSAGE);
						if (message.startsWith(IFeatureProject.MARKER_FALSE_OPTIONAL) && message.split("''")[1].equals(featureName)) {
							// Since the marker corresponds to a feature that may cause another defect and not this defect itself, we add a prefix to clarify
							// this for the user
							marker.setAttribute("LABEL_PREFIX", "[" + featureName + "]: ");
							for (final IMarkerResolution resolution : quickFixHandler.getResolutions(marker)) {
								offeredResolutions.add(resolution);
							}
						}
					}
				}

			} catch (final CoreException e) {
				Logger.logError(e);
			}

		}
	}

	/**
	 * Check, if a feature is involved in both an implication and an exclusion with another feature included in the set of reasons.
	 *
	 * @param reasons
	 * @param affectedFeature
	 * @param offeredResolutions
	 */
	void checkForSimultaneousImplExcl(Set<Reason<?>> reasons, IFeature affectedFeature, Set<IMarkerResolution> offeredResolutions) {

		for (final Reason<?> r : reasons) {

			// System.out.println("Reason " + r + " IMPLYING: " + checkForImplication(affectedFeature.getName(), r.toNode().getContainedFeatures(), r));
		}
	}

	/**
	 * Determines all false-optional features that occur in the reasons for a dead feature and exclude it.
	 *
	 * @param deadFeature
	 * @return The list of all excluding false-optionals.
	 */
	private Set<String> getExcludingFalseOptionals(Set<Reason<?>> reasons, String deadFeature) {

		List<String> falseOptionalCore = new ArrayList<String>(analyzer.getCoreFeatures(null).stream().map(f -> f.getName()).toList());
		final List<String> toRemove = new ArrayList<>();
		for (final String fName : falseOptionalCore) {// remove all features that are not false optional from the list
			if (!analyzer.getFalseOptionalFeatures(null).stream().map(ft -> ft.getName()).toList().contains(fName)) {
				toRemove.add(fName);
			}
		}
		for (final String f : toRemove) {
			falseOptionalCore.remove(f);
		}
		final Set<String> result = new HashSet<>();
		for (final Reason<?> r : reasons) {
			result.addAll(checkForExclusion(deadFeature, falseOptionalCore, r));

		}

		falseOptionalCore = null;
		return result;
	}

	/**
	 *
	 *
	 * Checks, which of the features from {@code possiblyExcluding} exclude the feature {@code excludedFeature} in the formula that {@code r} is based on.
	 *
	 * @param excludedFeature
	 * @param possiblyExcluding
	 * @param r
	 * @return The list of features that would always exclude {@code excludedFeature}, when the formula from {@code r} evaluates to true.
	 */
	private List<String> checkForExclusion(String excludedFeature, List<String> possiblyExcluding, final Reason<?> r) {

		final List<String> excluding = new ArrayList<>();

		if (!r.toNode().getContainedFeatures().contains(excludedFeature)) {
			return excluding;
		}

		boolean featuresContained = false;

		for (final String fName : possiblyExcluding) {
			if (r.toNode().getContainedFeatures().contains(fName)) {
				featuresContained = true;
			}
		}

		if (!featuresContained) {
			return excluding;
		}

		for (final String s : possiblyExcluding) {
			if (r.toNode().getContainedFeatures().contains(s)) {
				excluding.add(s);
			}
		}
		final Set<Map<Object, Boolean>> set = r.toNode().getSatisfyingAssignments();
		for (final Map<Object, Boolean> map : set) {
			if ((map.get(excludedFeature) != null) && map.get(excludedFeature)) {
				for (final Map.Entry<Object, Boolean> entry : map.entrySet()) {
					if (possiblyExcluding.contains(entry.getKey().toString()) && entry.getValue()) {
						excluding.remove(entry.getKey().toString()); // Assignment was found, where feature is not excluded if excludedFeature was //
																	 // selected
					}
				}
			}
		}
		return excluding;
	}

	/**
	 * FIXME: Does not work
	 *
	 * Checks, which of the features from {@code possiblyExcluding} imply the feature {@code excludedFeature} in the formula that {@code r} is based on.
	 *
	 * @param impliedFeature
	 * @param possiblyImplying
	 * @param r
	 * @return The list of features that would always imply {@code excludedFeature}, when the formula from {@code r} evaluates to true.
	 */
	private List<String> checkForImplication(String impliedFeature, List<String> possiblyImplying, final Reason<?> r) {

		final List<String> implying = new ArrayList<>();

		if (!r.toNode().getContainedFeatures().contains(impliedFeature)) {
			return implying;
		}

		boolean featuresContained = false;

		for (final String fName : possiblyImplying) {
			if (r.toNode().getContainedFeatures().contains(fName)) {
				featuresContained = true;
			}
		}

		if (!featuresContained) {
			return implying;
		}

		for (final String s : possiblyImplying) {
			if (r.toNode().getContainedFeatures().contains(s)) {
				implying.add(s);
			}
		}
		final Set<Map<Object, Boolean>> set = r.toNode().getSatisfyingAssignments();
		for (final Map<Object, Boolean> map : set) {
			if ((map.get(impliedFeature) != null) && !map.get(impliedFeature)) {
				for (final Map.Entry<Object, Boolean> entry : map.entrySet()) {
					if (possiblyImplying.contains(entry.getKey().toString()) && entry.getValue()) {
						implying.remove(entry.getKey().toString()); // Assignment was found, where the implication would be violated if impliedFeature was //
																	 // selected
					}
				}
			}
		}
		return implying;
	}

}
