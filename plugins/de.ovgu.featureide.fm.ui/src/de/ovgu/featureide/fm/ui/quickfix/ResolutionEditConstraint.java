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

import org.eclipse.core.resources.IMarker;
import org.eclipse.ui.IMarkerResolution;

import de.ovgu.featureide.fm.core.base.IConstraint;
import de.ovgu.featureide.fm.core.io.manager.FeatureModelManager;
import de.ovgu.featureide.fm.ui.editors.featuremodel.actions.EditConstraintAction;

/**
 * TODO description
 *
 * @author Simon Berlinger
 */
public class ResolutionEditConstraint extends QuickFixDefect implements IMarkerResolution {

	IConstraint constraint;

	/**
	 * @param constr
	 */
	public ResolutionEditConstraint(IMarker marker, IConstraint constr, FeatureModelManager fmManager) {
		super(marker, fmManager);
		constraint = constr;
	}

	@Override
	public String getLabel() {
		return "Edit the constraint ''" + constraint + "''";
	}

	@Override
	public void run(IMarker marker) {

		fmManager.overwrite();
		fmManager.editObject(featureModel -> {
			final EditConstraintAction action = (EditConstraintAction) diagramEditor.getDiagramAction(EditConstraintAction.ID);
			action.setConstraint(constraint);
			action.run();
		});

		fmManager.save();
		fmManager.overwrite();
	}

}
