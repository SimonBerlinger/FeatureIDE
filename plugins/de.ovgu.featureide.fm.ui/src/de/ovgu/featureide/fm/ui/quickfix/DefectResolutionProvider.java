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

import org.eclipse.ui.IMarkerResolution;
import org.prop4j.And;
import org.prop4j.Implies;
import org.prop4j.Literal;
import org.prop4j.Node;

import de.ovgu.featureide.fm.core.FeatureModelAnalyzer;
import de.ovgu.featureide.fm.core.Features;
import de.ovgu.featureide.fm.core.Logger;
import de.ovgu.featureide.fm.core.base.IConstraint;
import de.ovgu.featureide.fm.core.base.IFeature;
import de.ovgu.featureide.fm.core.base.IFeatureModel;
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

	private final IFeatureModel featureModel;
	private final FeatureModelManager fmManager;
	private final FeatureModelAnalyzer analyzer;
	private final DefectQuickFixHandler quickFixHandler;

	public DefectResolutionProvider(IFeatureModel featureModel, FeatureModelManager fmManager, FeatureModelAnalyzer analyzer,
			DefectQuickFixHandler quickFixHandler) {
		this.featureModel = featureModel;
		this.fmManager = fmManager;
		this.analyzer = analyzer;
		this.quickFixHandler = quickFixHandler;
	}

	/**
	 * Checks, if a reason consists of a mandatory relation and if so, adds the resolution to {@code offeredResolutions}.
	 *
	 * @param reason The reason to check.
	 * @param offeredResolutions The set of resolutions for the current fix.
	 * @param featurePrefix The prefix that is shown for all found fixes in the quick fix dialog
	 *
	 * @return The mandatory feature if the reason consists of a feature being mandatory.
	 */
	IFeature checkMandatoryChildReason(Reason<?> reason, Set<IMarkerResolution> offeredResolutions, String featurePrefix) {

		if ((reason.toNode().getContainedFeatures().size() == 2)) {
			final IFeature featureA = featureModel.getFeature(reason.toNode().getContainedFeatures().get(0));
			final IFeature featureB = featureModel.getFeature(reason.toNode().getContainedFeatures().get(1));

			if (reason.toNode().toString()
					.equals(new Implies(new Literal(featureA.getName()), new Literal(featureModel.getStructure().getRoot().getFeature().getName())).toString())
				|| reason.toNode().toString().equals(
						new Implies(new Literal(featureB.getName()), new Literal(featureModel.getStructure().getRoot().getFeature().getName())).toString())) {
				return null;
			}

			if ((featureA.getStructure().getParent() != null) && featureA.getStructure().getParent().getFeature().equals(featureB)) {
				if (featureA.getStructure().isMandatory()) {
					offeredResolutions.add(new ResolutionMakeOptional(fmManager, featureA, featurePrefix));
					return featureA;
				}
			} else if ((featureB.getStructure().getParent() != null) && featureB.getStructure().getParent().getFeature().equals(featureA)) {
				if (featureB.getStructure().isMandatory()) {
					offeredResolutions.add(new ResolutionMakeOptional(fmManager, featureB, featurePrefix));
				}
				return featureB;
			}
		}
		return null;
	}

	/**
	 * Checks, if the reasons contain a constraint, where {@code possibleReason} excludes {@code possiblyExcluded}
	 *
	 * @param reasons The reasons where an exclusion is searched
	 * @param offeredResolutions The set to which found resolutions for the exclusion are added
	 * @param possiblyExcluded The name of the feature, which might be excluded
	 * @param possibleReason The name of the feature possibly excluding {@code possiblyExcluded}
	 * @param featurePrefix The current feature-prefix for the label of the resolutions.
	 *
	 * @return true, if possiblyExcluded is excluded by possibleReason
	 */
	boolean checkExclusion(Set<Reason<?>> reasons, Set<IMarkerResolution> offeredResolutions, String possiblyExcluded, String possibleReason,
			String featurePrefix) {

		for (final Reason<?> reason : reasons) {
			if (testForExclusion(possiblyExcluded, List.of(possibleReason), reason.toNode()).size() > 0) {

				if (reason.getSubject() instanceof FeatureModelToNodeTraceModel.FeatureModelElementTrace) {

					final FeatureModelToNodeTraceModel.FeatureModelElementTrace traceModel =
						(FeatureModelToNodeTraceModel.FeatureModelElementTrace) reason.getSubject();

					switch (traceModel.getOrigin()) {
					case CHILD_DOWN:
						break;
					case CHILD_HORIZONTAL:

						final IFeature affectedFeature = featureModel.getFeature(possiblyExcluded);

						if ((affectedFeature != null)) {
							final IFeature affectedParent = affectedFeature.getStructure().getParent().getFeature();
							if (affectedParent != null) {

								offeredResolutions.add(new ResolutionConvertAlternativeToOr(fmManager, affectedParent, featurePrefix));
							}
						}
						return true;
					case CHILD_UP:
						break;
					case CONSTRAINT:
						offeredResolutions.add(new ResolutionDeleteConstraint(reason.toNode(), fmManager, featurePrefix));

						IConstraint editConstraint = null;

						for (final IConstraint c : featureModel.getConstraints()) {
							if (c.getNode().equals(reason.toNode())) {
								editConstraint = c;
							}
						}
						if (editConstraint != null) {
							offeredResolutions.add(new ResolutionEditConstraint(editConstraint, fmManager, featurePrefix));

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
	 * Checks, a feature excludes its own parent and adds the respective resolutions to the set of resolutions
	 *
	 * @param reasons The set of reasons.
	 * @param offeredResolutions The set of offered resolutions.
	 * @param childOfExcluded The feature, that potentially excludes its parent.
	 * @param involvedFeatures The set of all features, that occur in the reasons.
	 * @param featurePrefix The prefix for the feature.
	 */
	void checkParentExclusion(Set<Reason<?>> reasons, Set<IMarkerResolution> offeredResolutions, String childOfExcluded, Set<String> involvedFeatures,
			String featurePrefix) {

		final IFeature child = featureModel.getFeature(childOfExcluded);

		final String parentName = child.getStructure().getParent().getFeature().getName();

		for (final String f : involvedFeatures) {
			checkExclusion(reasons, offeredResolutions, f, parentName, featurePrefix);
		}
	}

	/**
	 * Checks, if the reasons contain a constraint, where {@code possibleReason} implies {@code possiblyImplied}
	 *
	 * @param reasons The set of reasons, where the implication is searched
	 * @param offeredResolutions The set of resolutions to which possible resolutions of the implication are added
	 * @param possiblyImplied The name of the false-optional feature, that might be implied
	 * @param possibleReason The name of the feature, that might imply {@code possiblyImplied}
	 * @param featurePrefix The current feature-prefix for the label of the resolutions.featurePrefix
	 * @param isOriginalDefect Defines, if the current defect is the original defect (making a false-optional mandatory is no solution, if the false-optional is
	 *        causing another defect)
	 */
	void checkImplicationConstraintForFalseOptional(Set<Reason<?>> reasons, Set<IMarkerResolution> offeredResolutions, IFeature possiblyImplied,
			IFeature possibleReason, String featurePrefix) {
		for (final Reason<?> reason : reasons) {
			if (testForImplication(List.of(possibleReason.getName()), possiblyImplied.getName(), reason.toNode()).size() > 0) {
				offeredResolutions.add(new ResolutionDeleteConstraint(reason.toNode(), fmManager, featurePrefix));

				offeredResolutions.add(new ResolutionMakeMandatory(fmManager, possiblyImplied.getName(),
						Features.getCommonAncestor(List.of(possiblyImplied, possibleReason)).getName(), featurePrefix));

				IConstraint editConstraint = null;

				for (final IConstraint c : featureModel.getConstraints()) {
					if (c.getNode().equals(reason.toNode())) {
						editConstraint = c;
					}
				}
				if (editConstraint != null) {
					offeredResolutions.add(new ResolutionEditConstraint(editConstraint, fmManager, featurePrefix));
				} else {
					Logger.logWarning("Constraint " + reason.toNode() + " was not found");
				}
			}
		}
	}

	/**
	 * Checks, if a feature is excluded by false-optional features and add their fixes to the list.
	 *
	 * @param reasons The set of reasons, where the exclusion is searched
	 * @param offeredResolutions The set of resolutions, to which found resolutions are added
	 * @param affectedFeature The feature which is possibly excluded
	 * @param featurePrefix The prefix for the resolutions to show in the quick fix dialog
	 * @param involvedFeatures The set of involved features.
	 */
	void checkForExcludingFalseOptionals(Set<Reason<?>> reasons, Set<IMarkerResolution> offeredResolutions, IFeature affectedFeature, String featurePrefix,
			Set<String> involvedFeatures) {
		final Set<String> falseOptionals = getExcludingFalseOptionals(reasons, affectedFeature.getName(), involvedFeatures);

		for (final String featureName : falseOptionals) {

			quickFixHandler.getFalseOptionalResolutions(this, offeredResolutions, featureModel.getFeature(featureName));
		}
	}

	/**
	 * Checks, if a feature is implied by false-optional features and add their fixes to the list.
	 *
	 * @param reasons The set of reasons, where the implication is searched
	 * @param offeredResolutions The set of resolutions, to which found resolutions are added
	 * @param affectedFeature The feature, that might be implied
	 * @param featurePrefix The prefix for the resolutions to show in the quick fix dialog
	 * @param involvedFeatures The set of involved features.
	 * @return true, if an implication was found, false otherwise
	 */
	boolean checkForImplyingFalseOptionals(Set<Reason<?>> reasons, Set<IMarkerResolution> offeredResolutions, IFeature affectedFeature, String featurePrefix,
			Set<String> involvedFeatures) {
		boolean found = false;
		final Set<String> falseOptionals = getImplyingFalseOptionals(reasons, affectedFeature.getName(), involvedFeatures);
		for (final String featureName : falseOptionals) {

			found |= quickFixHandler.getFalseOptionalResolutions(this, offeredResolutions, featureModel.getFeature(featureName)).length > 0;

		}
		return found;

	}

	/**
	 * Checks, if a feature is involved in both an implication and an exclusion with another feature included in the set of reasons.
	 *
	 * @param reasons The set of reasons, where the implication and exclusion are searched
	 * @param offeredResolutions The set of resolutions, to which found resolutions are added
	 * @param affectedFeature The feature, that might imply its own exclusion
	 * @param featurePrefix The prefix for the resolutions to show in the quick fix dialog
	 */
	void checkImpliesOwnExclusion(Set<Reason<?>> reasons, Set<IMarkerResolution> offeredResolutions, IFeature affectedFeature, String featurePrefix) {
		// Map containing feature-names and the Node, in which it is implied through {@code affectedFeature}
		final Map<String, Node> impliedFeatures = new HashMap<>();

		for (final Reason<?> r : reasons) {
			if (r.getSubject() instanceof FeatureModelToNodeTraceModel.FeatureModelElementTrace) {
				final FeatureModelToNodeTraceModel.FeatureModelElementTrace traceModel = (FeatureModelToNodeTraceModel.FeatureModelElementTrace) r.getSubject();
				if (traceModel.getOrigin() == Origin.CONSTRAINT) {
					for (final String f : r.toNode().getContainedFeatures()) {
						if (testForImplication(affectedFeature.getName(), f, r.toNode()).contains(affectedFeature.getName())) {
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
				offeredResolutions.add(new ResolutionDeleteConstraint(impliedFeatures.get(implied), fmManager, featurePrefix));
				offeredResolutions.add(new ResolutionEditConstraint(impliedFeatures.get(implied), fmManager, featurePrefix));
			}
		}
	}

	/**
	 * Determines all false-optional features that occur in the reasons for a dead feature and exclude it.
	 *
	 * @param reasons The reasons, where the exclusions are searched
	 * @param deadFeature The feature, that might be excluded through false-optionals
	 * @param involvedFeatures A set of all feature names contained in the reason-set
	 * @return The list of all excluding false-optionals.
	 */
	private Set<String> getExcludingFalseOptionals(Set<Reason<?>> reasons, String deadFeature, Set<String> involvedFeatures) {
		final List<String> falseOptionals = new ArrayList<String>(
				involvedFeatures.stream().filter(x -> analyzer.getFalseOptionalFeatures(null).stream().map(ft -> ft.getName()).toList().contains(x)).toList());
		final Set<String> result = new HashSet<>();
		for (final Reason<?> r : reasons) {
			result.addAll(testForExclusion(deadFeature, falseOptionals, r.toNode()));
		}
		return result;
	}

	/**
	 * Determines all false-optional features that occur in the reasons for a defect feature and imply it.
	 *
	 * @param reasons The set of reasons, where the implications are searched
	 * @param falseOptionalFeature The feature, that might be implied through false-optionals
	 * @param involvedFeatures A set of all feature names contained in the reason-set
	 * @return The list of all implying false-optionals.
	 */
	private Set<String> getImplyingFalseOptionals(Set<Reason<?>> reasons, String falseOptionalFeature, Set<String> involvedFeatures) {
		final List<String> falseOptionals = new ArrayList<String>(
				involvedFeatures.stream().filter(x -> analyzer.getFalseOptionalFeatures(null).stream().map(ft -> ft.getName()).toList().contains(x)).toList());
		final Set<String> result = new HashSet<>();
		for (final Reason<?> r : reasons) {
			result.addAll(testForImplication(falseOptionals, falseOptionalFeature, r.toNode()));
		}
		return result;
	}

	/**
	 *
	 * Checks, which of the features from {@code possiblyExcluding} exclude the feature {@code excludedFeature} in the formula that {@code formula} represents.
	 *
	 * @param excludedFeature The feature, that might be excluded
	 * @param possiblyExcluding The set of features, that are tested if they exclude {@code excludedFeature}
	 * @param formula The formula, that is checked for containing exclusions
	 * @return The list of features that would always exclude {@code excludedFeature}, when the formula from {@code r} evaluates to true.
	 */
	private List<String> testForExclusion(String excludedFeature, List<String> possiblyExcluding, final Node formula) {
		final List<String> excluding = new ArrayList<>();

		if (!formula.getContainedFeatures().contains(excludedFeature)) {
			return excluding;
		}

		boolean featuresContained = false;

		for (final String fName : possiblyExcluding) {
			if (formula.getContainedFeatures().contains(fName)) {
				featuresContained = true;
			}
		}

		if (!featuresContained) {
			return excluding;
		}

		for (final String s : possiblyExcluding) {
			if (formula.getContainedFeatures().contains(s)) {
				excluding.add(s);
			}
		}
		final Set<Map<Object, Boolean>> set = formula.getSatisfyingAssignments();
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
	 * Determines, if a feature is excluding another feature through a specified reason.
	 *
	 * @param possibleReason The feature, that is potentially excluding.
	 * @param excluded The feature, that is potentially excluded
	 * @param reason The reason to be checked
	 * @return True, if {@code possibleReason} is excluding {@code excluded}
	 */
	boolean isExcluding(String possibleReason, String excluded, Node reason) {
		if (testForExclusion(excluded, List.of(possibleReason), reason).isEmpty()) {
			return false;
		}
		return true;
	}

	/**
	 * Determines, if a feature is implying another feature through a specified reason.
	 *
	 * @param possibleReason The feature, that is potentially implying the other.
	 * @param implied The feature, that is potentially implied
	 * @param reason The reason to be checked
	 * @return True, if {@code possibleReason} is implying {@code excluded}
	 */
	boolean isImplying(String possibleReason, String implied, Reason<?> reason) {
		if (testForImplication(List.of(possibleReason), implied, reason.toNode()).isEmpty()) {
			return false;
		}
		return true;
	}

	/**
	 *
	 * Checks, if the features {@code possiblyExcluding} implies the feature {@code excludedFeature} in the formula {@code formula} is based on.
	 *
	 * @param possiblyImplying THe possibly implying feature.
	 * @param impliedFeature The feature, that might be implied.
	 * @param formula The node, that is to be checked for implications.
	 * @return The list of features that would always imply {@code excludedFeature}, when the formula from {@code r} evaluates to true.
	 */
	private List<String> testForImplication(String possiblyImplying, String impliedFeature, Node formula) {
		return testForImplication(List.of(possiblyImplying), impliedFeature, formula);
	}

	/**
	 *
	 * Checks, which of the features from {@code possiblyExcluding} imply the feature {@code excludedFeature} in the formula that {@code r} is based on.
	 *
	 * @param possiblyImplying The list of possibly implying features.
	 * @param impliedFeature The feature, that might be implied.
	 * @param formula The node, that is to be checked for implications.
	 *
	 * @return The list of features that would always imply {@code excludedFeature}, when the formula {@code formula} evaluates to true.
	 */
	private List<String> testForImplication(List<String> possiblyImplying, String impliedFeature, final Node formula) {

		final List<String> implying = new ArrayList<>();

		if (!formula.getContainedFeatures().contains(impliedFeature)) {
			return implying;
		}

		boolean featuresContained = false;

		for (final String fName : possiblyImplying) {
			if (formula.getContainedFeatures().contains(fName)) {
				featuresContained = true;
			}
		}

		if (!featuresContained) {
			return implying;
		}

		for (final String s : possiblyImplying) {
			if (formula.getContainedFeatures().contains(s) && !s.equals(impliedFeature)) {
				implying.add(s);
			}
		}

		final Set<Map<Object, Boolean>> set = formula.getSatisfyingAssignments();
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

	/**
	 *
	 * Checks, if a feature implies multiple alternatives of the same group and adds the resolutions to the set of resolutions.
	 *
	 * @param reasons The set of reasons for the defect.
	 * @param offeredResolutions The set of offered resolutions.
	 * @param affectedFeature The current dead feature to be checked
	 * @param featurePrefix The current feature-prefix for the quick-fix label
	 */
	void checkImpliesMultiAlt(Set<Reason<?>> reasons, Set<IMarkerResolution> offeredResolutions, IFeature affectedFeature, String featurePrefix) {

		final Map<String, Map<String, Node>> impliedAlternatives = new HashMap<>();

		for (final Reason<?> r : reasons) {

			if (r.getSubject() instanceof FeatureModelToNodeTraceModel.FeatureModelElementTrace) {

				final FeatureModelToNodeTraceModel.FeatureModelElementTrace traceModel = (FeatureModelToNodeTraceModel.FeatureModelElementTrace) r.getSubject();

				if (traceModel.getOrigin() == Origin.CONSTRAINT) {
					for (final String f : r.toNode().getContainedFeatures()) {
						if ((featureModel.getFeature(f).getStructure().getParent() != null)
							&& featureModel.getFeature(f).getStructure().getParent().isAlternative() && isImplying(affectedFeature.getName(), f, r)) {

							final IFeature impliedFeature = featureModel.getFeature(f);

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

					for (final IConstraint c : featureModel.getConstraints()) {
						if (c.getNode().equals(implyingNode)) {
							editConstraint = c;
						}
					}
					if (editConstraint != null) {
						offeredResolutions.add(new ResolutionEditConstraint(editConstraint, fmManager, featurePrefix));

					} else {
						Logger.logWarning("Constraint for " + implyingNode + " was not found");
					}

					offeredResolutions.add(new ResolutionDeleteConstraint(implyingNode, fmManager, featurePrefix));
				}
			}
		}

	}

	/**
	 * Checks, if the feature {@code featureName} is true in any of the satisfying assignments of {@code node}
	 *
	 * @param featureName The name of the feature.
	 * @param node The formula to be tested.
	 * @return True, if the feature can be selected
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
	 *
	 * Determines and returns a list of dead features, that are implied by a feature.
	 *
	 * @param reasons The reasons, where the implications are searched.
	 * @param affectedFeature THe feature, that may imply dead features.
	 * @param involvedFeatures The set of features involved in the reasons.
	 * @return A list of the implied dead features.
	 */
	private List<String> getImpliedDeadFeatures(Set<Reason<?>> reasons, String affectedFeature, Set<String> involvedFeatures) {
		final List<String> deadFeatures = new ArrayList<String>(
				involvedFeatures.stream().filter(x -> analyzer.getDeadFeatures(null).stream().map(ft -> ft.getName()).toList().contains(x)).toList());
		final List<String> result = new ArrayList<>();
		for (final Reason<?> r : reasons) {
			for (final String deadFeature : deadFeatures) {
				if (isImplying(affectedFeature, deadFeature, r)) {
					result.add(deadFeature);
				}
			}
		}
		return result;
	}

	/**
	 *
	 * Checks, if a feature implies dead features, and if so, adds its resolutions to the set of resolutions.
	 *
	 * @param reasons The current set of reasons.
	 * @param offeredResolutions The set of resolutions.
	 * @param affectedFeature The feature, that may imply dead features.
	 * @param featurePrefix The current feature-prefix for the label of the resolutions.
	 * @param involvedFeatures The set of involved features.
	 * @return True, if at least one dead feature is implied.
	 */
	boolean checkImpliedDeadFeatures(Set<Reason<?>> reasons, Set<IMarkerResolution> offeredResolutions, IFeature affectedFeature, String featurePrefix,
			Set<String> involvedFeatures) {
		final List<String> deadFeatures = getImpliedDeadFeatures(reasons, affectedFeature.getName(), involvedFeatures);

		// Only add resolutions for the first found implied dead feature to avoid too long lists of resolutions
		if (deadFeatures.size() > 0) {
			quickFixHandler.getDeadFeatureResolutions(this, offeredResolutions, featureModel.getFeature(deadFeatures.get(0)));
			return true;
		}
		return false;
	}

	/**
	 * checks, if enough alternatives are dead to make a feature false optional
	 *
	 * @param reasons The current set of reasons.
	 * @param offeredResolutions The set of offered resolutions.
	 * @param affectedFeature The feature, that might be false-optional because of exclusions.
	 * @param featurePrefix The current feature-prefix for the label of the resolutions.
	 */
	void checkAlternativeExclusion(Set<Reason<?>> reasons, Set<IMarkerResolution> offeredResolutions, IFeature affectedFeature, String featurePrefix) {

		final IFeature parent = affectedFeature.getStructure().getParent().getFeature();
		final Set<IMarkerResolution> proposedResolutions = new HashSet<>();

		if (parent.getStructure().isAlternative() || parent.getStructure().isOr()) {
			for (final IFeature child : parent.getStructure().getChildren().stream().map(x -> x.getFeature()).toList()) {
				if (!analyzer.getDeadFeatures(null).contains(child) && !child.getName().equals(affectedFeature.getName())) {
					return;
				} else if (!child.getName().equals(affectedFeature.getName())) {

					quickFixHandler.getDeadFeatureResolutions(this, proposedResolutions, child);
				}
			}
			offeredResolutions.addAll(proposedResolutions);

		}
	}

	/**
	 * Checks, if a reason originates from a constraint.
	 *
	 * @param reason THe reason to check.
	 * @return True if the reason originates from a constraint.
	 */
	boolean isReasonConstraint(Reason<?> reason) {
		if (reason.getSubject() instanceof FeatureModelToNodeTraceModel.FeatureModelElementTrace) {
			return ((FeatureModelToNodeTraceModel.FeatureModelElementTrace) reason.getSubject()).getOrigin() == Origin.CONSTRAINT;
		}
		return false;
	}

	/**
	 * Checks, which of the clauses of the cnf representation of {@code possiblyContained} are redundant in {@code constraint} and adds resolutions to either
	 * delete {@code possiblyContained} or to adapt {@code constraint}.
	 *
	 * @param possiblyContained The formula, that might be contained in {@code constraint}
	 * @param constraint The formula, that contains information from {@code possiblyContained}
	 * @param offeredResolutions The set of resolutions.
	 * @param isContainedRelation Indicates, whether the contained constraint originates from a relation.
	 * @param isConstraintRelation Indicates, whether the constraint, in which {@code possiblyContained} is contained, originates from a relation.
	 * @return True, if all clauses or {@code possiblyContained} are contained in {@code constraint}
	 */
	boolean checkClausesContained(Node possiblyContained, Node constraint, Set<IMarkerResolution> offeredResolutions, boolean isContainedRelation,
			boolean isConstraintRelation) {

		final List<Node> clausesOfContained = getClausesOfCnf(possiblyContained.toCNF());
		final List<Node> clausesOfConstraint = getClausesOfCnf(constraint.toCNF());

		final List<Node> irredundantClauses = new ArrayList<>(clausesOfContained);

		for (final Node clause : clausesOfContained) {
			if (clausesOfConstraint.contains(clause)) {
				irredundantClauses.remove(clause);
			}
		}

		if (irredundantClauses.size() > 0) {
			// Not all clauses of {@code possiblyContained} are contained in {@code constraint} -> check, if remaining clauses, that are not equal to any of the
			// contained constraints in {@code constraint} are redundant

			removeRedundantClauses(irredundantClauses, constraint);

			if (!isConstraintRelation && removeRedundantClauses(clausesOfConstraint, possiblyContained)) {

				final Node[] newChildren = new Node[clausesOfConstraint.size()];
				for (int i = 0; i < clausesOfConstraint.size(); i++) {
					newChildren[i] = clausesOfConstraint.get(i);
				}

				final Node newConstraint = new And();
				newConstraint.setChildren(newChildren);

				offeredResolutions.add(new ResolutionChangeConstraint(fmManager, constraint, newConstraint, ""));
			}

		} else if (!isConstraintRelation) {
			// All clauses of {@code possiblyContained} are contained in {@code constraint}

			for (final Node clause : clausesOfContained) {
				clausesOfConstraint.remove(clause);
			}

			final Node[] newChildren = new Node[clausesOfConstraint.size()];
			for (int i = 0; i < clausesOfConstraint.size(); i++) {
				newChildren[i] = clausesOfConstraint.get(i);
			}

			final Node newConstraint = new And();
			newConstraint.setChildren(newChildren);

			offeredResolutions.add(new ResolutionChangeConstraint(fmManager, constraint, newConstraint, ""));

		}

		if (!isContainedRelation && (irredundantClauses.size() == 0)) {
			offeredResolutions.add(new ResolutionDeleteConstraint(possiblyContained, fmManager));
		}

		return irredundantClauses.size() == 0;

	}

	/**
	 * Removes clauses, that are redundant to a formula.
	 *
	 * @param clauses The clauses, that are checked for redundancy
	 * @param constraint The formula, to which redundancies are tested.
	 * @return True, if any of the clauses are redundant and were removed.
	 */
	private boolean removeRedundantClauses(final List<Node> clauses, Node constraint) {
		boolean changed = false;

		final Set<Map<Object, Boolean>> assignments = constraint.getSatisfyingAssignments();
		final List<Node> toRemove = new ArrayList<>();
		for (final Node clause : clauses) {

			boolean redundant = true;

			final Node[] literals = clause.getChildren() != null ? clause.getChildren() : new Node[] { clause };

			for (final Map<Object, Boolean> assignment : assignments) {

				boolean literalSatisfied = false;

				for (final Node literal : literals) {

					if (literal instanceof Literal) {

						if (((Literal) literal).positive && assignment.keySet().contains(literal.getContainedFeatures().get(0))
							&& assignment.get(literal.getContainedFeatures().get(0))) {
							literalSatisfied = true;

							break;
						} else if (!((Literal) literal).positive && assignment.keySet().contains(literal.getContainedFeatures().get(0))
							&& !assignment.get(literal.getContainedFeatures().get(0))) {
								literalSatisfied = true;

								break;
							}
					} else {

					}
				}

				// if a satisfying assignment of the constraint does not satisfy any of the literals of the cnf-clause, the clause is not redundant
				if (!literalSatisfied) {

					redundant = false;
					break;
				}

			}
			if (redundant) {
				toRemove.add(clause);
			}
		}

		for (final Node clause : toRemove) {
			clauses.remove(clause);
			changed = true;
		}
		return changed;
	}

	/**
	 * Checks, if a redundant constraint is contained in multiple other constraints and adds resolutions to the set of resolutons.
	 *
	 * @param possiblyContained The redundant constraint.
	 * @param reasons The current set of reasons.
	 * @param offeredResolutions The set of resolutions.
	 */
	void checkMultipleRedundanycReasons(IConstraint possiblyContained, Set<Reason<?>> reasons, Set<IMarkerResolution> offeredResolutions) {

		for (final Reason<?> r : reasons) {
			checkClausesContained(r.toNode(), possiblyContained.getNode(), offeredResolutions, isReasonConstraint(r), false);
		}

		offeredResolutions.add(new ResolutionDeleteConstraint(possiblyContained.getNode(), fmManager));
		offeredResolutions.add(new ResolutionEditConstraint(possiblyContained, fmManager, ""));

	}

	/**
	 * Returns the clauses of a formula in conjunctive normal form.
	 *
	 * @param cnf The formula.
	 * @return The list of clauses.
	 */
	private List<Node> getClausesOfCnf(Node cnf) {
		final List<Node> clauses = new ArrayList<>();
		cnf = cnf.toCNF();

		if (cnf instanceof Literal) {
			clauses.add(cnf);
			return clauses;
		}

		for (final Node n : cnf.getChildren()) {
			if (!clauses.contains(n)) {
				clauses.add(n);
			}
		}

		return clauses;
	}
}
