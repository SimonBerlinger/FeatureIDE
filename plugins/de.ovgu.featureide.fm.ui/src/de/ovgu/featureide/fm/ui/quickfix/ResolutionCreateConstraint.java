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

import de.ovgu.featureide.fm.core.base.impl.Constraint;
import de.ovgu.featureide.fm.core.io.manager.FeatureModelManager;

/**
 * A defect resolution, that creates a new cross-tree constraint.
 *
 * @author Simon Berlinger
 */
public class ResolutionCreateConstraint extends AbstractResolution {

	/**
	 * The node of the constraint which is to be created
	 */
	Node toCreateNode = null;

	/**
	 *
	 * @param toCreateNode The node of the constraint which is to be created
	 * @param fmManager The FeatureModelManager
	 */
	public ResolutionCreateConstraint(Node toCreateNode, FeatureModelManager fmManager) {
		super(fmManager);
		this.toCreateNode = toCreateNode;
	}

	@Override
	public String getLabel() {

		return prefix + "Create the constraint ''" + toCreateNode + "''";
	}

	@Override
	public void run(IMarker marker) {

		fmManager.overwrite();
		fmManager.editObject(featureModel -> {
			featureModel.addConstraint(new Constraint(featureModel, toCreateNode));
		});

		fmManager.save();
		fmManager.overwrite();
	}

	@Override
	public int hashCode() {
		return Objects.hash(toCreateNode);
	}

	@Override
	public boolean equals(Object obj) {
		System.out.println("A");
		if (this == obj) {
			return true;
		}
		System.out.println("A");
		if (obj == null) {
			return false;
		}
		System.out.println("A");
		if (getClass() != obj.getClass()) {
			return false;
		}
		System.out.println("A");
		final ResolutionCreateConstraint other = (ResolutionCreateConstraint) obj;
		return Objects.equals(toCreateNode, other.toCreateNode);
	}

	@Override
	public String toString() {
		return "ResolutionCreateConstraint [toCreateNode=" + toCreateNode + "]";
	}

}
