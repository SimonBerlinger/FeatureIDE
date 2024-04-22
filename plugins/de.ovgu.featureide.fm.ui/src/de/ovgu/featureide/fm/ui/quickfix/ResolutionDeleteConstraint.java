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
import org.eclipse.ui.IMarkerResolution;
import org.prop4j.Node;

import de.ovgu.featureide.fm.core.base.IConstraint;
import de.ovgu.featureide.fm.core.io.manager.FeatureModelManager;

/**
 * TODO description
 *
 * @author Simon Berlinger
 */
public class ResolutionDeleteConstraint extends QuickFixDefect implements IMarkerResolution {

	Node toDeleteNode;

	public ResolutionDeleteConstraint(IMarker marker, Node node, FeatureModelManager fmManager) {
		super(marker, fmManager);
		toDeleteNode = node;
	}

	/**
	 * @param node
	 * @param postfix
	 * @param prefix
	 */
	public ResolutionDeleteConstraint(IMarker marker, Node node, FeatureModelManager fmManager, String prefix, String postfix) {
		super(marker, fmManager);
		toDeleteNode = node;
		this.prefix = prefix;
		this.postfix = postfix;
	}

	public ResolutionDeleteConstraint(IMarker marker, Node node, FeatureModelManager fmManager, String prefix) {
		super(marker, fmManager);
		toDeleteNode = node;
		this.prefix = prefix;
	}

	@Override
	public String getLabel() {
		return prefix + "Delete the constraint ''" + toDeleteNode + "''" + postfix;
	}

	@Override
	public void run(IMarker marker) {

		fmManager.overwrite();
		fmManager.editObject(featureModel -> {

			IConstraint toDelete = null;
			for (final IConstraint c : featureModel.getConstraints()) {
				if (c.getNode().equals(toDeleteNode)) {
					toDelete = c;
					break;
				}
			}
			if (toDelete != null) {
				featureModel.removeConstraint(toDelete);
			}
		});

		fmManager.save();
		fmManager.overwrite();
	}

	@Override
	public int hashCode() {
		return Objects.hash(toDeleteNode);
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
		final ResolutionDeleteConstraint other = (ResolutionDeleteConstraint) obj;
		return Objects.equals(toDeleteNode, other.toDeleteNode);
	}

}
