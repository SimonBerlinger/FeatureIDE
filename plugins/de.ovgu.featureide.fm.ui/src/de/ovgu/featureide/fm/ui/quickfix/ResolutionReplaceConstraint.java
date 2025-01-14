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

import java.util.Objects;

import org.eclipse.core.resources.IMarker;
import org.prop4j.Node;

import de.ovgu.featureide.fm.core.base.IConstraint;
import de.ovgu.featureide.fm.core.base.impl.Constraint;
import de.ovgu.featureide.fm.core.io.manager.FeatureModelManager;

/**
 * A defect resolution, that replaces a constraint with another constraint.
 *
 * @author Simon Berlinger
 */
public class ResolutionReplaceConstraint extends AbstractResolution {

	/**
	 * The node of the original constraint
	 */
	private final Node originalNode;
	/**
	 * The node of the new constraint, that should replace the old one
	 */
	private final Node newNode;

	/**
	 *
	 * @param fmManager The FeatureModelManager
	 * @param originalNode The node of the original constraint
	 * @param newNode The node of the new constraint, that replaces the old one
	 * @param prefix The prefix for the label indicating the defect
	 */
	public ResolutionReplaceConstraint(FeatureModelManager fmManager, Node originalNode, Node newNode, String prefix) {
		super(fmManager);
		this.originalNode = originalNode;
		this.newNode = newNode;
		this.prefix = prefix;
	}

	@Override
	public String getLabel() {

		return prefix + "Change the constraint ''" + originalNode + "'' to ''" + newNode + "''";
	}

	@Override
	public void run(IMarker marker) {
		fmManager.overwrite();
		fmManager.editObject(featureModel -> {
			for (final IConstraint c : featureModel.getConstraints()) {
				if (c.getNode().equals(originalNode)) {
					featureModel.removeConstraint(c);
					break;
				}
			}
			featureModel.addConstraint(new Constraint(featureModel, newNode));
		});

		fmManager.save();
		fmManager.overwrite();

	}

	@Override
	public int hashCode() {
		return Objects.hash(newNode, originalNode);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final ResolutionReplaceConstraint other = (ResolutionReplaceConstraint) obj;
		return Objects.equals(newNode, other.newNode) && Objects.equals(originalNode, other.originalNode);
	}

	@Override
	public String toString() {
		return "ResolutionSetConstraint [originalNode=" + originalNode + ", newNode=" + newNode + "]";
	}

}
