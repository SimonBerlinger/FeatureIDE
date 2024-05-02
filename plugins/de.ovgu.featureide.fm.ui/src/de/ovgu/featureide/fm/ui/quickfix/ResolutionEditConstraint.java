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

import java.util.List;
import java.util.Objects;

import org.eclipse.core.resources.IMarker;
import org.prop4j.Node;

import de.ovgu.featureide.fm.core.base.IConstraint;
import de.ovgu.featureide.fm.core.io.manager.FeatureModelManager;
import de.ovgu.featureide.fm.ui.editors.featuremodel.actions.EditConstraintAction;

/**
 * TODO description
 *
 * @author Simon Berlinger
 */
public class ResolutionEditConstraint extends AbstractResolution {

	private IConstraint constraint;

	public ResolutionEditConstraint(IConstraint constr, FeatureModelManager fmManager, String prefix) {
		super(fmManager);
		constraint = constr;
		this.prefix = prefix;
	}

	public ResolutionEditConstraint(Node constrNode, FeatureModelManager fmManager, String prefix) {
		super(fmManager);
		final List<IConstraint> constraints =
			fmManager.getPersistentFormula().getFeatureModel().getConstraints().stream().filter(x -> x.getNode().equals(constrNode)).toList();
		if (constraints.size() > 0) {
			constraint = constraints.get(0);
		}
		this.prefix = prefix;
	}

	@Override
	public String getLabel() {
		return prefix + "Edit the constraint ''" + constraint.getDisplayName() + "''";
	}

	@Override
	public void run(IMarker marker) {

		fmManager.overwrite();
		fmManager.editObject(featureModel -> {
			if (constraint != null) {
				final EditConstraintAction action = (EditConstraintAction) diagramEditor.getDiagramAction(EditConstraintAction.ID);
				action.setConstraint(constraint);
				action.run();
			}
		});

		fmManager.save();
		fmManager.overwrite();
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "ResolutionEditConstraint [constraint=" + constraint + "]";
	}

	@Override
	public int hashCode() {
		return Objects.hash(constraint);
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
		final ResolutionEditConstraint other = (ResolutionEditConstraint) obj;
		return Objects.equals(constraint.getNode(), other.constraint.getNode());
	}

}
