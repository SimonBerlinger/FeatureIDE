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
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.ui.IMarkerResolution;
import org.prop4j.And;
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
	 * Checks, if the reasons contain a constraint, where {@code possibleReason} implies {@code possiblyImplied}
	 *
	 * @param reasons The set of reasons, where the implication is searched
	 * @param offeredResolutions The set of resolutions to which possible resolutions of the implication are added
	 * @param possiblyImplied The name of the feature, that might be implied
	 * @param possibleReason The name of the feature, that might imply {@code possiblyImplied}
	 */
	void checkImplicationConstraintForFalseOptional(Set<Reason<?>> reasons, Set<IMarkerResolution> offeredResolutions, IFeature possiblyImplied,
			IFeature possibleReason, String featurePrefix) {
		for (final Reason<?> reason : reasons) {
			if (testForImplication(possiblyImplied.getName(), List.of(possibleReason.getName()), reason.toNode()).size() > 0) {
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
		// Map containing feature-names and the Node, in which it it implied by {@code affectedFeature}
		final Map<String, Node> impliedFeatures = new HashMap<>();

		for (final Reason<?> r : reasons) {

			if (r.getSubject() instanceof FeatureModelToNodeTraceModel.FeatureModelElementTrace) {
				final FeatureModelToNodeTraceModel.FeatureModelElementTrace traceModel = (FeatureModelToNodeTraceModel.FeatureModelElementTrace) r.getSubject();
				if (traceModel.getOrigin() == Origin.CONSTRAINT) {
					for (final String f : r.toNode().getContainedFeatures()) {
						if (testForImplication(f, List.of(affectedFeature.getName()), r.toNode()).contains(affectedFeature.getName())) {
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
				offeredResolutions.add(new ResolutionDeleteConstraint((org.prop4j.Node) impliedFeatures.get(implied), fmManager, featurePrefix));

				if (((org.prop4j.Node) impliedFeatures.get(implied)).getContainedFeatures().size() > 2) {
					offeredResolutions.add(new ResolutionEditConstraint(((org.prop4j.Node) impliedFeatures.get(implied)), fmManager, featurePrefix));
				}
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
			result.addAll(testForImplication(falseOptionalFeature, falseOptionals, r.toNode()));
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
	 *
	 * @param possibleReason
	 * @param excluded
	 * @param reason
	 * @return
	 */
	boolean isExcluding(String possibleReasonFeature, String excludedFeature, Node reason) {
		if (testForExclusion(excludedFeature, List.of(possibleReasonFeature), reason).isEmpty()) {
			return false;
		}
		return true;
	}

	/**
	 *
	 * @param possibleReason
	 * @param implied
	 * @param reason
	 * @return
	 */
	boolean isImplying(String possibleReasonFeature, String impliedFeature, Reason<?> reason) {
		if (testForImplication(impliedFeature, List.of(possibleReasonFeature), reason.toNode()).isEmpty()) {
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
	 * @param formula
	 * @return The list of features that would always imply {@code excludedFeature}, when the formula from {@code r} evaluates to true.
	 */
	private List<String> testForImplication(String impliedFeature, List<String> possiblyImplying, final Node formula) {

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
						Logger.logWarning("Constraint " + implyingNode + " was not found");
					}

					offeredResolutions.add(new ResolutionDeleteConstraint(implyingNode, fmManager, featurePrefix));
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

	boolean checkImpliedDeadFeatures(Set<Reason<?>> reasons, Set<IMarkerResolution> offeredResolutions, IFeature affectedFeature, String featurePrefix,
			Set<String> involvedFeatures) {
		final List<String> deadFeatures = getImpliedDeadFeatures(reasons, affectedFeature.getName(), involvedFeatures);

		if (deadFeatures.size() > 0) {
			quickFixHandler.getDeadFeatureResolutions(this, offeredResolutions, featureModel.getFeature(deadFeatures.get(0)));
			return true;
		}
		return false;
	}

	/**
	 * checks, if enough alternatives are dead to make a feature false optional
	 *
	 * @param reasons
	 * @param offeredResolutions
	 * @param affectedFeature
	 * @param featurePrefix
	 * @param string
	 */
	void checkAlternativeExclusion(Set<Reason<?>> reasons, Set<IMarkerResolution> offeredResolutions, IFeature affectedFeature, String prefix, String postfix) {

		final IFeature parent = affectedFeature.getStructure().getParent().getFeature();
		final Set<IMarkerResolution> proposedResolutions = new HashSet<>();

		if (parent.getStructure().isAlternative() || parent.getStructure().isOr()) {
			for (final IFeature child : parent.getStructure().getChildren().stream().map(x -> x.getFeature()).toList()) {
				if (!analyzer.getDeadFeatures(null).contains(child) && !child.getName().equals(affectedFeature.getName())) {
					return;
				} else if (!child.getName().equals(affectedFeature.getName())) {
					System.out.println("DEAD FIX FOR " + affectedFeature.getName() + " CHILD " + child.getName());
					quickFixHandler.getDeadFeatureResolutions(this, proposedResolutions, child);
				}
			}
			offeredResolutions.addAll(proposedResolutions);

		}
	}

	void checkExcludesParent() {
		// TODO
	}

	boolean isReasonConstraint(Reason<?> reason) {
		if (reason.getSubject() instanceof FeatureModelToNodeTraceModel.FeatureModelElementTrace) {
			return ((FeatureModelToNodeTraceModel.FeatureModelElementTrace) reason.getSubject()).getOrigin() == Origin.CONSTRAINT;
		}
		return false;
	}

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

				offeredResolutions.add(new ResolutionSetConstraint(fmManager, constraint, newConstraint, ""));
			}

		} else if (!isConstraintRelation) {
			// All clauses of {@code possiblyContained} are contained in {@code constraint}
			boolean changed = false;
			for (final Node clause : clausesOfContained) {
				clausesOfConstraint.remove(clause);
				changed = true;
			}

			if (changed) {

				final Node[] newChildren = new Node[clausesOfConstraint.size()];
				for (int i = 0; i < clausesOfConstraint.size(); i++) {
					newChildren[i] = clausesOfConstraint.get(i);
				}

				final Node newConstraint = new And();
				newConstraint.setChildren(newChildren);

				offeredResolutions.add(new ResolutionSetConstraint(fmManager, constraint, newConstraint, ""));
			}

		}

		if (!isContainedRelation && (irredundantClauses.size() == 0)) {
			offeredResolutions.add(new ResolutionDeleteConstraint(possiblyContained, fmManager));
		}

		return irredundantClauses.size() == 0;

	}

	/**
	 * @param clauses The clauses, that are checked for redundancy
	 * @param assignments The assignments of the formula in which the clause is checked for redundancy
	 * @param toRemove
	 */
	private boolean removeRedundantClauses(final List<Node> clauses, Node constraint) {
		boolean changed = false;
		System.out.println("   Constraint: " + constraint + " Clauses: " + clauses);
		final Set<Map<Object, Boolean>> assignments = constraint.getSatisfyingAssignments();
		final List<Node> toRemove = new ArrayList<>();
		for (final Node clause : clauses) {
			System.out.println("     Current clause: " + clause);

			boolean redundant = true;

			final Node[] literals = clause.getChildren() != null ? clause.getChildren() : new Node[] { clause };

			for (final Map<Object, Boolean> assignment : assignments) {
				System.out.println("          Current assignment: " + assignment);

				boolean literalSatisfied = false;

				for (final Node literal : literals) {
					System.out.println("               Current literal: " + literal + ": " + literal.getClass() + " :: "
						+ Arrays.toString(literal.getChildren()) + " pos? " + literal);
					System.out.println("Contains?: " + assignment.keySet().contains(literal.getContainedFeatures().get(0)));
					System.out.println("Value?: " + assignment.get(literal.getContainedFeatures().get(0)));
					if (literal instanceof Literal) {

						if (((Literal) literal).positive && assignment.keySet().contains(literal.getContainedFeatures().get(0))
							&& assignment.get(literal.getContainedFeatures().get(0))) {
							literalSatisfied = true;
							System.out.println("               Satisfied");
							break;
						} else if (!((Literal) literal).positive && assignment.keySet().contains(literal.getContainedFeatures().get(0))
							&& !assignment.get(literal.getContainedFeatures().get(0))) {
								literalSatisfied = true;
								System.out.println("               Satisfied");
								break;
							}
					} else {
						System.out.println("LITERAL???");
					}
				}

				// if a satisfying assignment of the constraint does not satisfy any of the literals of the cnf-clause, the clause is not redundant
				if (!literalSatisfied) {
					System.out.println("clause " + clause + " is not redundant");
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

	void checkMultipleRedunanycReasons(IConstraint possiblyContained, Set<Reason<?>> reasons, Set<IMarkerResolution> offeredResolutions) {

		for (final Reason<?> r : reasons) {
			checkClausesContained(r.toNode(), possiblyContained.getNode(), offeredResolutions, isReasonConstraint(r), false);
		}

		offeredResolutions.add(new ResolutionDeleteConstraint(possiblyContained.getNode(), fmManager));
		offeredResolutions.add(new ResolutionEditConstraint(possiblyContained, fmManager, ""));

	}

	/**
	 * @param cnf
	 * @return
	 */
	private List<Node> getClausesOfCnf(Node cnf) {
		final List<Node> clauses = new ArrayList<>();

		if (cnf instanceof Literal) {
			clauses.add(cnf);
			return clauses;
		}

		// this expects a Node in cnf, so the children are clauses of the cnf
		for (final Node n : cnf.getChildren()) {
			if (!clauses.contains(n)) {
				clauses.add(n);
			}
		}

		return clauses;
	}
}
