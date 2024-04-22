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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ui.IMarkerResolution;
import org.prop4j.And;
import org.prop4j.Node;

import de.ovgu.featureide.core.IFeatureProject;
import de.ovgu.featureide.fm.core.FeatureModelAnalyzer;
import de.ovgu.featureide.fm.core.Logger;
import de.ovgu.featureide.fm.core.PluginID;
import de.ovgu.featureide.fm.core.analysis.cnf.formula.FeatureModelFormula;
import de.ovgu.featureide.fm.core.base.IConstraint;
import de.ovgu.featureide.fm.core.base.IFeature;
import de.ovgu.featureide.fm.core.editing.FeatureModelToNodeTraceModel;
import de.ovgu.featureide.fm.core.editing.FeatureModelToNodeTraceModel.Origin;
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
	IFeature checkMandatoryChildReason(Reason<?> reason, Set<IMarkerResolution> offeredResolutions, String featurePrefix) {

		if ((reason.toNode().getContainedFeatures().size() == 2)) {
			final IFeature featureA = featureModelFormula.getFeatureModel().getFeature(reason.toNode().getContainedFeatures().get(0));
			final IFeature featureB = featureModelFormula.getFeatureModel().getFeature(reason.toNode().getContainedFeatures().get(1));

			if ((featureA.getStructure().getParent() != null) && featureA.getStructure().getParent().getFeature().equals(featureB)) {
				if (featureA.getStructure().isMandatory()) {
					offeredResolutions.add(new ResolutionMakeOptional(marker, featureA.getName(), fmManager, featurePrefix));
					return featureA;
				}
			} else if ((featureB.getStructure().getParent() != null) && featureB.getStructure().getParent().getFeature().equals(featureA)) {
				if (featureB.getStructure().isMandatory()) {
					offeredResolutions.add(new ResolutionMakeOptional(marker, featureB.getName(), fmManager, featurePrefix));
				}
				return featureB;
			}
		}
		return null;
	}

	/**
	 * Checks, if the reasons contain a constraint, where {@code possibleReason} excludes {@code possiblyExcluded}
	 *
	 * @param reason
	 * @param offeredResolutions
	 * @param possiblyExcluded
	 * @param possibleReason
	 *
	 * @return true, if possiblyExcluded is excluded by possibleReason
	 */
	boolean checkExclusion(Set<Reason<?>> reasons, Set<IMarkerResolution> offeredResolutions, String possiblyExcluded, String possibleReason,
			String featurePrefix) {

		for (final Reason<?> reason : reasons) {
			if (checkForExclusion(possiblyExcluded, List.of(possibleReason), reason.toNode()).size() > 0) {

				System.out.println("EXCLUSION SUBJECT: " + reason.getSubject().getClass());

				if (reason.getSubject() instanceof FeatureModelToNodeTraceModel.FeatureModelElementTrace) {

					final FeatureModelToNodeTraceModel.FeatureModelElementTrace traceModel =
						(FeatureModelToNodeTraceModel.FeatureModelElementTrace) reason.getSubject();

					switch (traceModel.getOrigin()) {
					case CHILD_DOWN:
						break;
					case CHILD_HORIZONTAL:

						final IFeature affectedFeature = featureModelFormula.getFeatureModel().getFeature(possiblyExcluded);

						if ((affectedFeature != null)) {
							final IFeature affectedParent = affectedFeature.getStructure().getParent().getFeature();
							if (affectedParent != null) {

								offeredResolutions.add(new ResolutionConvertAlternativeToOr(marker, fmManager, affectedParent, featurePrefix, ""));
							}
						}
						return true;
					case CHILD_UP:
						break;
					case CONSTRAINT:
						offeredResolutions.add(new ResolutionDeleteConstraint(marker, reason.toNode(), fmManager, featurePrefix,
								" (It excludes ''" + possiblyExcluded + "'')"));

						IConstraint editConstraint = null;

						for (final IConstraint c : featureModelFormula.getFeatureModel().getConstraints()) {
							if (c.getNode().equals(reason.toNode())) {
								editConstraint = c;
							}
						}
						if (editConstraint != null) {
							offeredResolutions.add(new ResolutionEditConstraint(marker, editConstraint, fmManager, featurePrefix,
									" (It excludes ''" + possiblyExcluded + "'')"));

						} else {
							Logger.logWarning("Constraint " + reason.toNode() + " was not found");
						}
						return true;
					case ROOT:
						break;
					default:
						break;
					}
				}
			}
		}
		return false;
	}

	/**
	 * Checks, if the reasons contain a constraint, where {@code possibleReason} implies {@code possiblyImplied}
	 *
	 * @param reason
	 * @param offeredResolutions
	 * @param possiblyImplied
	 * @param possibleReason
	 */
	void checkImplicationConstraint(Set<Reason<?>> reasons, Set<IMarkerResolution> offeredResolutions, String possiblyImplied, String possibleReason,
			String featurePrefix) {
		for (final Reason<?> reason : reasons) {
			if (checkForImplication(possiblyImplied, List.of(possibleReason), reason).size() > 0) {
				offeredResolutions.add(new ResolutionDeleteConstraint(marker, reason.toNode(), fmManager, featurePrefix));
				IConstraint editConstraint = null;

				for (final IConstraint c : featureModelFormula.getFeatureModel().getConstraints()) {
					if (c.getNode().equals(reason.toNode())) {
						editConstraint = c;
					}
				}
				if (editConstraint != null) {
					offeredResolutions.add(new ResolutionEditConstraint(marker, editConstraint, fmManager, featurePrefix));
				} else {
					Logger.logWarning("Constraint " + reason.toNode() + " was not found");
				}
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
	void checkForExcludingFalseOptionals(Set<Reason<?>> reasons, IFeature affectedFeature, Set<IMarkerResolution> offeredResolutions, String featurePrefix,
			Set<String> involvedFeatures) {
		final Set<String> falseOptionals = getExcludingFalseOptionals(reasons, affectedFeature.getName(), involvedFeatures);
		System.out.println("Check false optionals excluding " + affectedFeature);

		for (final String featureName : falseOptionals) {
			IMarker[] markers = null;
			try {
				markers = modelFile.findMarkers(MODEL_MARKER, false, 0);
				if ((markers != null) && (markers.length > 0)) {
					for (final IMarker marker : markers) {
						final String message = (String) marker.getAttribute(IMarker.MESSAGE);
						if ((message != null) && message.startsWith(IFeatureProject.MARKER_FALSE_OPTIONAL) && message.split("''")[1].equals(featureName)) {

							for (final IMarkerResolution resolution : quickFixHandler.getResolutionsShowPrefix(marker)) {
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
	 * Check, if a feature is implied by false-optional features and add their fixes to the list.
	 *
	 * @param reasons
	 * @param affectedFeature
	 * @param offeredResolutions
	 */
	boolean checkForImplyingFalseOptionals(Set<Reason<?>> reasons, IFeature affectedFeature, Set<IMarkerResolution> offeredResolutions, String featurePrefix,
			Set<String> involvedFeatures) {
		System.out.println("Check optionals implying " + affectedFeature);
		boolean found = false;
		final Set<String> falseOptionals = getImplyingFalseOptionals(reasons, affectedFeature.getName(), involvedFeatures);
		for (final String featureName : falseOptionals) {
			IMarker[] markers = null;
			try {
				markers = modelFile.findMarkers(MODEL_MARKER, false, 0);
				final IMarker reasonMarker = getMarkerOfDefectFeature(featureName, IFeatureProject.MARKER_FALSE_OPTIONAL);

				if (reasonMarker != null) {
					for (final IMarkerResolution resolution : quickFixHandler.getResolutionsShowPrefix(reasonMarker)) {
						offeredResolutions.add(resolution);
					}
					found = true;

				}

			} catch (final CoreException e) {
				Logger.logError(e);
			}

		}
		return found;

	}

	/**
	 * Check, if a feature is involved in both an implication and an exclusion with another feature included in the set of reasons.
	 *
	 * @param reasons
	 * @param affectedFeature
	 * @param offeredResolutions
	 */
	void checkImpliesOwnExclusion(Set<Reason<?>> reasons, IFeature affectedFeature, Set<IMarkerResolution> offeredResolutions, String featurePrefix) {
		System.out.println("CKECK IMPLY OWN EXCL for " + affectedFeature);
		final Map<String, Node> impliedFeatures = new HashMap<>();

		for (final Reason<?> r : reasons) {

			if (r.getSubject() instanceof FeatureModelToNodeTraceModel.FeatureModelElementTrace) {
				final FeatureModelToNodeTraceModel.FeatureModelElementTrace traceModel = (FeatureModelToNodeTraceModel.FeatureModelElementTrace) r.getSubject();
				if (traceModel.getOrigin() == Origin.CONSTRAINT) {
					for (final String f : r.toNode().getContainedFeatures()) {
						if (checkForImplication(f, List.of(affectedFeature.getName()), r).contains(affectedFeature.getName())) {
							impliedFeatures.put(f, r.toNode());
						}
					}
				}
			}
		}

		final Node allReasons = new And();

		final Node[] reasonNodes = new Node[reasons.size()];

		for (int i = 0; i < reasons.size(); i++) {
			reasonNodes[i] = ((Reason<?>) reasons.toArray()[i]).toNode();
		}

		allReasons.setChildren(reasonNodes);

		for (final String implied : impliedFeatures.keySet()) {
			if (isExcluding(implied, affectedFeature.getName(), allReasons)) {
				offeredResolutions.add(new ResolutionDeleteConstraint(marker, (org.prop4j.Node) impliedFeatures.get(implied), fmManager, featurePrefix,
						" (It implies ''" + implied + "'', leading to the exclusion of ''" + affectedFeature + "'')"));
			}
		}
	}

	/**
	 * Determines all false-optional features that occur in the reasons for a dead feature and exclude it.
	 *
	 * @param deadFeature
	 * @return The list of all excluding false-optionals.
	 */
	private Set<String> getExcludingFalseOptionals(Set<Reason<?>> reasons, String deadFeature, Set<String> involvedFeatures) {
		final List<String> falseOptionals = new ArrayList<String>(
				involvedFeatures.stream().filter(x -> analyzer.getFalseOptionalFeatures(null).stream().map(ft -> ft.getName()).toList().contains(x)).toList());
		final Set<String> result = new HashSet<>();
		for (final Reason<?> r : reasons) {
			result.addAll(checkForExclusion(deadFeature, falseOptionals, r.toNode()));
		}
		return result;
	}

	/**
	 * Determines all false-optional features that occur in the reasons for a defect feature and imply it.
	 *
	 * @param falseOptionalFeature
	 * @return The list of all implying false-optionals.
	 */
	private Set<String> getImplyingFalseOptionals(Set<Reason<?>> reasons, String falseOptionalFeature, Set<String> involvedFeatures) {
		final List<String> falseOptionals = new ArrayList<String>(
				involvedFeatures.stream().filter(x -> analyzer.getFalseOptionalFeatures(null).stream().map(ft -> ft.getName()).toList().contains(x)).toList());
		final Set<String> result = new HashSet<>();
		for (final Reason<?> r : reasons) {
			result.addAll(checkForImplication(falseOptionalFeature, falseOptionals, r));
		}
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
	private List<String> checkForExclusion(String excludedFeature, List<String> possiblyExcluding, final Node r) {
		final List<String> excluding = new ArrayList<>();

		if (!r.getContainedFeatures().contains(excludedFeature)) {
			return excluding;
		}

		boolean featuresContained = false;

		for (final String fName : possiblyExcluding) {
			if (r.getContainedFeatures().contains(fName)) {
				featuresContained = true;
			}
		}

		if (!featuresContained) {
			return excluding;
		}

		for (final String s : possiblyExcluding) {
			if (r.getContainedFeatures().contains(s)) {
				excluding.add(s);
			}
		}
		final Set<Map<Object, Boolean>> set = r.getSatisfyingAssignments();
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

	public boolean isExcluding(String possibleReason, String excluded, Node reason) {
		if (checkForExclusion(excluded, List.of(possibleReason), reason).isEmpty()) {
			return false;
		}
		return true;
	}

	public boolean isImplying(String possibleReason, String implied, Reason<?> reason) {
		if (checkForImplication(implied, List.of(possibleReason), reason).isEmpty()) {
			return false;
		}
		return true;
	}

	/**
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
			if (r.toNode().getContainedFeatures().contains(s) && !s.equals(impliedFeature)) {
				implying.add(s);
			}
		}

		final Set<Map<Object, Boolean>> set = r.toNode().getSatisfyingAssignments();
		for (final Map<Object, Boolean> map : set) {
			if ((map.get(impliedFeature) != null) && !map.get(impliedFeature)) {

				for (final Map.Entry<Object, Boolean> entry : map.entrySet()) {

					if ((possiblyImplying.contains(entry.getKey().toString()) && entry.getValue()) || entry.getKey().toString().equals(impliedFeature)) {

						implying.remove(entry.getKey().toString()); // Assignment was found, where the implication would be violated
					}
				}
			}
		}
		return implying;
	}

	// TODO where is checked, that the implied features are alternatives?
	void checkImpliesMultiAlt(IMarker marker, Set<Reason<?>> reasons, IFeature affectedFeature, Set<IMarkerResolution> offeredResolutions,
			String featurePrefix) {

		final Map<String, Map<String, Node>> impliedAlternatives = new HashMap<>();

		for (final Reason<?> r : reasons) {

			if (r.getSubject() instanceof FeatureModelToNodeTraceModel.FeatureModelElementTrace) {

				final FeatureModelToNodeTraceModel.FeatureModelElementTrace traceModel = (FeatureModelToNodeTraceModel.FeatureModelElementTrace) r.getSubject();

				if (traceModel.getOrigin() == Origin.CONSTRAINT) {
					for (final String f : r.toNode().getContainedFeatures()) {
						if ((featureModelFormula.getFeatureModel().getFeature(f).getStructure().getParent() != null)
							&& featureModelFormula.getFeatureModel().getFeature(f).getStructure().getParent().isAlternative()
							&& isImplying(affectedFeature.getName(), f, r)) {

							final IFeature impliedFeature = featureModelFormula.getFeatureModel().getFeature(f);

							if (impliedAlternatives.get(impliedFeature.getStructure().getParent().getFeature().getName()) == null) {
								impliedAlternatives.put(impliedFeature.getStructure().getParent().getFeature().getName(), new HashMap<String, Node>());
							}
							impliedAlternatives.get(impliedFeature.getStructure().getParent().getFeature().getName()).put(f, r.toNode());

						}
					}
				}
			}
		}

		for (final String parent : impliedAlternatives.keySet()) {
			if (impliedAlternatives.get(parent).size() > 1) {
				for (final String impliedAlt : impliedAlternatives.get(parent).keySet()) {

					IConstraint editConstraint = null;
					final Node implyingNode = impliedAlternatives.get(parent).get(impliedAlt);

					for (final IConstraint c : featureModelFormula.getFeatureModel().getConstraints()) {
						if (c.getNode().equals(implyingNode)) {
							editConstraint = c;
						}
					}
					if (editConstraint != null) {
						offeredResolutions.add(new ResolutionEditConstraint(marker, editConstraint, fmManager, featurePrefix, " (It implies an alternative)"));

					} else {
						Logger.logWarning("Constraint " + implyingNode + " was not found");
					}

					offeredResolutions.add(new ResolutionDeleteConstraint(marker, implyingNode, fmManager, featurePrefix, " (It implies an alternative)"));
				}
			}
		}

	}

	/**
	 * Checks, if the feature {@code featureName} is true in any of the satisfying assignments of {@code node}
	 *
	 * @param featureName
	 * @param node
	 * @return
	 */
	boolean canBeSelectedInConstraint(String featureName, Node node) {

		if (node.getContainedFeatures().contains(featureName)) {
			for (final Map<Object, Boolean> map : node.getSatisfyingAssignments()) {
				if (map.get(featureName) == true) {
					return true;
				}
			}
			return false;
		}
		return true;
	}

	/**
	 * @param reasons
	 * @param affectedFeature
	 * @param offeredResolutions
	 * @param featurePrefix
	 * @param involvedFeatures
	 */
	public Set<String> getImpliedDeadFeatures(Set<Reason<?>> reasons, String affectedFeature, Set<String> involvedFeatures) {
		final List<String> deadFeatures = new ArrayList<String>(
				involvedFeatures.stream().filter(x -> analyzer.getDeadFeatures(null).stream().map(ft -> ft.getName()).toList().contains(x)).toList());
		final Set<String> result = new HashSet<>();
		for (final Reason<?> r : reasons) {
			for (final String deadFeature : deadFeatures) {
				if (isImplying(affectedFeature, deadFeature, r)) {
					result.add(deadFeature);
				}
			}
		}
		return result;
	}

	public boolean checkImpliedDeadFeatures(Set<Reason<?>> reasons, IFeature affectedFeature, Set<IMarkerResolution> offeredResolutions, String featurePrefix,
			Set<String> involvedFeatures) {

		boolean found = false;
		final Set<String> deadFeatures = getImpliedDeadFeatures(reasons, affectedFeature.getName(), involvedFeatures);
		System.out.println("CHECK IMPLIED DEAD: " + affectedFeature + " -> " + deadFeatures);
		System.out.println("    DEAD: " + deadFeatures);
		for (final String featureName : deadFeatures) {

			final IMarker reasonMarker = getMarkerOfDefectFeature(featureName, IFeatureProject.MARKER_DEAD);

			if (reasonMarker != null) {
				for (final IMarkerResolution resolution : quickFixHandler.getResolutionsShowPrefix(reasonMarker)) {
					System.out.println("ADD IMPLY DEAD RES " + resolution.getLabel());
					offeredResolutions.add(resolution);
					found = true;
				}
			}
		}
		System.out.println("Found? " + found + " resset: " + offeredResolutions);
		return found;
	}

	/**
	 * @param reasons
	 * @param offeredResolutions
	 * @param affectedFeature
	 * @param featurePrefix
	 * @param string
	 */
	public void checkAlternativeExclusion(Set<Reason<?>> reasons, Set<IMarkerResolution> offeredResolutions, IFeature affectedFeature, String prefix,
			String postfix) {
		final IFeature parent = affectedFeature.getStructure().getParent().getFeature();

		for (final IFeature child : parent.getStructure().getChildren().stream().map(x -> x.getFeature()).toList()) {
			if (analyzer.getDeadFeatures(null).contains(child)) {

			}
		}
	}

	// TODO use for false-optionals as well
	private IMarker getMarkerOfDefectFeature(String featureName, String markerPrefix) {

		IMarker[] markers;
		try {
			markers = modelFile.findMarkers(MODEL_MARKER, false, 0);
			if ((markers != null) && (markers.length > 0)) {
				for (final IMarker marker : markers) {
					final String message = (String) marker.getAttribute(IMarker.MESSAGE);
					if (message.startsWith(markerPrefix) && message.split("''")[1].equals(featureName)) {

						return marker;
					}
				}
			} else {
				return null;
			}
		} catch (final CoreException e) {
			Logger.logError(featureName);
		}
		return null;
	}

}
