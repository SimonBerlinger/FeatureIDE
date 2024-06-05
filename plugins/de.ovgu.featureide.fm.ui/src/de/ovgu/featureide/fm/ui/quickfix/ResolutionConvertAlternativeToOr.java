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

import de.ovgu.featureide.fm.core.base.IFeature;
import de.ovgu.featureide.fm.core.io.manager.FeatureModelManager;

/**
 * A defect resolution, that converts an alternative-group to an or-group.
 *
 * @author Simon Berlinger
 */
public class ResolutionConvertAlternativeToOr extends AbstractResolution {

	/**
	 * The parent of the affected alternative-group
	 */
	private final IFeature alternativeParent;

	/**
	 *
	 * @param fmManager THe FeatureModelManager
	 * @param alternativeParent The parent of the alternative-group
	 * @param prefix The prefix for the label indicating the defect
	 */
	public ResolutionConvertAlternativeToOr(FeatureModelManager fmManager, IFeature alternativeParent, String prefix) {
		super(fmManager);
		this.alternativeParent = alternativeParent;
		this.prefix = prefix;
	}

	@Override
	public String getLabel() {

		return prefix + "Change the alternative-relation below ''" + alternativeParent + "'' to an or-relation";
	}

	@Override
	public void run(IMarker marker) {
		fmManager.overwrite();
		fmManager.editObject(featureModel -> {
			featureModel.getFeature(alternativeParent.getName()).getStructure().changeToOr();
			System.out.println(featureModel.getFeature(alternativeParent.getName()).getStructure().isOr() + " SUCCESS?");
		});

		fmManager.save();
		fmManager.overwrite();
	}

	@Override
	public int hashCode() {
		return Objects.hash(alternativeParent);
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
		final ResolutionConvertAlternativeToOr other = (ResolutionConvertAlternativeToOr) obj;
		return Objects.equals(alternativeParent, other.alternativeParent);
	}

	@Override
	public String toString() {
		return "ResolutionConvertAlternativeToOr [alternativeParent=" + alternativeParent + "]";
	}

}
