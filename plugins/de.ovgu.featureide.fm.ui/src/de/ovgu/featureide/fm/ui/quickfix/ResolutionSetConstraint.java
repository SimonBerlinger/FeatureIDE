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
import org.prop4j.Node;

import de.ovgu.featureide.fm.core.base.IConstraint;
import de.ovgu.featureide.fm.core.base.impl.Constraint;
import de.ovgu.featureide.fm.core.io.manager.FeatureModelManager;

/**
 * TODO description
 *
 * @author Simon Berlinger
 */
public class ResolutionSetConstraint extends QuickFixDefect implements IMarkerResolution {

	Node originalNode;
	Node newNode;

	public ResolutionSetConstraint(IMarker marker, FeatureModelManager manager, Node originalNode, Node newNode) {
		super(marker, manager);
		this.originalNode = originalNode;
		this.newNode = newNode;
	}

	public ResolutionSetConstraint(IMarker marker, FeatureModelManager manager, Node originalNode, Node newNode, String prefix, String postfix) {
		super(marker, manager);
		this.originalNode = originalNode;
		this.newNode = newNode;
		this.prefix = prefix;
		this.postfix = postfix;
	}

	@Override
	public String getLabel() {

		return prefix + " Change the constraint ''" + originalNode + "'' to ''" + newNode + "'' " + postfix;
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

}
